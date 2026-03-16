package com.gustavocirino.myday_productivity.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Registro de evento de adiamento de uma tarefa, usado pelo Rastreador de Procrastinação.
 *
 * <p>Inspirado no módulo de Detecção de Anomalias do SpinOps, cada vez que uma tarefa
 * é movida de volta para o backlog ou reagendada para um horário posterior, um registro
 * é criado ou atualizado aqui. O serviço de análise calcula o Z-score do adiamento
 * comparando o padrão atual com a média histórica do usuário para aquele tipo de
 * tarefa, identificando quando o adiamento foge estatisticamente do comportamento
 * normal (anomalia = procrastinação real).</p>
 *
 * <p>Quando {@code isAnomaly = true}, a IA gera uma sugestão para quebrar a tarefa
 * em passos menores e reduzir a resistência de execução.</p>
 */
@Entity
@Table(name = "tb_procrastination_records", indexes = {
    @Index(name = "idx_procrastination_task_id", columnList = "task_id"),
    @Index(name = "idx_procrastination_user_id", columnList = "user_id"),
    @Index(name = "idx_procrastination_is_anomaly", columnList = "is_anomaly")
})
public class ProcrastinationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tarefa sendo monitorada por adiamentos recorrentes.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    /**
     * Usuário dono da tarefa (desnormalizado para queries diretas por usuário).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Data/hora em que a tarefa estava originalmente agendada para execução.
     * Serve como baseline para calcular o delta de adiamento.
     */
    @Column(name = "original_scheduled_at")
    private LocalDateTime originalScheduledAt;

    /**
     * Data/hora para a qual a tarefa foi reagendada no último adiamento.
     * Nulo se a tarefa foi movida para o backlog sem nova data definida.
     */
    @Column(name = "rescheduled_at")
    private LocalDateTime rescheduledAt;

    /**
     * Número acumulado de vezes que esta tarefa foi adiada pelo usuário.
     * Incrementado a cada novo evento de adiamento.
     */
    @Column(name = "postpone_count", nullable = false)
    private Integer postponeCount = 0;

    /**
     * Z-score calculado para este adiamento.
     * Z = (x − μ) / σ, onde x = adiamentos desta tarefa,
     * μ = média de adiamentos do usuário e σ = desvio padrão histórico.
     * Valores |Z| > 2.0 são considerados anomalias estatisticamente significativas.
     */
    @Column(name = "z_score")
    private Double zScore;

    /**
     * Indica se o Z-score ultrapassou o limiar de anomalia (|Z| > 2.0).
     * Quando {@code true}, a tarefa foi detectada como pauta de procrastinação real.
     */
    @Column(name = "is_anomaly", nullable = false)
    private Boolean isAnomaly = false;

    /**
     * Indica se a IA já gerou uma sugestão de decomposição para esta anomalia.
     * Evita duplicar sugestões ao usuário para o mesmo episódio.
     */
    @Column(name = "suggestion_generated", nullable = false)
    private Boolean suggestionGenerated = false;

    /**
     * Sugestão gerada pela IA para reduzir a resistência de execução.
     * Geralmente contém uma proposta de divisão da tarefa em 3–5 micro-passos
     * com tempo estimado menor que 25 minutos cada (compatível com Pomodoro).
     */
    @Column(name = "ai_suggestion", length = 2000)
    private String aiSuggestion;

    /**
     * Timestamp de quando este registro de adiamento foi criado.
     */
    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @PrePersist
    protected void onCreate() {
        if (detectedAt == null) {
            detectedAt = LocalDateTime.now();
        }
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getOriginalScheduledAt() { return originalScheduledAt; }
    public void setOriginalScheduledAt(LocalDateTime originalScheduledAt) { this.originalScheduledAt = originalScheduledAt; }

    public LocalDateTime getRescheduledAt() { return rescheduledAt; }
    public void setRescheduledAt(LocalDateTime rescheduledAt) { this.rescheduledAt = rescheduledAt; }

    public Integer getPostponeCount() { return postponeCount; }
    public void setPostponeCount(Integer postponeCount) { this.postponeCount = postponeCount; }

    public Double getZScore() { return zScore; }
    public void setZScore(Double zScore) { this.zScore = zScore; }

    public Boolean getIsAnomaly() { return isAnomaly; }
    public void setIsAnomaly(Boolean isAnomaly) { this.isAnomaly = isAnomaly; }

    public Boolean getSuggestionGenerated() { return suggestionGenerated; }
    public void setSuggestionGenerated(Boolean suggestionGenerated) { this.suggestionGenerated = suggestionGenerated; }

    public String getAiSuggestion() { return aiSuggestion; }
    public void setAiSuggestion(String aiSuggestion) { this.aiSuggestion = aiSuggestion; }

    public LocalDateTime getDetectedAt() { return detectedAt; }
    public void setDetectedAt(LocalDateTime detectedAt) { this.detectedAt = detectedAt; }
}
