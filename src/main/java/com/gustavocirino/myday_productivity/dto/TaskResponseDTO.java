package com.gustavocirino.myday_productivity.dto;

import com.gustavocirino.myday_productivity.model.enums.TaskPriority;
import com.gustavocirino.myday_productivity.model.enums.TaskStatus;
import java.time.LocalDateTime;

/**
 * DTO para retornar tarefas ao frontend (GET /api/tasks)
 * Expõe apenas o necessário, escondendo detalhes internos da entidade
 */
public record TaskResponseDTO(
                Long id,
                String title,
                String description,
                TaskPriority priority,
                TaskStatus status,
                LocalDateTime startTime,
                LocalDateTime endTime,
                LocalDateTime createdAt,
                String color) {
}
