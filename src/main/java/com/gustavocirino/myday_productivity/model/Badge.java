package com.gustavocirino.myday_productivity.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Badge / conquista desbloqueada pelo usuário.
 * Gamificação baseada em streaks e marcos de produtividade.
 */
@Entity
@Table(name = "tb_badges", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "badge_code"})
})
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Código único do badge (ex: "streak_7", "focus_master", "early_bird") */
    @Column(name = "badge_code", nullable = false, length = 50)
    private String badgeCode;

    /** Nome exibível (ex: "Madrugador", "Foco Total") */
    @Column(nullable = false, length = 100)
    private String name;

    /** Descrição do badge */
    @Column(length = 255)
    private String description;

    /** Emoji/ícone do badge */
    @Column(length = 10)
    private String icon;

    private LocalDateTime unlockedAt;

    public Badge() {
        this.unlockedAt = LocalDateTime.now();
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getBadgeCode() { return badgeCode; }
    public void setBadgeCode(String badgeCode) { this.badgeCode = badgeCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public LocalDateTime getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(LocalDateTime unlockedAt) { this.unlockedAt = unlockedAt; }
}
