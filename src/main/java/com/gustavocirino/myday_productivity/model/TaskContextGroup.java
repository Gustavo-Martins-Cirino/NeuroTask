package com.gustavocirino.myday_productivity.model;

import com.gustavocirino.myday_productivity.model.enums.ContextType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Grupo de contexto para agrupamento inteligente de tarefas (Batching).
 *
 * <p>Inspirado no módulo de Clustering K-Means do SpinOps, esta entidade representa
 * um "bloco de foco" composto por tarefas de natureza similar. O algoritmo de
 * clustering agrupa tarefas com o mesmo {@link ContextType} (ex.: todas as ligações
 * do dia, todos os e-mails pendentes) em um único bloco contíguo no calendário,
 * reduzindo o custo cognitivo de trocar de contexto e mantendo o usuário em estado
 * de "flow".</p>
 *
 * <p>O campo {@code centroidVector} armazena o centroide do cluster em formato JSON
 * (vetor de features normalizado) para que o algoritmo possa refinar os agrupamentos
 * iterativamente sem recalcular o histórico completo a cada execução.</p>
 */
@Entity
@Table(name = "tb_task_context_groups", indexes = {
    @Index(name = "idx_context_group_user_id", columnList = "user_id"),
    @Index(name = "idx_context_group_context_type", columnList = "context_type")
})
public class TaskContextGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuário dono deste grupo de contexto.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Nome descritivo do grupo gerado pela IA ou definido pelo usuário.
     * Exemplos: "Bloco de Ligações da Manhã", "Sprint de E-mails".
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Tipo de contexto que define a natureza das tarefas agrupadas.
     * Usado como dimensão principal pelo algoritmo de clustering.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "context_type", nullable = false)
    private ContextType contextType;

    /**
     * Cor hexadecimal de identificação visual do bloco no calendário.
     * Cada ContextType pode ter uma cor padrão sugerida (ex.: CALLS = #4A90E2).
     */
    @Column(name = "color_hex", length = 20)
    private String colorHex;

    /**
     * Descrição do grupo gerada pela IA explicando a lógica de agrupamento
     * e o benefício cognitivo estimado de executar estas tarefas em sequência.
     */
    @Column(length = 500)
    private String description;

    /**
     * Duração sugerida pela IA para o bloco de foco (em minutos).
     * Calculada com base na soma de {@code Task.estimatedDurationMinutes} das tarefas
     * do grupo, com folga de 10% para transições.
     */
    @Column(name = "suggested_focus_duration_minutes")
    private Integer suggestedFocusDurationMinutes;

    /**
     * Tarefas que compõem este grupo de contexto.
     * Uma tarefa pode temporariamente pertencer a mais de um grupo durante
     * uma reclusterização, mas idealmente deve ter apenas um grupo ativo.
     */
    @ManyToMany
    @JoinTable(
        name = "context_group_tasks",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "task_id")
    )
    private List<Task> tasks = new ArrayList<>();

    /**
     * Vetor centroide do cluster serializado como JSON.
     * Formato: {"weight": 0.7, "estimatedMinutes": 0.4, "priority": 0.9, ...}
     * Atualizado a cada ciclo de reclusterização pelo serviço de batching.
     */
    @Column(name = "centroid_vector", length = 500)
    private String centroidVector;

    /**
     * Timestamp de criação do grupo.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Última vez que o algoritmo recalculou os clusters e redistribuiu as tarefas.
     * Nulo se o grupo ainda não passou por uma reclusterização desde a criação.
     */
    @Column(name = "last_optimized_at")
    private LocalDateTime lastOptimizedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ContextType getContextType() { return contextType; }
    public void setContextType(ContextType contextType) { this.contextType = contextType; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getSuggestedFocusDurationMinutes() { return suggestedFocusDurationMinutes; }
    public void setSuggestedFocusDurationMinutes(Integer suggestedFocusDurationMinutes) { this.suggestedFocusDurationMinutes = suggestedFocusDurationMinutes; }

    public List<Task> getTasks() { return tasks; }
    public void setTasks(List<Task> tasks) { this.tasks = tasks; }

    public String getCentroidVector() { return centroidVector; }
    public void setCentroidVector(String centroidVector) { this.centroidVector = centroidVector; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastOptimizedAt() { return lastOptimizedAt; }
    public void setLastOptimizedAt(LocalDateTime lastOptimizedAt) { this.lastOptimizedAt = lastOptimizedAt; }
}
