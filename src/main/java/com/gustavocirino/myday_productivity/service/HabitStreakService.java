package com.gustavocirino.myday_productivity.service;

import com.gustavocirino.myday_productivity.dto.BadgeDTO;
import com.gustavocirino.myday_productivity.dto.GamificationDTO;
import com.gustavocirino.myday_productivity.dto.HabitStreakDTO;
import com.gustavocirino.myday_productivity.model.Badge;
import com.gustavocirino.myday_productivity.model.HabitStreak;
import com.gustavocirino.myday_productivity.model.User;
import com.gustavocirino.myday_productivity.model.enums.TaskStatus;
import com.gustavocirino.myday_productivity.repository.BadgeRepository;
import com.gustavocirino.myday_productivity.repository.HabitStreakRepository;
import com.gustavocirino.myday_productivity.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HabitStreakService {

    private final HabitStreakRepository habitStreakRepository;
    private final BadgeRepository badgeRepository;
    private final TaskRepository taskRepository;

    private static final Map<String, BadgeDefinition> BADGE_DEFINITIONS = Map.of(
            "streak_3", new BadgeDefinition("streak_3", "Sequência de 3", "Completou tarefas por 3 dias seguidos", "🔥"),
            "streak_7", new BadgeDefinition("streak_7", "Semana Perfeita", "Completou tarefas por 7 dias seguidos", "⭐"),
            "streak_14", new BadgeDefinition("streak_14", "Duas Semanas", "14 dias consecutivos de produtividade", "💎"),
            "streak_30", new BadgeDefinition("streak_30", "Mês de Ouro", "30 dias consecutivos!", "🏆"),
            "tasks_10", new BadgeDefinition("tasks_10", "Primeira Dezena", "Completou 10 tarefas no total", "📋"),
            "tasks_50", new BadgeDefinition("tasks_50", "Meio Centenário", "Completou 50 tarefas no total", "🎯"),
            "tasks_100", new BadgeDefinition("tasks_100", "Centenário", "Completou 100 tarefas!", "🏅"),
            "focus_10", new BadgeDefinition("focus_10", "Focado", "10 sessões de foco completadas", "🧠"),
            "focus_50", new BadgeDefinition("focus_50", "Mestre do Foco", "50 sessões de foco completadas", "🎓")
    );

    /**
     * Records a daily check-in and updates streak. Called when user completes tasks.
     */
    @Transactional
    public HabitStreakDTO checkIn(User user, String habitType) {
        HabitStreak streak = habitStreakRepository.findByUserIdAndHabitType(user.getId(), habitType)
                .orElseGet(() -> {
                    HabitStreak newStreak = new HabitStreak();
                    newStreak.setUser(user);
                    newStreak.setHabitType(habitType);
                    newStreak.setCurrentStreak(0);
                    newStreak.setBestStreak(0);
                    newStreak.setTotalCompletions(0);
                    return newStreak;
                });

        LocalDate today = LocalDate.now();
        LocalDate lastCompleted = streak.getLastCompletedDate();

        if (lastCompleted != null && lastCompleted.equals(today)) {
            // Already checked in today
            return toDTO(streak);
        }

        // Update streak
        if (lastCompleted != null && lastCompleted.equals(today.minusDays(1))) {
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
        } else {
            streak.setCurrentStreak(1); // Reset streak
        }

        if (streak.getCurrentStreak() > streak.getBestStreak()) {
            streak.setBestStreak(streak.getCurrentStreak());
        }

        streak.setLastCompletedDate(today);
        streak.setTotalCompletions(streak.getTotalCompletions() + 1);
        streak.setUpdatedAt(LocalDateTime.now());

        HabitStreak saved = habitStreakRepository.save(streak);
        log.info("🔥 Streak atualizado: {} - dia {}", habitType, saved.getCurrentStreak());

        // Check for badge unlocks
        checkAndAwardBadges(user, saved);

        return toDTO(saved);
    }

    public GamificationDTO getGamificationData(User user) {
        List<HabitStreakDTO> streaks = habitStreakRepository
                .findByUserIdOrderByCurrentStreakDesc(user.getId())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        List<BadgeDTO> badges = badgeRepository.findByUserIdOrderByUnlockedAtDesc(user.getId())
                .stream()
                .map(this::toBadgeDTO)
                .collect(Collectors.toList());

        // Calculate total points: streaks + badges + completions
        int totalPoints = streaks.stream().mapToInt(s -> s.totalCompletions() * 10 + s.currentStreak() * 5).sum()
                + badges.size() * 50;

        int level = calculateLevel(totalPoints);

        return new GamificationDTO(streaks, badges, totalPoints, level);
    }

    public List<BadgeDTO> getBadges(User user) {
        return badgeRepository.findByUserIdOrderByUnlockedAtDesc(user.getId())
                .stream()
                .map(this::toBadgeDTO)
                .collect(Collectors.toList());
    }

    private void checkAndAwardBadges(User user, HabitStreak streak) {
        // Streak-based badges
        int[] streakMilestones = {3, 7, 14, 30};
        for (int milestone : streakMilestones) {
            if (streak.getCurrentStreak() >= milestone) {
                awardBadge(user, "streak_" + milestone);
            }
        }

        // Completion-based badges
        int[] completionMilestones = {10, 50, 100};
        for (int milestone : completionMilestones) {
            if (streak.getTotalCompletions() >= milestone) {
                awardBadge(user, "tasks_" + milestone);
            }
        }
    }

    private void awardBadge(User user, String badgeCode) {
        if (badgeRepository.existsByUserIdAndBadgeCode(user.getId(), badgeCode)) {
            return; // Already has this badge
        }

        BadgeDefinition def = BADGE_DEFINITIONS.get(badgeCode);
        if (def == null) return;

        Badge badge = new Badge();
        badge.setUser(user);
        badge.setBadgeCode(def.code());
        badge.setName(def.name());
        badge.setDescription(def.description());
        badge.setIcon(def.icon());
        badge.setUnlockedAt(LocalDateTime.now());

        badgeRepository.save(badge);
        log.info("🏆 Badge desbloqueado: {} para usuário {}", badgeCode, user.getId());
    }

    private int calculateLevel(int totalPoints) {
        if (totalPoints >= 5000) return 10;
        if (totalPoints >= 3000) return 8;
        if (totalPoints >= 2000) return 7;
        if (totalPoints >= 1500) return 6;
        if (totalPoints >= 1000) return 5;
        if (totalPoints >= 600) return 4;
        if (totalPoints >= 300) return 3;
        if (totalPoints >= 100) return 2;
        return 1;
    }

    private HabitStreakDTO toDTO(HabitStreak s) {
        return new HabitStreakDTO(
                s.getId(),
                s.getHabitType(),
                s.getCurrentStreak(),
                s.getBestStreak(),
                s.getLastCompletedDate() != null ? s.getLastCompletedDate().toString() : null,
                s.getTotalCompletions()
        );
    }

    private BadgeDTO toBadgeDTO(Badge b) {
        return new BadgeDTO(
                b.getId(),
                b.getBadgeCode(),
                b.getName(),
                b.getDescription(),
                b.getIcon(),
                b.getUnlockedAt()
        );
    }

    private record BadgeDefinition(String code, String name, String description, String icon) {}
}
