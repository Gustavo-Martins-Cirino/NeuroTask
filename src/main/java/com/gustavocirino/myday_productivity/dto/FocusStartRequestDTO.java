package com.gustavocirino.myday_productivity.dto;

public record FocusStartRequestDTO(
    Long taskId,
    Integer plannedMinutes,
    Integer breakMinutes,
    Integer totalCycles
) {}
