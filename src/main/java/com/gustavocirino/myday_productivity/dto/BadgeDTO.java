package com.gustavocirino.myday_productivity.dto;

import java.time.LocalDateTime;

public record BadgeDTO(
    Long id,
    String badgeCode,
    String name,
    String description,
    String icon,
    LocalDateTime unlockedAt
) {}
