package com.gustavocirino.myday_productivity.dto;

import java.time.LocalDateTime;

public record SmartRescheduleSuggestionDTO(
    Long taskId,
    String taskTitle,
    LocalDateTime currentStart,
    LocalDateTime currentEnd,
    LocalDateTime suggestedStart,
    LocalDateTime suggestedEnd,
    String reason
) {}
