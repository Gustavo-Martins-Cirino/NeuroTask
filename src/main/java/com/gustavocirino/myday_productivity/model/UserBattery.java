package com.gustavocirino.myday_productivity.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gustavocirino.myday_productivity.model.enums.BurnoutRiskLevel;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Snapshot periódico da "Bateria Social/Foco" do usuário.
 *
 * <p>Inspirado no módulo de Saúde do Equipamento do SpinOps, esta entidade
 * armazena leituras históricas do estado cognitivo do usuário para que a IA
 * possa detectar tendências de degradação, prever risco de burnout e sugerir
 * pausas preventivas com base no peso acumulado das tarefas agendadas.</p>
 *
 * <p>Cada registro representa um instantâneo calculado em um momento específico.
 * O histórico de registros forma a série temporal usada para análise de tendências.</p>
 */
@Entity
@Table(name = "tb_user_battery", indexes = {
    @Index(name = "idx_user_battery_user_id", columnList = "user_id"),
    @Index(name = "idx_user_battery_recorded_at", columnList = "recorded_at")
})
public class UserBattery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuário ao qual este snapshot pertence.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    /**
     * Nível da bateria cognitiva estimado (0–100).
     * 100 = totalmente descansado e focado. 0 = completamente esgotado.
     * Calculado combinando carga cognitiva, histórico de pausas e completude de tarefas.
     */
    @Column(name = "battery_level", nullable = false)
    private Integer batteryLevel;

    /**
     * Carga cognitiva atual (0–100).
     * Derivada da soma ponderada do peso ({@code Task.weight}) das tarefas
     * com status SCHEDULED no período atual.
     */
    @Column(name = "cognitive_load", nullable = false)
    private Integer cognitiveLoad;

    /**
     * Soma do peso cognitivo de todas as tarefas agendadas no dia corrente.
     * Usada como entrada principal para o cálculo de burnoutRiskScore.
     */
    @Column(name = "total_task_weight", nullable = false)
    private Integer totalTaskWeight;

    /**
     * Score de risco de burnout (0.0 = nenhum risco, 1.0 = burnout iminente).
     * Calculado pela IA com base em: carga acumulada vs. média histórica,
     * frequência de adiamentos e redução de completude ao longo do tempo.
     */
    @Column(name = "burnout_risk_score", columnDefinition = "DECIMAL(5,4)")
    private Double burnoutRiskScore;

    /**
     * Classificação qualitativa do risco derivada do burnoutRiskScore.
     * LOW < 0.25, MODERATE 0.25–0.50, HIGH 0.50–0.75, CRITICAL > 0.75.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private BurnoutRiskLevel riskLevel;

    /**
     * Duração de pausa sugerida pela IA (em minutos).
     * Nulo quando o risco é LOW. Aumenta progressivamente com o risco.
     */
    @Column(name = "suggested_break_minutes")
    private Integer suggestedBreakMinutes;

    /**
     * Análise narrativa gerada pela IA explicando o estado atual da bateria
     * e recomendações personalizadas para o usuário.
     */
    @Column(name = "ai_notes", length = 1000)
    private String aiNotes;

    /**
     * Timestamp do momento em que este snapshot foi registrado.
     */
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Integer getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(Integer batteryLevel) { this.batteryLevel = batteryLevel; }

    public Integer getCognitiveLoad() { return cognitiveLoad; }
    public void setCognitiveLoad(Integer cognitiveLoad) { this.cognitiveLoad = cognitiveLoad; }

    public Integer getTotalTaskWeight() { return totalTaskWeight; }
    public void setTotalTaskWeight(Integer totalTaskWeight) { this.totalTaskWeight = totalTaskWeight; }

    public Double getBurnoutRiskScore() { return burnoutRiskScore; }
    public void setBurnoutRiskScore(Double burnoutRiskScore) { this.burnoutRiskScore = burnoutRiskScore; }

    public BurnoutRiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(BurnoutRiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public Integer getSuggestedBreakMinutes() { return suggestedBreakMinutes; }
    public void setSuggestedBreakMinutes(Integer suggestedBreakMinutes) { this.suggestedBreakMinutes = suggestedBreakMinutes; }

    public String getAiNotes() { return aiNotes; }
    public void setAiNotes(String aiNotes) { this.aiNotes = aiNotes; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
