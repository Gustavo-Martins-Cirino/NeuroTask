package com.gustavocirino.myday_productivity.dto;

import java.util.List;

public record GamificationDTO(
    List<HabitStreakDTO> streaks,
    List<BadgeDTO> badges,
    int totalPoints,
    int level
) {}
