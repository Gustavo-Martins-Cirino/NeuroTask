package com.gustavocirino.myday_productivity.dto;

/**
 * DTO para resposta de autenticação bem-sucedida.
 */
public record AuthResponseDTO(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        Long userId,
        String email
) {
    public static final String BEARER = "Bearer";

    public static AuthResponseDTO of(String accessToken, String refreshToken, long expiresIn, Long userId, String email) {
        return new AuthResponseDTO(accessToken, refreshToken, BEARER, expiresIn, userId, email);
    }
}