package com.gustavocirino.myday_productivity.service.ai;

import com.gustavocirino.myday_productivity.model.ProcrastinationRecord;
import com.gustavocirino.myday_productivity.model.Task;
import com.gustavocirino.myday_productivity.model.User;
import com.gustavocirino.myday_productivity.repository.ProcrastinationRecordRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Rastreador de Procrastinação — módulo inspirado na Detecção de Anomalias do SpinOps.
 *
 * <p>Cada vez que o usuário adia uma tarefa (move de volta ao backlog ou reagenda),
 * {@link #handleTaskPostponement(Task)} deve ser chamado pelo {@code TaskService}.
 * O método mantém um {@link ProcrastinationRecord} por tarefa, incrementa o contador
 * de adiamentos e calcula o Z-score comparando o padrão atual com o histórico do
 * usuário. Quando |Z| > 2.0 (anomalia estatística), a IA gera uma sugestão de
 * decomposição em micro-passos Pomodoro para reduzir a resistência de execução.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProcrastinationService {

    /** Limiar de anomalia: adiamentos que ficam além de 2 desvios padrão da média. */
    private static final double ANOMALY_THRESHOLD = 2.0;

    /**
     * Amostra mínima de registros para que o Z-score seja estatisticamente significativo.
     * Com menos registros, o desvio padrão é instável e qualquer valor pareceria anomalia.
     */
    private static final int MIN_SAMPLE_SIZE = 3;

    private final ProcrastinationRecordRepository procrastinationRecordRepository;
    private final ChatLanguageModel chatLanguageModel;

    // ─── Método principal ─────────────────────────────────────────────────────

    /**
     * Registra um evento de adiamento para a tarefa fornecida e, se necessário,
     * gera uma sugestão de decomposição via IA.
     *
     * <p>Deve ser invocado pelo {@code TaskService} sempre que uma tarefa SCHEDULED
     * for movida de volta ao backlog ({@code moveBackToBacklog}) ou reagendada para
     * uma data futura diferente da original.</p>
     *
     * @param task Tarefa que foi adiada; {@code task.getUser()} pode ser nulo
     *             (tarefas órfãs), caso em que o Z-score é ignorado.
     * @return O {@link ProcrastinationRecord} salvo com contadores atualizados.
     */
    @Transactional
    public ProcrastinationRecord handleTaskPostponement(Task task) {
        User user = task.getUser();

        // 1. Busca o registro existente ou cria um novo para esta tarefa
        ProcrastinationRecord record = procrastinationRecordRepository
                .findByTaskId(task.getId())
                .orElseGet(() -> buildNewRecord(task, user));

        // 2. Incrementa o contador de adiamentos
        record.setPostponeCount(record.getPostponeCount() + 1);
        // A nova data será definida fora deste service quando o usuário reagendar
        record.setRescheduledAt(null);

        // 3. Calcula Z-score (requer usuário e amostra mínima)
        if (user != null) {
            double zScore = calculateZScore(record, user);
            record.setZScore(zScore);

            // 4. Anomalia detectada quando |Z| ultrapassa o limiar estatístico
            boolean isAnomaly = Math.abs(zScore) > ANOMALY_THRESHOLD;
            record.setIsAnomaly(isAnomaly);

            // 5. Gera sugestão de decomposição via IA (apenas uma vez por anomalia)
            if (isAnomaly && !record.getSuggestionGenerated()) {
                log.info("Anomalia de procrastinação detectada: tarefa='{}', Z={}, adiamentos={}",
                        task.getTitle(), String.format("%.2f", zScore), record.getPostponeCount());

                String suggestion = generateBreakdownSuggestion(task, record.getPostponeCount());
                record.setAiSuggestion(suggestion);
                record.setSuggestionGenerated(true);
            }
        }

        return procrastinationRecordRepository.save(record);
    }

    // ─── Z-score ──────────────────────────────────────────────────────────────

    /**
     * Calcula Z = (x − μ) / σ onde:
     * <ul>
     *   <li>x = {@code current.postponeCount} (já incrementado, ainda não salvo)</li>
     *   <li>μ = média histórica dos postponeCount de TODOS os registros do usuário</li>
     *   <li>σ = desvio padrão populacional dos mesmos registros</li>
     * </ul>
     *
     * <p>Os registros no DB contêm o valor ANTERIOR ao incremento atual, o que é
     * intencional: queremos saber se o novo valor foge do padrão estabelecido.</p>
     *
     * @return Z-score calculado, ou 0.0 se a amostra for insuficiente ou σ = 0.
     */
    private double calculateZScore(ProcrastinationRecord current, User user) {
        List<Double> allValues = procrastinationRecordRepository
                .findAllPostponeCountValuesByUser(user);

        if (allValues.size() < MIN_SAMPLE_SIZE) {
            log.debug("Amostra insuficiente ({} registros) para calcular Z-score do usuário {}",
                    allValues.size(), user.getId());
            return 0.0;
        }

        double[] values = allValues.stream().mapToDouble(Double::doubleValue).toArray();
        double mean = calculateMean(values);
        double stdDev = calculateStdDev(values, mean);

        if (stdDev == 0.0) {
            // Todos os registros têm o mesmo valor: sem variação histórica detectável
            return 0.0;
        }

        return (current.getPostponeCount() - mean) / stdDev;
    }

    private double calculateMean(double[] values) {
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    /** Desvio padrão populacional (divide por N, não por N-1). */
    private double calculateStdDev(double[] values, double mean) {
        double sumSquaredDiffs = 0.0;
        for (double v : values) {
            double diff = v - mean;
            sumSquaredDiffs += diff * diff;
        }
        return Math.sqrt(sumSquaredDiffs / values.length);
    }

    // ─── Geração de sugestão via IA ───────────────────────────────────────────

    /**
     * Chama o LLM (Groq / Llama) para gerar uma lista de micro-passos concretos
     * que reduzam a resistência de execução da tarefa. Em caso de falha (sem
     * internet, cota esgotada, timeout), retorna um fallback Pomodoro local.
     *
     * @param task          Tarefa a ser decomposta.
     * @param postponeCount Número atual de adiamentos — contextualiza o prompt.
     * @return Sugestão em linguagem natural (gerada pela IA ou pelo fallback local).
     */
    private String generateBreakdownSuggestion(Task task, int postponeCount) {
        try {
            String prompt = buildBreakdownPrompt(task, postponeCount);
            String response = chatLanguageModel.generate(prompt);
            log.debug("Sugestão de decomposição gerada pela IA para tarefa '{}'", task.getTitle());
            return response;
        } catch (Exception ex) {
            log.warn("Falha ao chamar IA para decomposição da tarefa '{}': {}. Aplicando fallback Pomodoro.",
                    task.getTitle(), ex.getMessage());
            return buildPomodoroFallback(task);
        }
    }

    /**
     * Prompt estruturado em português brasileiro para o LLM.
     * Solicita exatamente 3–5 micro-passos com verbo de ação e duração ≤ 25 min.
     */
    private String buildBreakdownPrompt(Task task, int postponeCount) {
        String estimatedInfo = task.getEstimatedDurationMinutes() != null
                ? "Duração estimada original: " + task.getEstimatedDurationMinutes() + " minutos."
                : "Duração estimada: não informada pelo usuário.";

        String description = task.getDescription() != null && !task.getDescription().isBlank()
                ? task.getDescription()
                : "Nenhuma descrição fornecida.";

        return """
                Você é um coach de produtividade especializado em superar procrastinação crônica.

                O usuário adiou a tarefa a seguir %d vezes — um padrão estatisticamente anômalo \
                para o histórico dele, o que indica resistência real de execução.

                TAREFA:
                  Título: %s
                  Descrição: %s
                  %s

                SUA MISSÃO:
                Quebre esta tarefa em exatamente 3 a 5 micro-passos concretos e objetivos que \
                eliminam a ambiguidade que causa procrastinação.

                REGRAS OBRIGATÓRIAS para cada micro-passo:
                  1. Máximo de 25 minutos de duração (compatível com a Técnica Pomodoro).
                  2. Deve começar com um verbo de ação no infinitivo \
                (ex.: Abrir, Escrever, Pesquisar, Revisar, Listar, Definir, Enviar).
                  3. Deve ser específico o suficiente para que não haja dúvida sobre o que fazer.
                  4. Não deve depender de um recurso externo que o usuário não controla.

                Responda APENAS com a lista numerada dos micro-passos. \
                Sem introdução, sem conclusão, sem explicações extras.
                """.formatted(postponeCount, task.getTitle(), description, estimatedInfo);
    }

    /**
     * Sugestão genérica gerada localmente quando o serviço de IA está indisponível.
     * Baseada na Técnica Pomodoro para garantir sempre um plano acionável ao usuário.
     */
    private String buildPomodoroFallback(Task task) {
        return """
                (Sugestão gerada localmente — serviço de IA temporariamente indisponível)

                Para vencer a resistência em "%s", experimente estes passos Pomodoro:

                1. Definir em 1 frase qual é o ÚNICO resultado concreto esperado ao final desta sessão.
                2. Trabalhar por 25 minutos em modo foco total (notificações desligadas, tela cheia).
                3. Fazer uma pausa obrigatória de 5 minutos — levante-se e mova-se.
                4. Revisar o que foi feito e decidir se mais um bloco de 25 min é necessário.
                5. Repetir até concluir, sem buscar perfeição no primeiro ciclo.
                """.formatted(task.getTitle());
    }

    // ─── Helper de criação ────────────────────────────────────────────────────

    private ProcrastinationRecord buildNewRecord(Task task, User user) {
        ProcrastinationRecord newRecord = new ProcrastinationRecord();
        newRecord.setTask(task);
        newRecord.setUser(user);
        newRecord.setOriginalScheduledAt(task.getStartTime());
        return newRecord;
    }
}
