package com.gustavocirino.myday_productivity.dto;

import com.gustavocirino.myday_productivity.model.enums.FocusSessionStatus;
import java.time.LocalDateTime;

public record FocusSessionDTO(
    Long id,
    Long taskId,
    String taskTitle,
    FocusSessionStatus status,
    Integer plannedMinutes,
    Long actualSeconds,
    Integer breakMinutes,
    Integer cycleNumber,
    Integer totalCycles,
    LocalDateTime startedAt,
    LocalDateTime pausedAt,
    LocalDateTime completedAt
) {}
