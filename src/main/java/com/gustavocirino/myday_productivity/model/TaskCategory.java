package com.gustavocirino.myday_productivity.model;

import com.gustavocirino.myday_productivity.model.enums.TaskCategoryType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Classificação automática de uma tarefa gerada pelo módulo de Triagem de Tarefas.
 *
 * <p>Inspirado no módulo de Classificação de OS do SpinOps, a IA analisa o título,
 * descrição, prazo, peso cognitivo e contexto histórico do usuário para categorizar
 * cada tarefa segundo a Matriz de Eisenhower expandida (URGENT, IMPORTANT, ROUTINE,
 * DELEGABLE) e gerar uma explicação em linguagem natural ("Por quê?") justificando
 * aquela classificação.</p>
 *
 * <p>Relacionamento 1:1 com {@link Task}. Uma tarefa pode ter apenas uma categoria
 * ativa. O usuário pode sobrescrever manualmente a classificação da IA.</p>
 */
@Entity
@Table(name = "tb_task_categories", indexes = {
    @Index(name = "idx_task_categories_task_id", columnList = "task_id")
})
public class TaskCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tarefa que recebeu esta classificação.
     * Relacionamento exclusivo: cada tarefa possui no máximo uma categoria ativa.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false, unique = true)
    private Task task;

    /**
     * Categoria atribuída pela IA.
     * URGENT → ação imediata | IMPORTANT → valor futuro |
     * ROUTINE → delegável/urgente | DELEGABLE → eliminar ou delegar.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category_type", nullable = false)
    private TaskCategoryType categoryType;

    /**
     * Explicação em linguagem natural gerada pela IA respondendo "Por quê esta categoria?".
     * Exemplo: "Esta tarefa tem prazo em menos de 2 horas e impacta diretamente
     * a entrega do cliente X, caracterizando urgência real."
     */
    @Column(nullable = false, length = 1000)
    private String explanation;

    /**
     * Grau de confiança da IA na classificação (0.0 = incerto, 1.0 = certeza total).
     * Scores abaixo de 0.6 podem indicar que o usuário deve revisar manualmente.
     */
    @Column(name = "confidence_score", columnDefinition = "DECIMAL(5,4)")
    private Double confidenceScore;

    /**
     * Indica se o usuário substituiu a classificação automática da IA.
     * Quando {@code true}, a categoria foi definida pelo usuário, não pela IA.
     */
    @Column(name = "manual_override", nullable = false)
    private Boolean manualOverride = false;

    /**
     * Momento em que a IA classificou a tarefa pela última vez.
     */
    @Column(name = "classified_at", nullable = false)
    private LocalDateTime classifiedAt;

    /**
     * Momento em que o usuário sobrescreveu a classificação da IA (se aplicável).
     */
    @Column(name = "overridden_at")
    private LocalDateTime overriddenAt;

    @PrePersist
    protected void onCreate() {
        if (classifiedAt == null) {
            classifiedAt = LocalDateTime.now();
        }
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    public TaskCategoryType getCategoryType() { return categoryType; }
    public void setCategoryType(TaskCategoryType categoryType) { this.categoryType = categoryType; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    public Boolean getManualOverride() { return manualOverride; }
    public void setManualOverride(Boolean manualOverride) { this.manualOverride = manualOverride; }

    public LocalDateTime getClassifiedAt() { return classifiedAt; }
    public void setClassifiedAt(LocalDateTime classifiedAt) { this.classifiedAt = classifiedAt; }

    public LocalDateTime getOverriddenAt() { return overriddenAt; }
    public void setOverriddenAt(LocalDateTime overriddenAt) { this.overriddenAt = overriddenAt; }
}
