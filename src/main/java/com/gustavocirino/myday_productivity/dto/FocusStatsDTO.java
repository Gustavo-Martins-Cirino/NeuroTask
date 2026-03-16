package com.gustavocirino.myday_productivity.dto;

import java.util.List;

public record FocusStatsDTO(
    long totalSessions,
    long completedSessions,
    long totalFocusMinutes,
    long avgSessionMinutes,
    long todaySessions,
    long todayFocusMinutes,
    List<FocusSessionDTO> recentSessions
) {}
