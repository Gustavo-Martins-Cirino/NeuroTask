package com.gustavocirino.myday_productivity.model;

import com.gustavocirino.myday_productivity.model.enums.DayPeriod;
import com.gustavocirino.myday_productivity.model.enums.EnergyLevel;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Registro de nível de energia do usuário em um período do dia.
 * Usado para cruzar com produtividade e sugerir horários ideais por tipo de tarefa.
 */
@Entity
@Table(name = "tb_energy_logs", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "log_date", "period"})
})
public class EnergyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayPeriod period;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnergyLevel level;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDateTime createdAt;

    public EnergyLog() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDate getLogDate() { return logDate; }
    public void setLogDate(LocalDate logDate) { this.logDate = logDate; }

    public DayPeriod getPeriod() { return period; }
    public void setPeriod(DayPeriod period) { this.period = period; }

    public EnergyLevel getLevel() { return level; }
    public void setLevel(EnergyLevel level) { this.level = level; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
