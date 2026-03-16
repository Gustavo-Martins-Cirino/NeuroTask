package com.gustavocirino.myday_productivity.dto;

public record HabitStreakDTO(
    Long id,
    String habitType,
    int currentStreak,
    int bestStreak,
    String lastCompletedDate,
    int totalCompletions
) {}
