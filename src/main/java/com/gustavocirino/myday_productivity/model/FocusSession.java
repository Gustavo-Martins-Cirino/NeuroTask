package com.gustavocirino.myday_productivity.model;

import com.gustavocirino.myday_productivity.model.enums.FocusSessionStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Sessão de foco (Pomodoro) vinculada a uma tarefa.
 * Registra quando o usuário entra em modo foco, pausas e conclusão.
 */
@Entity
@Table(name = "tb_focus_sessions")
public class FocusSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FocusSessionStatus status;

    /** Duração planejada do foco em minutos (ex: 25) */
    @Column(nullable = false)
    private Integer plannedMinutes;

    /** Duração real trabalhada em segundos */
    @Column(nullable = false)
    private Long actualSeconds = 0L;

    /** Duração do intervalo em minutos (ex: 5) */
    @Column(nullable = false)
    private Integer breakMinutes = 5;

    /** Número do ciclo dentro do grupo (1, 2, 3, 4...) */
    @Column(nullable = false)
    private Integer cycleNumber = 1;

    /** Total de ciclos planejados nesta sequência */
    @Column(nullable = false)
    private Integer totalCycles = 4;

    private LocalDateTime startedAt;
    private LocalDateTime pausedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    public FocusSession() {
        this.createdAt = LocalDateTime.now();
        this.status = FocusSessionStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public FocusSessionStatus getStatus() { return status; }
    public void setStatus(FocusSessionStatus status) { this.status = status; }

    public Integer getPlannedMinutes() { return plannedMinutes; }
    public void setPlannedMinutes(Integer plannedMinutes) { this.plannedMinutes = plannedMinutes; }

    public Long getActualSeconds() { return actualSeconds; }
    public void setActualSeconds(Long actualSeconds) { this.actualSeconds = actualSeconds; }

    public Integer getBreakMinutes() { return breakMinutes; }
    public void setBreakMinutes(Integer breakMinutes) { this.breakMinutes = breakMinutes; }

    public Integer getCycleNumber() { return cycleNumber; }
    public void setCycleNumber(Integer cycleNumber) { this.cycleNumber = cycleNumber; }

    public Integer getTotalCycles() { return totalCycles; }
    public void setTotalCycles(Integer totalCycles) { this.totalCycles = totalCycles; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getPausedAt() { return pausedAt; }
    public void setPausedAt(LocalDateTime pausedAt) { this.pausedAt = pausedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
