package com.gustavocirino.myday_productivity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para requisição de refresh token.
 */
public record RefreshTokenRequestDTO(
        @NotBlank(message = "Refresh token é obrigatório")
        String refreshToken
) {}