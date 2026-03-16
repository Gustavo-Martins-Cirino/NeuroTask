package com.gustavocirino.myday_productivity.service;

import com.gustavocirino.myday_productivity.dto.WeeklyReviewDTO;
import com.gustavocirino.myday_productivity.model.FocusSession;
import com.gustavocirino.myday_productivity.model.Task;
import com.gustavocirino.myday_productivity.model.User;
import com.gustavocirino.myday_productivity.model.WeeklyReview;
import com.gustavocirino.myday_productivity.model.enums.FocusSessionStatus;
import com.gustavocirino.myday_productivity.model.enums.TaskStatus;
import com.gustavocirino.myday_productivity.repository.FocusSessionRepository;
import com.gustavocirino.myday_productivity.repository.TaskRepository;
import com.gustavocirino.myday_productivity.repository.WeeklyReviewRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeeklyReviewService {

    private final WeeklyReviewRepository weeklyReviewRepository;
    private final TaskRepository taskRepository;
    private final FocusSessionRepository focusSessionRepository;
    private final ChatLanguageModel chatModel;

    @Transactional
    public WeeklyReviewDTO generateReview(User user) {
        return generateReview(user, false);
    }

    @Transactional
    public WeeklyReviewDTO generateReview(User user, boolean forceRegenerate) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        if (forceRegenerate) {
            weeklyReviewRepository.findByUserIdAndWeekStart(user.getId(), weekStart)
                    .ifPresent(weeklyReviewRepository::delete);
            weeklyReviewRepository.flush();
            return createNewReview(user, weekStart, weekEnd);
        }

        // Check if review already exists for this week
        return weeklyReviewRepository.findByUserIdAndWeekStart(user.getId(), weekStart)
                .map(this::toDTO)
                .orElseGet(() -> createNewReview(user, weekStart, weekEnd));
    }

    public List<WeeklyReviewDTO> getReviewHistory(User user) {
        return weeklyReviewRepository.findTop4ByUserIdOrderByWeekStartDesc(user.getId())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public WeeklyReviewDTO getReviewById(Long id, User user) {
        WeeklyReview review = weeklyReviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review não encontrada"));
        if (!review.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Acesso negado");
        }
        return toDTO(review);
    }

    private WeeklyReviewDTO createNewReview(User user, LocalDate weekStart, LocalDate weekEnd) {
        LocalDateTime startDT = weekStart.atStartOfDay();
        LocalDateTime endDT = weekEnd.atTime(LocalTime.MAX);

        // Gather week statistics
        List<Task> weekTasks = taskRepository.findByUserId(user.getId()).stream()
                .filter(t -> {
                    // Usa startTime como referência (para tarefas agendadas esta semana
                    // mas criadas antes) ou createdAt caso startTime não exista.
                    LocalDateTime ref = t.getStartTime() != null ? t.getStartTime() : t.getCreatedAt();
                    return ref != null && !ref.isBefore(startDT) && !ref.isAfter(endDT);
                })
                .toList();

        int totalPlanned = weekTasks.size();
        int totalCompleted = (int) weekTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .count();

        // Focus session minutes
        List<FocusSession> focusSessions = focusSessionRepository
                .findByUserIdAndStatusAndStartedAtBetween(
                        user.getId(), FocusSessionStatus.COMPLETED, startDT, endDT);
        int totalFocusMinutes = (int) focusSessions.stream()
                .mapToLong(s -> s.getActualSeconds() / 60)
                .sum();

        int productivityScore = totalPlanned > 0
                ? (int) ((totalCompleted * 100.0) / totalPlanned)
                : 0;

        // Determine peak productivity time
        String peakTime = determinePeakTime(weekTasks);

        // Generate AI analysis
        String context = buildWeekContext(totalPlanned, totalCompleted, totalFocusMinutes,
                productivityScore, peakTime, weekTasks);

        String aiSummary;
        List<String> aiInsights;
        List<String> aiRecommendations;

        try {
            String prompt = buildReviewPrompt(context);
            String aiResponse = chatModel.generate(prompt);
            aiSummary = extractSection(aiResponse, "SUMMARY:");
            aiInsights = extractList(aiResponse, "INSIGHTS:");
            aiRecommendations = extractList(aiResponse, "RECOMMENDATIONS:");
        } catch (Exception e) {
            log.warn("⚠️ IA indisponível para review semanal: {}", e.getMessage());
            aiSummary = String.format("Dados reais: você concluiu %d de %d tarefas na semana (%d%%), com %d minutos de foco e pico de produtividade na %s.",
                    totalCompleted, totalPlanned,
                    productivityScore, totalFocusMinutes, peakTime.toLowerCase());
            aiInsights = List.of(
                    String.format("Seu padrão de pico foi na %s.", peakTime.toLowerCase()),
                    String.format("Você manteve taxa de conclusão de %d%% na semana.", productivityScore),
                    String.format("Foram %d minutos de foco registrados.", totalFocusMinutes)
            );
            aiRecommendations = List.of(
                    "Bloqueie no calendário as tarefas críticas no seu período de maior energia.",
                    "Defina 1 tarefa de alta prioridade para concluir antes do meio-dia no próximo dia útil.",
                    "Revise diariamente ao fim da tarde o que ficou pendente para evitar acúmulo."
            );
        }

        WeeklyReview review = new WeeklyReview();
        review.setUser(user);
        review.setWeekStart(weekStart);
        review.setWeekEnd(weekEnd);
        review.setTotalPlanned(totalPlanned);
        review.setTotalCompleted(totalCompleted);
        review.setTotalFocusMinutes(totalFocusMinutes);
        review.setProductivityScore(productivityScore);
        review.setAiSummary(aiSummary);
        review.setAiInsights(String.join(";", aiInsights));
        review.setAiRecommendations(String.join(";", aiRecommendations));
        review.setPeakProductivityTime(peakTime);

        WeeklyReview saved = weeklyReviewRepository.save(review);
        log.info("📊 Weekly review gerada: semana {} - score {}%", weekStart, productivityScore);
        return toDTO(saved);
    }

    private String determinePeakTime(List<Task> tasks) {
        long morning = tasks.stream()
                .filter(t -> t.getStartTime() != null && t.getStatus() == TaskStatus.DONE)
                .filter(t -> t.getStartTime().getHour() < 12)
                .count();
        long afternoon = tasks.stream()
                .filter(t -> t.getStartTime() != null && t.getStatus() == TaskStatus.DONE)
                .filter(t -> t.getStartTime().getHour() >= 12 && t.getStartTime().getHour() < 18)
                .count();
        long evening = tasks.stream()
                .filter(t -> t.getStartTime() != null && t.getStatus() == TaskStatus.DONE)
                .filter(t -> t.getStartTime().getHour() >= 18)
                .count();

        if (morning >= afternoon && morning >= evening) return "Manhã";
        if (afternoon >= evening) return "Tarde";
        return "Noite";
    }

    private String buildWeekContext(int planned, int completed, int focusMin,
                                    int score, String peakTime, List<Task> tasks) {
        long highPriority = tasks.stream()
                .filter(t -> t.getPriority() != null && t.getPriority().name().equals("HIGH"))
                .count();

        return """
                Resumo da semana:
                - Tarefas planejadas: %d
                - Tarefas completadas: %d
                - Taxa de conclusão: %d%%
                - Minutos em foco: %d
                - Horário de pico: %s
                - Tarefas de alta prioridade: %d
                """.formatted(planned, completed, score, focusMin, peakTime, highPriority);
    }

    private String buildReviewPrompt(String context) {
        return """
                                Você é uma IA analista de produtividade baseada em dados reais do NeuroTask.
                                Analise os dados brutos abaixo e forneça uma revisão estratégica.
                                
                                DADOS REAIS DO USUÁRIO:
                %s

                                REGRAS DE OURO (Se violar, a resposta será rejeitada):
                                1. PROIBIDO conselhos genéricos como "beba água", "use pomodoro", "planeje o dia".
                                2. USE OS DADOS: Se o pico é Manhã, diga "Como seu pico é Manhã, agende X...". Se a taxa é 30%%, pergunte o motivo.
                                3. Se houver tarefas de alta prioridade, OBRIGATORIAMENTE mencione estratégia para elas.
                                4. O tom deve ser analítico e "coach", não "chatbot de suporte".

                Forneça a resposta NESTE FORMATO EXATO:
                SUMMARY: [Resumo direto usando %s e números cruciais. Ex: "Você performou bem com 80%% de conclusão..."]
                INSIGHTS: [Padrão observado 1]; [Padrão observado 2 (ex: relação entre foco e tarefas)]; [Padrão 3]
                RECOMMENDATIONS: [Ação específica baseada no pico de produtividade]; [Ação para resolver tarefas pendentes]; [Ação estratégica para próxima semana]

                                Responda em português natural, sem marcadores tipo **bold** no texto cru, apenas texto limpo.
                """.formatted(context, "%");
    }

    private String extractSection(String text, String marker) {
        int start = text.indexOf(marker);
        if (start == -1) return "Análise em processamento...";
        start += marker.length();
        int end = text.indexOf("\n", start);
        if (end == -1) end = text.length();
        return text.substring(start, end).trim();
    }

    private List<String> extractList(String text, String marker) {
        String section = extractSection(text, marker);
        if (section.isEmpty()) return new ArrayList<>();
        return Arrays.stream(section.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private WeeklyReviewDTO toDTO(WeeklyReview r) {
        List<String> insights = r.getAiInsights() != null
                ? Arrays.stream(r.getAiInsights().split(";")).map(String::trim).filter(s -> !s.isEmpty()).toList()
                : List.of();
        List<String> recommendations = r.getAiRecommendations() != null
                ? Arrays.stream(r.getAiRecommendations().split(";")).map(String::trim).filter(s -> !s.isEmpty()).toList()
                : List.of();

        return new WeeklyReviewDTO(
                r.getId(),
                r.getWeekStart(),
                r.getWeekEnd(),
                r.getTotalPlanned(),
                r.getTotalCompleted(),
                r.getTotalFocusMinutes(),
                r.getProductivityScore(),
                r.getAiSummary(),
                insights,
                recommendations,
                r.getPeakProductivityTime(),
                r.getCreatedAt()
        );
    }
}
