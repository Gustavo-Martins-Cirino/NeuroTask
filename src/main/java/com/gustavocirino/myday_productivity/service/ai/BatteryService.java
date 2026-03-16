package com.gustavocirino.myday_productivity.service.ai;

import com.gustavocirino.myday_productivity.model.User;
import com.gustavocirino.myday_productivity.model.UserBattery;
import com.gustavocirino.myday_productivity.model.Task;
import com.gustavocirino.myday_productivity.model.enums.BurnoutRiskLevel;
import com.gustavocirino.myday_productivity.model.enums.ContextType;
import com.gustavocirino.myday_productivity.model.enums.TaskStatus;
import com.gustavocirino.myday_productivity.repository.TaskRepository;
import com.gustavocirino.myday_productivity.repository.UserBatteryRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bateria Social/Foco — módulo inspirado no monitoramento de Saúde do Equipamento do SpinOps.
 *
 * <p>O método principal {@link #calculateAndSaveBatterySnapshot(User)} calcula o estado
 * cognitivo atual do usuário com base nas tarefas agendadas para hoje, compara com o
 * histórico recente e persiste um snapshot em {@code tb_user_battery}. Quando o risco
 * de burnout é alto, a IA gera um alerta empático e sugere um tempo de pausa.</p>
 *
 * <p>Deve ser invocado: (1) via scheduler periódico ao longo do dia; (2) após qualquer
 * criação/agendamento de tarefa de peso cognitivo elevado.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatteryService {

    // ─── Multiplicadores de peso por tipo de contexto ─────────────────────────
    private static final double HEAVY_CONTEXT_MULTIPLIER  = 1.5;
    private static final double MEDIUM_CONTEXT_MULTIPLIER = 1.2;
    private static final double LIGHT_CONTEXT_MULTIPLIER  = 1.0;

    private final TaskRepository taskRepository;
    private final UserBatteryRepository userBatteryRepository;
    private final ChatLanguageModel chatLanguageModel;

    /** Resultado da avaliação de risco feita pela IA. */
    private record AiRiskResult(BurnoutRiskLevel riskLevel, double riskScore,
                                 String message, int breakMinutes) {}

    // ─── Método principal ─────────────────────────────────────────────────────

    @Transactional
    public UserBattery calculateAndSaveBatterySnapshot(User user) {
        log.debug("Calculando snapshot de bateria para usuário {}", user.getId());

        List<Task> todayTasks = fetchTodayActiveTasks(user);
        int totalTaskWeight   = todayTasks.stream().mapToInt(Task::getWeight).sum();
        int cognitiveLoad     = calculateCognitiveLoad(todayTasks);

        Double historicalAvg = userBatteryRepository.findAverageCognitiveLoadByUser(user);
        double baseline = (historicalAvg != null && historicalAvg > 0) ? historicalAvg : 50.0;

        int batteryLevel = Math.max(0, Math.min(100, 100 - cognitiveLoad));

        // Delega toda a interpretação de risco à IA e aplica coerência hardcoded.
        AiRiskResult risk = assessRiskWithAI(batteryLevel, cognitiveLoad, totalTaskWeight, todayTasks, baseline);
        risk = enforceBatteryCoherence(risk, batteryLevel, cognitiveLoad);

        UserBattery snapshot = new UserBattery();
        snapshot.setUser(user);
        snapshot.setBatteryLevel(batteryLevel);
        snapshot.setCognitiveLoad(cognitiveLoad);
        snapshot.setTotalTaskWeight(totalTaskWeight);
        snapshot.setBurnoutRiskScore(risk.riskScore());
        snapshot.setRiskLevel(risk.riskLevel());
        if (risk.breakMinutes() > 0) snapshot.setSuggestedBreakMinutes(risk.breakMinutes());
        if (risk.message() != null && !risk.message().isBlank()) snapshot.setAiNotes(risk.message());

        UserBattery saved = userBatteryRepository.save(snapshot);
        log.info("Snapshot salvo: userId={}, bateria={}%, carga={}%, risco={}",
                user.getId(), batteryLevel, cognitiveLoad, risk.riskLevel());
        return saved;
    }

    // ─── Avaliação de risco via IA ────────────────────────────────────────────

    private AiRiskResult assessRiskWithAI(int batteryLevel, int cognitiveLoad,
                                           int totalTaskWeight, List<Task> todayTasks,
                                           double baseline) {
        try {
            String prompt = buildBatteryPrompt(batteryLevel, cognitiveLoad, totalTaskWeight, todayTasks, baseline);
            String response = chatLanguageModel.generate(prompt);
            log.debug("Resposta IA bateria: {}", response);
            return parseAiRiskResponse(response, batteryLevel, cognitiveLoad, baseline);
        } catch (Exception e) {
            log.warn("IA indisponível para avaliação de bateria, usando fallback: {}", e.getMessage());
            return formulaFallback(batteryLevel, cognitiveLoad, baseline);
        }
    }

    private String buildBatteryPrompt(int batteryLevel, int cognitiveLoad,
                                       int totalTaskWeight, List<Task> todayTasks, double baseline) {
        long done = todayTasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
        long active = todayTasks.stream().filter(t -> t.getStatus() != TaskStatus.DONE).count();
        long highPriority = todayTasks.stream()
            .filter(t -> t.getPriority() != null && "HIGH".equalsIgnoreCase(t.getPriority().name()))
            .count();

        return """
            Você é uma IA analista do NeuroTask. Analise os dados reais e responda SOMENTE com JSON válido.
            Proibido usar texto genérico, frases prontas, markdown ou qualquer conteúdo fora do JSON.

            Estado atual:
            - Bateria cognitiva: %d%% (0=esgotado, 100=totalmente descansado)
            - Carga cognitiva: %d%%
            - Peso total de tarefas hoje: %d
            - Carga histórica média: %.0f%%
            - Tarefas concluídas hoje: %d | Tarefas ativas restantes: %d
            - Tarefas de alta prioridade hoje: %d

            Regras OBRIGATÓRIAS de coerência (HARD RULES):
            1. SE BATERIA >= 70%%:
               - O risco DEVE ser 'BAIXO' (ou no máximo 'MODERADO' se carga > 80%%).
               - O risco NUNCA pode ser 'ALTO' ou 'CRÍTICO' ou 'CRITICO'.
               - A 'sugestaoPausa' DEVE ser 0.
               - A mensagem deve PARABENIZAR o usuário e incentivar o fluxo (ex: "Bateria excelente! Continue focado.").
            2. SE BATERIA < 70%% e >= 50%%:
               - Risco 'MODERADO' (ou 'ALTO' se muitas tarefas críticas).
               - Pausa 0 a 10 min.
            3. SE BATERIA < 30%%:
               - Risco 'ALTO' ou 'CRÍTICO'.
               - Pausa obrigatória 15-30 min.

            Responda SOMENTE com este JSON exato:
            {"risco":"BAIXO","mensagem":"sua mensagem curta em pt-BR","sugestaoPausa":0}

            Valores válidos para risco: BAIXO, MODERADO, ALTO, CRÍTICO
            sugestaoPausa: 0 a 60 minutos
            """.formatted(batteryLevel, cognitiveLoad, totalTaskWeight, baseline, done, active, highPriority);
    }

    private AiRiskResult parseAiRiskResponse(String response, int batteryLevel,
                                              int cognitiveLoad, double baseline) {
        try {
            Pattern jsonPat = Pattern.compile("\\{[^{}]*\\}");
            Matcher m = jsonPat.matcher(response);
            if (!m.find()) return formulaFallback(batteryLevel, cognitiveLoad, baseline);

            String json = m.group();
            String risco    = extractJsonString(json, "risco");
            String mensagem = extractJsonString(json, "mensagem");
            int    pausa    = extractJsonInt(json, "sugestaoPausa");

            BurnoutRiskLevel level = switch (risco.toUpperCase().trim()) {
                case "ALTO"              -> BurnoutRiskLevel.HIGH;
                case "CRÍTICO", "CRITICO" -> BurnoutRiskLevel.CRITICAL;
                case "MODERADO"          -> BurnoutRiskLevel.MODERATE;
                default                  -> BurnoutRiskLevel.LOW;
            };

            // Garantia de coerência: bateria alta não pode ter risco crítico/alto
            if (batteryLevel >= 70 && (level == BurnoutRiskLevel.CRITICAL || level == BurnoutRiskLevel.HIGH)) {
                level = BurnoutRiskLevel.MODERATE;
                pausa = 0;
            }

            double score = switch (level) {
                case CRITICAL -> 0.85; case HIGH -> 0.65;
                case MODERATE -> 0.40; case LOW  -> 0.15;
            };

            return new AiRiskResult(level, score, mensagem.isBlank() ? null : mensagem, pausa);
        } catch (Exception e) {
            log.warn("Falha ao parsear JSON de risco da IA: {}", e.getMessage());
            return formulaFallback(batteryLevel, cognitiveLoad, baseline);
        }
    }

    private AiRiskResult enforceBatteryCoherence(AiRiskResult raw, int batteryLevel, int cognitiveLoad) {
        // Fallback robusto se resultado nulo
        if (raw == null) {
            return formulaFallback(batteryLevel, cognitiveLoad, 50.0);
        }

        BurnoutRiskLevel level = raw.riskLevel();
        double score = raw.riskScore();
        int pause = raw.breakMinutes(); // pode vir negativo do JSON se IA alucinar, tratar abaixo
        String message = raw.message();

        // 1. REGRA SUPREMA: Bateria Alta (>= 70%) -> NUNCA ALTO/CRÍTICO
        if (batteryLevel >= 70) {
            // Força rebaixamento de nível
            if (level == BurnoutRiskLevel.CRITICAL || level == BurnoutRiskLevel.HIGH) {
                level = BurnoutRiskLevel.LOW; // Default para LOW
                // Exception: se a carga cognitiva medida for realmente alta (>80%), aceitamos MODERADO
                if (cognitiveLoad > 80) {
                    level = BurnoutRiskLevel.MODERATE;
                }
            }
            // Força pausa zero
            pause = 0;
            // Ajusta score para refletir o nível rebaixado
            score = (level == BurnoutRiskLevel.MODERATE) ? 0.35 : 0.10;
            
            // Se a mensagem original parecia alarmista, substitui
            boolean msgAlarmista = message != null && (message.toLowerCase().contains("crítico") || message.toLowerCase().contains("pausa") || message.toLowerCase().contains("esgotamento"));
            if (message == null || message.isBlank() || msgAlarmista) {
                message = "Sua bateria está ótima (" + batteryLevel + "%). Aproveite essa energia para avançar nas tarefas principais.";
            }
        }

        // 2. REGRA: Bateria Crítica (< 30%) -> Força alerta se IA vier com "BAIXO"
        if (batteryLevel < 30 ) {
             if (level == BurnoutRiskLevel.LOW || level == BurnoutRiskLevel.MODERATE) {
                 level = BurnoutRiskLevel.HIGH;
                 score = 0.75;
                 pause = Math.max(pause, 15);
                 if (message == null || !message.contains("pausa")) {
                     message = "Bateria baixa (" + batteryLevel + "%). Recomendamos uma pausa de " + pause + " min para recarregar.";
                 }
             }
        }
        
        // Sanitização final de pausa
        pause = Math.max(0, pause);

        return new AiRiskResult(level, score, message, pause);
    }

    private String extractJsonString(String json, String field) {
        Matcher m = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1).trim() : "";
    }

    private int extractJsonInt(String json, String field) {
        Matcher m = Pattern.compile("\"" + field + "\"\\s*:\\s*(\\d+)").matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    /** Cálculo local usado como fallback quando a IA está indisponível. */
    private AiRiskResult formulaFallback(int batteryLevel, int cognitiveLoad, double baseline) {
        double raw   = baseline > 0 ? cognitiveLoad / (baseline * 1.5) : 0.0;
        double score = Math.min(1.0, Math.max(0.0, raw));
        BurnoutRiskLevel level;
        if      (score >= 0.80) level = BurnoutRiskLevel.CRITICAL;
        else if (score >= 0.60) level = BurnoutRiskLevel.HIGH;
        else if (score >= 0.35) level = BurnoutRiskLevel.MODERATE;
        else                    level = BurnoutRiskLevel.LOW;

        if (batteryLevel >= 70 && (level == BurnoutRiskLevel.CRITICAL || level == BurnoutRiskLevel.HIGH)) {
            level = BurnoutRiskLevel.MODERATE;
        }

        int breakMin = level == BurnoutRiskLevel.CRITICAL ? 30
                     : level == BurnoutRiskLevel.HIGH     ? 15 : 0;

        String msg = level == BurnoutRiskLevel.CRITICAL
                ? "Carga cognitiva muito elevada. " + breakMin + " minutos de pausa vão ajudar a recuperar o foco."
                : level == BurnoutRiskLevel.HIGH
                ? "Sinais de sobrecarga. Uma pausa curta de " + breakMin + " minutos aumentará sua produtividade."
                : null;

        return new AiRiskResult(level, score, msg, breakMin);
    }

    // ─── Cálculos de carga cognitiva ──────────────────────────────────────────

    /**
     * Retorna tarefas SCHEDULED do dia atual para o usuário.
     * Janela: 00:00:00 até 23:59:59 do dia corrente.
     */
    private List<Task> fetchTodayActiveTasks(User user) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay   = LocalDate.now().atTime(LocalTime.MAX);

        // Tarefas agendadas com horário dentro de hoje
        List<Task> scheduled = taskRepository
                .findByStatusAndStartTimeBetween(TaskStatus.SCHEDULED, startOfDay, endOfDay)
                .stream()
                .filter(t -> user.getId().equals(t.getUser() != null ? t.getUser().getId() : null))
                .toList();

        // Tarefas PENDING sem horário também contribuem para a carga do dia
        List<Task> pending = taskRepository.findByUserId(user.getId())
                .stream()
                .filter(t -> t.getStatus() == TaskStatus.PENDING)
                .toList();

        return java.util.stream.Stream.concat(scheduled.stream(), pending.stream()).toList();
    }

    /**
     * Carga cognitiva ponderada (0–100).
     *
     * <p>Cada tarefa contribui com: {@code weight × contextMultiplier × 4}.
     * O fator 4 normaliza o score: 5 tarefas de peso máximo (5) com multiplicador 1.0
     * resultam em 100 pontos. Contextos CREATIVE/LEARNING amplificam esse valor.</p>
     *
     * <p>O resultado é limitado a 100 para que o campo {@code batteryLevel} não
     * assuma valores negativos.</p>
     */
    private int calculateCognitiveLoad(List<Task> tasks) {
        double raw = tasks.stream()
                .mapToDouble(task -> task.getWeight() * contextMultiplier(task.getContextType()) * 4.0)
                .sum();
        return (int) Math.min(100, Math.round(raw));
    }

    /**
     * Retorna o multiplicador cognitivo de acordo com o tipo de contexto da tarefa.
     * Contextos de foco profundo valem 1.5×; comunicação vale 1.2×; demais valem 1.0×.
     */
    private double contextMultiplier(ContextType contextType) {
        if (contextType == null) {
            return LIGHT_CONTEXT_MULTIPLIER;
        }
        return switch (contextType) {
            case CREATIVE, LEARNING  -> HEAVY_CONTEXT_MULTIPLIER;
            case MEETINGS, WRITING   -> MEDIUM_CONTEXT_MULTIPLIER;
            default                  -> LIGHT_CONTEXT_MULTIPLIER;
        };
    }

}
