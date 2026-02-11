package com.gustavocirino.myday_productivity.service.ai;

import com.gustavocirino.myday_productivity.dto.*;
import com.gustavocirino.myday_productivity.model.Task;
import com.gustavocirino.myday_productivity.model.enums.TaskPriority;
import com.gustavocirino.myday_productivity.model.enums.TaskStatus;
import com.gustavocirino.myday_productivity.repository.TaskRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serviço de análise de produtividade com IA usando Google Gemini.
 * 
 * Este serviço implementa o "coach cognitivo" do NeuroTask, analisando padrões
 * de trabalho, identificando gargalos e fornecendo insights personalizados.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductivityAnalysisService {

    private static final ZoneId APP_ZONE = ZoneId.of("America/Sao_Paulo");

    private final ChatLanguageModel chatModel;
    private final TaskRepository taskRepository;

    @Value("${openai.model.name}")
    private String modelName;

    /**
     * Analisa a produtividade do usuário usando IA.
     */
    public AIAnalysisResponseDTO analyzeProductivity(AIAnalysisRequestDTO request) {
        log.info("🤖 Iniciando análise de produtividade: {}", request.analysisType());

        // Busca todas as tarefas para análise
        List<Task> allTasks = taskRepository.findAll();

        // Calcula métricas base
        long totalTasks = allTasks.size();
        long completedTasks = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
        long lateTasks = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.LATE).count();
        long scheduledTasks = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.SCHEDULED).count();

        double completionRate = totalTasks > 0 ? (completedTasks * 100.0 / totalTasks) : 0.0;

        // Monta o contexto para a IA
        String context = buildAnalysisContext(allTasks, totalTasks, completedTasks, lateTasks, scheduledTasks,
                completionRate);

        // Gera o prompt baseado no tipo de análise
        String prompt = switch (request.analysisType()) {
            case "productivity" -> buildProductivityPrompt(context);
            case "patterns" -> buildPatternsPrompt(context);
            case "recommendations" -> buildRecommendationsPrompt(context);
            default -> buildGeneralPrompt(context);
        };

        try {
            // Consulta a IA
            String aiResponse = chatModel.generate(prompt);

            log.info("✅ Análise concluída. Score: {}", (int) completionRate);

            // Parse da resposta (formato esperado: Summary|||Insight1;Insight2|||Rec1;Rec2)
            return parseAIResponse(aiResponse, (int) completionRate);
        } catch (Exception e) {
            String rawMessage = e.getMessage() != null ? e.getMessage() : "Erro desconhecido na chamada da IA.";
            String simpleName = e.getClass().getSimpleName();

            boolean isOpenAiHttp = "OpenAiHttpException".equals(simpleName)
                    || simpleName.contains("OpenAiHttp");
            boolean isBalanceIssue = rawMessage.toLowerCase().contains("insufficient balance")
                    || rawMessage.contains("402");

            if (isOpenAiHttp && isBalanceIssue) {
                log.warn("⚠️ IA de análise indisponível por saldo insuficiente: {} - {}", simpleName, rawMessage);

                String fallbackSummary = "No momento, meu módulo de análise avançada está descansando. " +
                        "Mas você ainda pode agendar manualmente e acompanhar suas tarefas na linha do tempo.";

                return new AIAnalysisResponseDTO(
                        fallbackSummary,
                        List.of(),
                        List.of(),
                        (int) completionRate,
                        LocalDateTime.now());
            }

            log.error("❌ Erro ao gerar análise de produtividade: tipo={}, mensagem={}",
                    e.getClass().getSimpleName(), rawMessage, e);
            throw new RuntimeException("Erro ao gerar análise de produtividade: " + rawMessage, e);
        }
    }

    /**
     * Chat interativo com a IA sobre produtividade.
     */
    public AIChatResponseDTO chat(AIChatRequestDTO request) {
        log.info("💬 Chat com IA: {}", request.message());

        try {
            // Contexto geral das tarefas (todas as tarefas do sistema)
            List<Task> allTasks = taskRepository.findAll();
            String taskContext = sanitizeForPrompt(buildTaskSummary(allTasks));

            // Contexto de tarefas de HOJE e AMANHÃ, alinhado com o fuso configurado
            // (America/Sao_Paulo)
            LocalDate today = LocalDate.now(APP_ZONE);
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfTomorrow = today.plusDays(1).atTime(LocalTime.MAX);
            List<Task> contextTasks = taskRepository.findAllByStartTimeBetween(startOfDay, endOfTomorrow);
            String todayContext = sanitizeForPrompt(buildContextSummary(contextTasks, today));

            // Carimbo temporal explícito para o modelo não confundir "hoje" / "amanhã" /
            // "agora"
            LocalDateTime now = LocalDateTime.now(APP_ZONE);
            String dayOfWeek = today.getDayOfWeek().toString();

            String prompt = """
                    DATA_E_HORA_ATUAL: %s
                    DIA_DA_SEMANA: %s

                    DEFINIÇÃO DE INTENÇÕES (INTENTS):
                    - AÇÃO: quando o usuário quer CRIAR, MUDAR ou DELETAR algo na agenda (por exemplo: "agende", "remarque", "mude para", "apague"). Nesses casos, você deve responder usando JSON_ACTION.
                    - CONSULTA: quando o usuário apenas pergunta o que tem na agenda ou faz perguntas gerais (por exemplo: "o que tenho hoje?", "quais são minhas tarefas?", "como está minha semana?"). Nesses casos, você deve responder APENAS em texto natural, sem JSON_ACTION.

                    Regra geral:
                    - Se for uma CONSULTA, responda normalmente em texto, explicando a agenda ou dando orientações, SEM gerar JSON_ACTION.
                    - Se for uma AÇÃO de agendamento/alteração, siga o protocolo de JSON_ACTION abaixo.
                    - SE A MENSAGEM DO USUÁRIO NÃO CONTIVER NENHUM VERBO DE AÇÃO (como "agendar", "agende", "criar", "crie", "marcar", "remarcar", "mudar", "alterar", "adicionar", "apagar", "deletar"), VOCÊ ESTÁ TERMINANTEMENTE PROIBIDO DE GERAR JSON_ACTION. Nesses casos, trate como CONSULTA e responda APENAS EM TEXTO NATURAL.

                    Exemplo de CONSULTA (sem JSON_ACTION):
                    - Usuário: "O que tenho para amanhã?"
                    - Resposta correta: "Você tem X tarefas amanhã." (APENAS TEXTO, SEM JSON_ACTION)

                    DATA ATUAL DO SISTEMA:
                    - DATA_E_HORA_ATUAL representa exatamente o momento atual do sistema.
                    - "Hoje" significa sempre a data de DATA_E_HORA_ATUAL.
                    - "Amanhã" significa SEMPRE o dia imediatamente seguinte à data de DATA_E_HORA_ATUAL (por exemplo, se DATA_E_HORA_ATUAL for 15/01, então "amanhã" deve ser 16/01).
                    - Se o usuário mencionar explicitamente "amanhã" em um pedido de agendamento (INTENT = AÇÃO), o campo "start" do JSON_ACTION DEVE obrigatoriamente cair nesse dia seguinte calculado.
                    - Quando o usuário disser "agora", interprete como a HORA exata atual do sistema (DATA_E_HORA_ATUAL), nunca um horário aleatório na madrugada.
                    - A menos que o usuário peça explicitamente um horário entre 00:00 e 06:00, você está proibido de agendar tarefas nesse intervalo. Se o usuário disser "agora" e for 14:00, o agendamento deve ser 14:00 do dia atual.

                    CONTEXTO DO SISTEMA (NÃO MOSTRAR PARA O USUÁRIO):
                    - Resumo geral de tarefas (todas): %s
                    - Tarefas de hoje e amanhã (foco em priorização e conflitos): %s

                    Você é o estrategista da NeuroTask, um coach de produtividade cognitiva especializado em time-blocking e gestão de energia mental.
                    Seja amigável, empático e direto. Responda em português brasileiro.

                    IMPORTANTE: Se o usuário pedir mais de uma tarefa na mesma mensagem, você é OBRIGADO a focar em agendar apenas a PRIMEIRA via JSON_ACTION e, no texto, dizer explicitamente ao final: "Agendei o [Tarefa 1]. Quando quiser, podemos marcar o [Tarefa 2]?". Por enquanto, gere apenas UM JSON_ACTION (sem múltiplos JSONs na mesma resposta).
                    Você é um assistente atencioso. Se houver algo que você não pôde processar via JSON agora (como uma segunda tarefa, uma condição especial ou um detalhe pendente), você deve obrigatoriamente mencionar isso de forma amigável no texto da sua resposta. Nunca encerre a conversa sem dar algum feedback sobre todos os pontos pedidos pelo usuário.

                    VOCÊ DEVE ATUAR COMO UM EDITOR MINIMALISTA para os títulos das tarefas.
                    - PROIBIÇÃO DE ARTIGOS: Você deve obrigatoriamente remover artigos indefinidos ("um", "uma") e definidos ("o", "a") do início dos títulos.
                    - Exemplos: "Um café" vira "Café"; "Uma reunião com Diretor" vira "Diretor: Reunião"; "Agende um café" deve resultar em título apenas "Café".
                    - Remova também palavras genéricas como "reunião", "agendar", "marcar" quando não forem essenciais para o sentido.
                    - Se o usuário disser, por exemplo, "Reunião crítica com diretor", o título DEVE ser apenas "Diretor: Crítico".
                    - FORMATO: O título da tarefa deve SEMPRE começar com letra maiúscula (por exemplo: "Dentista", "Academia").
                    - Nunca utilize mais do que 3 ou 4 palavras no título final.
                    - ERRO CRÍTICO: Nunca use a frase completa do usuário como título. Extraia apenas o núcleo sem ruído (por exemplo, de "café de 15 min" use apenas "Café").

                    Se o usuário pedir para agendar algo (por exemplo: "me lembre de...", "agende...", "quarta às 15h quero..."),
                    responda mentalmente confirmando os detalhes entendidos (data, horário, título), mas NÃO escreva essa confirmação no texto.

                    SEMPRE que identificar um desejo claro de agendamento (INTENT = AÇÃO), você deve gerar APENAS um bloco JSON em uma única linha,
                    no seguinte formato geral (sem comentários, sem texto adicional):
                    JSON_ACTION: {"type":"CREATE_TASK","title":"Título da tarefa","start":"YYYY-MM-DDTHH:mm:ss","priority":2}

                    Protocolo de Verificação de Conflito (INFLEXÍVEL):
                    - Antes de gerar um CREATE_TASK, você deve verificar o campo "Tarefas de hoje e amanhã".
                    - Se o horário solicitado (start) estiver ocupado por qualquer tarefa existente hoje ou amanhã, é PROIBIDO usar "type":"CREATE_TASK".
                    - Se o horário X solicitado colidir com qualquer intervalo presente na lista Y de "Tarefas de hoje e amanhã", então retornar CREATE_TASK é um ERRO FATAL DE LÓGICA. Nessa situação, você deve OBRIGATORIAMENTE gerar um JSON_ACTION com "type":"CONFLICT_DETECTED".
                    - Se na descrição da tarefa de contexto houver o marcador "(OCUPADO)" exatamente no mesmo horário que o usuário está pedindo, você está TERMINANTEMENTE PROIBIDO de usar "type":"CREATE_TASK" para esse horário. Use sempre "type":"CONFLICT_DETECTED".
                    - Ao detectar um conflito, verifique o horário de término da tarefa conflitante. Exemplo: se o compromisso "Dentista" vai até 15:30, sugira o início da nova tarefa para 15:30 ou, se o contexto indicar, para 16:00. NUNCA sugira um horário que ainda esteja dentro do intervalo de uma tarefa marcada como (OCUPADO).
                    - Ao usar CONFLICT_DETECTED, você deve sugerir o próximo slot vago de 1 (uma) hora.

                    IDENTIFICAÇÃO PRECISA: Ao detectar um conflito, você deve citar o nome exato da tarefa que já está no horário (ex: "Você tem um Café agendado"). Não chame tudo de "reunião" ou "compromisso".

                    Regras importantes para o JSON_ACTION:
                    - Se o horário solicitado estiver LIVRE, use "type":"CREATE_TASK".
                    - Se o horário solicitado estiver OCUPADO por alguma tarefa do contexto de hoje, use "type":"CONFLICT_DETECTED" e preencha o campo "start" com o PRÓXIMO horário vago sugerido (no mesmo dia).
                    - "title" deve ser uma frase curta, profissional, sem artigos iniciais e com no máximo 3 ou 4 palavras.
                    - "start" deve ser um horário completo no formato ISO local: YYYY-MM-DDTHH:mm:ss (sem fuso, sem milissegundos).
                    - "priority" deve ser um número inteiro 1, 2 ou 3, onde 1 = baixa, 2 = média, 3 = alta, inferido a partir da urgência e importância descritas pelo usuário.
                    - Use por padrão "priority":2 (média) quando a urgência/impacto não estiverem claros.
                    - NÃO coloque o JSON em bloco de código, não use markdown após JSON_ACTION, apenas o objeto JSON puro.
                    - O JSON_ACTION deve ser SEMPRE a última e ÚNICA coisa da resposta, sem texto antes ou depois.
                    - DURAÇÃO PRECISA: Você deve sempre calcular e enviar o campo end no JSON_ACTION. Estime durações lógicas: Cafés/Pausas (15-20 min), Reuniões padrão (30-60 min), Deep Work (90-120 min). Nunca deixe o campo end vazio ou nulo.

                    REGRA DE OURO DE HORÁRIOS (OBRIGATÓRIA):
                    - VOCÊ É UM ASSISTENTE DE PRECISÃO. Antes de gerar qualquer JSON_ACTION, você deve realizar uma varredura OBRIGATÓRIA na lista "Tarefas de hoje e amanhã" enviada no contexto oculto.
                    - Se o horário de início (start) de uma nova tarefa colidir com QUALQUER minuto de uma tarefa existente hoje ou amanhã, você está PROIBIDO de usar "type":"CREATE_TASK".

                    COMPARAÇÃO COM A AGENDA (REGRA CRÍTICA):
                    - Leia atentamente a lista "Tarefas de hoje e amanhã" enviada no contexto oculto.
                    - Compare o horário que o usuário está pedindo para agendar com os horários dessas tarefas já agendadas (hoje ou amanhã).
                    - Se houver QUALQUER sobreposição de horário entre o pedido do usuário e uma tarefa existente (mesmo que parcial), você NÃO deve criar a tarefa diretamente.
                    - NESSA SITUAÇÃO, você DEVE usar exatamente "type":"CONFLICT_DETECTED" no JSON_ACTION.
                    - No campo "start" do JSON_ACTION com CONFLICT_DETECTED, coloque o PRÓXIMO horário LIVRE sugerido para hoje, sem colisão com as tarefas já agendadas.
                    - Em caso de conflito, procure especificamente o próximo slot livre de 1 (uma) hora IMEDIATAMENTE APÓS o término da última tarefa conflitante de hoje e use esse horário como "start" no JSON_ACTION com "type":"CONFLICT_DETECTED".
                    - Só use "type":"CREATE_TASK" quando o horário pedido NÃO tiver conflito com NENHUMA tarefa do dia de hoje.

                    Guia rápido para inferir prioridade (priority):
                    - PRIORIDADE 3 (ALTA/CRÍTICA): use quando houver prazos fatais ("hoje", "agora"), reuniões com liderança ("Diretor", "CEO"), ou atividades de alto impacto com consequência negativa imediata se não forem feitas.
                    - PRIORIDADE 2 (MÉDIA/ESTRUTURANTE): use para tarefas importantes, mas sem prazo imediato, trabalho focado (Deep Work), reuniões de alinhamento de time ou entregas semanais.
                    - PRIORIDADE 1 (BAIXA/ROTINA): use para atividades administrativas, lembretes simples, tarefas domésticas, café, pausas ou lazer.

                    DIRETRIZ DE PODER (prioridade vs. conflito):
                    - Se houver conflito entre uma tarefa de PRIORIDADE 1 (por exemplo, um café ou pausa) já existente na agenda e o usuário tentar agendar algo de PRIORIDADE 3 (por exemplo, reunião com Diretor ou CEO) em um horário específico, você NÃO deve sugerir como solução principal deslocar a tarefa de PRIORIDADE 3 para outro horário.
                    - Nessa situação, sua sugestão PRINCIPAL no TEXTO deve ser manter o horário solicitado pelo usuário para a tarefa de PRIORIDADE 3 e sugerir o deslocamento, remarcação ou cancelamento da tarefa antiga de PRIORIDADE 1.
                    - Você pode, opcionalmente, sugerir horários alternativos para a tarefa de PRIORIDADE 3 apenas como plano B, deixando claro que a recomendação prioritária é proteger o horário pedido para a tarefa crítica.
                    - Exemplo de mensagem: "Sua reunião com o CEO é crítica; mova seu café para mais tarde para priorizar o que realmente importa.".

                    Exemplo de negativa de conflito (NÃO mostre o rótulo "Usuário" na resposta final, use apenas a linha de JSON_ACTION):
                    - Usuário: "Café às 10h" (já existe tarefa às 10h).
                    - Resposta correta: JSON_ACTION: {"type":"CONFLICT_DETECTED","title":"Café","start":"2026-01-13T11:00:00","priority":1}

                    PERGUNTA DO USUÁRIO (use apenas para definir os campos do JSON ou para responder em texto no caso de CONSULTA, não repita literalmente o texto): %s

                    SUA RESPOSTA, QUANDO GERAR JSON_ACTION, DEVE SER APENAS UMA ÚNICA LINHA no seguinte formato exato,
                    sem qualquer explicação, comentário ou texto adicional antes ou depois.
                    Sua resposta DEVE OBRIGATORIAMENTE começar com o prefixo "JSON_ACTION: " seguido do objeto JSON em uma única linha.

                    Exemplo de saída CORRETA (apenas para referência de formato):
                    JSON_ACTION: {"type":"CREATE_TASK","title":"Diretor: Crítico","start":"2026-01-13T09:00:00","priority":3}

                        Não escreva nada além dessa linha de JSON_ACTION quando estiver gerando uma AÇÃO. Para CONSULTAS, responda apenas em texto natural, sem JSON_ACTION.
                    """
                    .formatted(now.toString(), dayOfWeek, taskContext, todayContext, request.message());

            log.debug("🔍 Enviando prompt completo para IA (tamanho={} chars)", prompt.length());
            log.info("===== PROMPT_ENVIADO_PARA_IA =====\n{}\n===== FIM_PROMPT_IA =====", prompt);

            log.info("Tentando chamada de IA com o modelo: {}", modelName);

            String response = chatModel.generate(prompt);

            // Pós-processamento: garantir que, se houver JSON_ACTION, apenas o bloco JSON
            // seja retornado
            String cleanedResponse = sanitizeChatResponse(response);

            log.info("✅ Resposta gerada com sucesso ({} caracteres)", cleanedResponse.length());

            return new AIChatResponseDTO(cleanedResponse, LocalDateTime.now());

        } catch (Exception e) {
            String rawMessage = e.getMessage() != null ? e.getMessage() : "Erro desconhecido na chamada da IA.";
            String simpleName = e.getClass().getSimpleName();

            boolean isOpenAiHttp = "OpenAiHttpException".equals(simpleName)
                    || simpleName.contains("OpenAiHttp");
            boolean isBalanceIssue = rawMessage.toLowerCase().contains("insufficient balance")
                    || rawMessage.contains("402");

            if (isOpenAiHttp && isBalanceIssue) {
                log.warn("⚠️ IA de chat indisponível por saldo insuficiente: {} - {}", simpleName, rawMessage);

                String friendly = "No momento, meu módulo de análise avançada está descansando. " +
                        "Mas você ainda pode agendar manualmente!";

                return new AIChatResponseDTO(friendly, LocalDateTime.now());
            }

            // Loga o erro completo sempre
            log.error("❌ Erro ao gerar resposta do chat: tipo={}, mensagem={}",
                    simpleName, rawMessage, e);

            // Se for um erro HTTP comum (401/403/429), repassa o conteúdo sem mascarar
            if (rawMessage.contains("401") || rawMessage.contains("403") || rawMessage.contains("429")) {
                throw new RuntimeException("Erro ao chamar a IA (detalhes): " + rawMessage, e);
            }

            if (rawMessage.contains("Configure sua chave de IA no backend para conversar comigo")) {
                throw new RuntimeException(rawMessage, e);
            }

            if (rawMessage.toLowerCase().contains("api key")) {
                throw new RuntimeException(
                        "Problema ao validar a chave da API do provedor de IA: " + rawMessage,
                        e);
            }

            if (rawMessage.toLowerCase().contains("timeout")) {
                throw new RuntimeException(
                        "A chamada para a IA expirou (timeout). Detalhes: " + rawMessage,
                        e);
            }

            if (rawMessage.toLowerCase().contains("rate limit")) {
                throw new RuntimeException(
                        "A IA retornou erro de rate limit. Detalhes: " + rawMessage,
                        e);
            }

            // Fallback: devolve a mensagem real para facilitar debug
            throw new RuntimeException("Erro genérico ao chamar a IA: " + rawMessage, e);
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private String buildAnalysisContext(List<Task> tasks, long total, long completed, long late, long scheduled,
            double rate) {
        return """
                Total de Tarefas: %d
                Tarefas Concluídas: %d
                Tarefas Atrasadas: %d
                Tarefas Agendadas: %d
                Taxa de Conclusão: %.1f%%

                DISTRIBUIÇÃO DE PRIORIDADES:
                %s
                """.formatted(total, completed, late, scheduled, rate, analyzePriorities(tasks));
    }

    private String analyzePriorities(List<Task> tasks) {
        long high = tasks.stream().filter(t -> "HIGH".equals(t.getPriority())).count();
        long medium = tasks.stream().filter(t -> "MEDIUM".equals(t.getPriority())).count();
        long low = tasks.stream().filter(t -> "LOW".equals(t.getPriority())).count();

        return "Alta: %d | Média: %d | Baixa: %d".formatted(high, medium, low);
    }

    private String buildTaskSummary(List<Task> tasks) {
        long pending = tasks.stream().filter(t -> t.getStatus() == TaskStatus.PENDING).count();
        long scheduled = tasks.stream().filter(t -> t.getStatus() == TaskStatus.SCHEDULED).count();
        long done = tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();

        return "To Do (sem horário): %d | Agendadas: %d | Concluídas: %d".formatted(pending, scheduled, done);
    }

    /**
     * Cria um resumo textual das tarefas de hoje e amanhã para uso como contexto
     * oculto no prompt da IA.
     */
    private String buildContextSummary(List<Task> contextTasks, LocalDate today) {
        if (contextTasks == null || contextTasks.isEmpty()) {
            return "Hoje e amanhã o usuário não têm tarefas agendadas no calendário.";
        }

        // Formato completo ISO local, alinhado com o campo "start" do JSON_ACTION
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        StringBuilder sb = new StringBuilder();
        LocalDate tomorrow = today.plusDays(1);

        sb.append("Contexto de tarefas entre hoje (")
                .append(today)
                .append(") e amanhã (")
                .append(tomorrow)
                .append("): ");

        sb.append("Principais tarefas (máximo 8, em ordem de data/hora): ");

        contextTasks.stream()
                .sorted(Comparator.comparing(
                        t -> t.getStartTime() != null ? t.getStartTime() : LocalDateTime.MAX))
                .limit(8)
                .forEach(task -> {
                    String priorityLabel = "";
                    TaskPriority priority = task.getPriority();
                    if (priority != null) {
                        switch (priority) {
                            case HIGH -> priorityLabel = "P3";
                            case MEDIUM -> priorityLabel = "P2";
                            case LOW -> priorityLabel = "P1";
                        }
                    }

                    sb.append("[");

                    if (!priorityLabel.isEmpty()) {
                        sb.append(priorityLabel).append(" - ");
                    }

                    LocalDateTime start = task.getStartTime();
                    LocalDateTime end = task.getEndTime();

                    if (start != null && end != null) {
                        sb.append(start.format(timeFormatter))
                                .append(" às ")
                                .append(end.format(timeFormatter))
                                .append(" (OCUPADO) - ");
                    } else if (start != null) {
                        sb.append(start.format(timeFormatter))
                                .append(" (OCUPADO) - ");
                    }

                    sb.append(task.getTitle() != null ? task.getTitle() : "(sem título)");

                    if (task.getPriority() != null) {
                        sb.append(" | prioridade ").append(task.getPriority().name());
                    }

                    if (task.getStatus() != null) {
                        sb.append(" | status ").append(task.getStatus().name());
                    }

                    sb.append("]; ");
                });

        return sb.toString().trim();
    }

    /**
     * Sanitiza uma string para uso no prompt, evitando quebras de linha ou
     * caracteres que possam atrapalhar serialização/logs. Mantém o conteúdo
     * semântico, apenas normalizando espaços.
     */
    private String sanitizeForPrompt(String raw) {
        if (raw == null) {
            return "";
        }
        return raw
                .replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String buildProductivityPrompt(String context) {
        return """
                Você é um especialista em produtividade cognitiva. Analise os seguintes dados:

                %s

                Forneça uma análise ESTRUTURADA neste formato EXATO:
                SUMMARY: [1-2 frases sobre o estado geral da produtividade]
                INSIGHTS: [Insight 1]; [Insight 2]; [Insight 3]
                RECOMMENDATIONS: [Recomendação 1]; [Recomendação 2]; [Recomendação 3]

                Seja específico, direto e acionável.
                """.formatted(context);
    }

    private String buildPatternsPrompt(String context) {
        return """
                Você é um analista de comportamento cognitivo. Identifique padrões nos dados:

                %s

                Forneça uma análise ESTRUTURADA neste formato EXATO:
                SUMMARY: [Principais padrões identificados]
                INSIGHTS: [Padrão 1]; [Padrão 2]; [Padrão 3]
                RECOMMENDATIONS: [Como otimizar padrão 1]; [Como otimizar padrão 2]; [Como otimizar padrão 3]

                Foque em carga cognitiva e time-blocking.
                """.formatted(context);
    }

    private String buildRecommendationsPrompt(String context) {
        return """
                Você é um coach de produtividade. Com base nos dados:

                %s

                Forneça recomendações ESTRUTURADAS neste formato EXATO:
                SUMMARY: [Diagnóstico principal]
                INSIGHTS: [Insight acionável 1]; [Insight acionável 2]; [Insight acionável 3]
                RECOMMENDATIONS: [Ação específica 1]; [Ação específica 2]; [Ação específica 3]

                Priorize ações de alto impacto e baixo esforço.
                """.formatted(context);
    }

    private String buildGeneralPrompt(String context) {
        return buildProductivityPrompt(context);
    }

    private AIAnalysisResponseDTO parseAIResponse(String aiResponse, int score) {
        try {
            String summary = extractSection(aiResponse, "SUMMARY:");
            List<String> insights = extractList(aiResponse, "INSIGHTS:");
            List<String> recommendations = extractList(aiResponse, "RECOMMENDATIONS:");

            return new AIAnalysisResponseDTO(
                    summary,
                    insights,
                    recommendations,
                    score,
                    LocalDateTime.now());
        } catch (Exception e) {
            log.warn("⚠️ Erro ao parsear resposta da IA. Retornando formato padrão.");
            return new AIAnalysisResponseDTO(
                    aiResponse.substring(0, Math.min(200, aiResponse.length())),
                    List.of("Análise em andamento..."),
                    List.of("Continue registrando suas tarefas para insights mais precisos."),
                    score,
                    LocalDateTime.now());
        }
    }

    /**
     * Sanitiza a resposta do chat da IA para evitar que textos extras quebrem o
     * JSON.
     *
     * - Se contiver JSON_ACTION:, extrai apenas o objeto JSON e o retorna em uma
     * única linha
     * no formato: "JSON_ACTION: { ... }" (sem textos antes/depois).
     * - Se não contiver JSON_ACTION:, devolve o texto original (caso de CONSULTA).
     */
    private String sanitizeChatResponse(String rawResponse) {
        if (rawResponse == null) {
            return "";
        }

        String trimmed = rawResponse.trim();
        String upper = trimmed.toUpperCase();

        if (!upper.contains("JSON_ACTION:")) {
            // CONSULTA: apenas texto, sem JSON_ACTION
            return trimmed;
        }

        // Tenta isolar JSON_ACTION e o objeto JSON associado, ignorando textos extras
        Pattern pattern = Pattern.compile("(?i)JSON_ACTION:\\s*(\\{[\\s\\S]*?\\})");
        Matcher matcher = pattern.matcher(trimmed);

        if (matcher.find()) {
            String jsonObject = matcher.group(1);
            // Compacta quebras de linha e espaços múltiplos dentro do JSON
            String compactJson = jsonObject.replaceAll("\\s+", " ").trim();
            return "JSON_ACTION: " + compactJson;
        }

        // Fallback: se não conseguir achar o objeto JSON, retorna o texto original
        // aparado
        return trimmed;
    }

    private String extractSection(String text, String marker) {
        int start = text.indexOf(marker);
        if (start == -1)
            return "Análise em processamento...";

        start += marker.length();
        int end = text.indexOf("\n", start);
        if (end == -1)
            end = text.length();

        return text.substring(start, end).trim();
    }

    private List<String> extractList(String text, String marker) {
        String section = extractSection(text, marker);
        if (section.isEmpty())
            return new ArrayList<>();

        String[] items = section.split(";");
        List<String> result = new ArrayList<>();
        for (String item : items) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
