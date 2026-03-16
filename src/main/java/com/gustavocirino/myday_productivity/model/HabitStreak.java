package com.gustavocirino.myday_productivity.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Registro de streak (sequência) de hábitos produtivos.
 * Rastreia dias consecutivos em que o usuário cumpre metas.
 */
@Entity
@Table(name = "tb_habit_streaks")
public class HabitStreak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Tipo de hábito rastreado (ex: "daily_completion", "early_finish", "focus_sessions") */
    @Column(nullable = false, length = 50)
    private String habitType;

    /** Quantidade atual de dias consecutivos */
    @Column(nullable = false)
    private Integer currentStreak = 0;

    /** Maior streak já atingido */
    @Column(nullable = false)
    private Integer bestStreak = 0;

    /** Último dia em que o hábito foi cumprido */
    private LocalDate lastCompletedDate;

    /** Total acumulado de vezes que o hábito foi cumprido */
    @Column(nullable = false)
    private Integer totalCompletions = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public HabitStreak() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getHabitType() { return habitType; }
    public void setHabitType(String habitType) { this.habitType = habitType; }

    public Integer getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(Integer currentStreak) { this.currentStreak = currentStreak; }

    public Integer getBestStreak() { return bestStreak; }
    public void setBestStreak(Integer bestStreak) { this.bestStreak = bestStreak; }

    public LocalDate getLastCompletedDate() { return lastCompletedDate; }
    public void setLastCompletedDate(LocalDate lastCompletedDate) { this.lastCompletedDate = lastCompletedDate; }

    public Integer getTotalCompletions() { return totalCompletions; }
    public void setTotalCompletions(Integer totalCompletions) { this.totalCompletions = totalCompletions; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
