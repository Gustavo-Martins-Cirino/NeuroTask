package com.gustavocirino.myday_productivity.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record WeeklyReviewDTO(
    Long id,
    LocalDate weekStart,
    LocalDate weekEnd,
    int totalPlanned,
    int totalCompleted,
    int totalFocusMinutes,
    int productivityScore,
    String aiSummary,
    List<String> aiInsights,
    List<String> aiRecommendations,
    String peakProductivityTime,
    LocalDateTime createdAt
) {}
