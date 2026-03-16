package com.gustavocirino.myday_productivity.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Relatório semanal de produtividade gerado pela IA.
 * Armazena o conteúdo do review para consulta posterior.
 */
@Entity
@Table(name = "tb_weekly_reviews")
public class WeeklyReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "week_end", nullable = false)
    private LocalDate weekEnd;

    /** Tarefas planejadas na semana */
    @Column(nullable = false)
    private Integer totalPlanned = 0;

    /** Tarefas concluídas na semana */
    @Column(nullable = false)
    private Integer totalCompleted = 0;

    /** Total de minutos em sessões de foco */
    @Column(nullable = false)
    private Integer totalFocusMinutes = 0;

    /** Score de produtividade (0-100) */
    @Column(nullable = false)
    private Integer productivityScore = 0;

    /** Resumo gerado pela IA */
    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    /** Insights gerados pela IA (separados por ;) */
    @Column(columnDefinition = "TEXT")
    private String aiInsights;

    /** Recomendações geradas pela IA (separadas por ;) */
    @Column(columnDefinition = "TEXT")
    private String aiRecommendations;

    /** Melhor horário de produtividade detectado */
    @Column(length = 50)
    private String peakProductivityTime;

    private LocalDateTime createdAt;

    public WeeklyReview() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDate getWeekStart() { return weekStart; }
    public void setWeekStart(LocalDate weekStart) { this.weekStart = weekStart; }

    public LocalDate getWeekEnd() { return weekEnd; }
    public void setWeekEnd(LocalDate weekEnd) { this.weekEnd = weekEnd; }

    public Integer getTotalPlanned() { return totalPlanned; }
    public void setTotalPlanned(Integer totalPlanned) { this.totalPlanned = totalPlanned; }

    public Integer getTotalCompleted() { return totalCompleted; }
    public void setTotalCompleted(Integer totalCompleted) { this.totalCompleted = totalCompleted; }

    public Integer getTotalFocusMinutes() { return totalFocusMinutes; }
    public void setTotalFocusMinutes(Integer totalFocusMinutes) { this.totalFocusMinutes = totalFocusMinutes; }

    public Integer getProductivityScore() { return productivityScore; }
    public void setProductivityScore(Integer productivityScore) { this.productivityScore = productivityScore; }

    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }

    public String getAiInsights() { return aiInsights; }
    public void setAiInsights(String aiInsights) { this.aiInsights = aiInsights; }

    public String getAiRecommendations() { return aiRecommendations; }
    public void setAiRecommendations(String aiRecommendations) { this.aiRecommendations = aiRecommendations; }

    public String getPeakProductivityTime() { return peakProductivityTime; }
    public void setPeakProductivityTime(String peakProductivityTime) { this.peakProductivityTime = peakProductivityTime; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
