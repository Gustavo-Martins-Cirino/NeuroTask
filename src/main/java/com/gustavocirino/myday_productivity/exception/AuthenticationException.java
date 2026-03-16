package com.gustavocirino.myday_productivity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exceção lançada quando há falha na autenticação.
 * Retorna HTTP 401 Unauthorized.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class AuthenticationException extends DomainException {

    public AuthenticationException(String message) {
        super(message);
    }

    public static AuthenticationException invalidToken() {
        return new AuthenticationException("Token de autenticação inválido ou expirado");
    }

    public static AuthenticationException missingToken() {
        return new AuthenticationException("Token de autenticação não fornecido");
    }

    public static AuthenticationException invalidCredentials() {
        return new AuthenticationException("Credenciais inválidas");
    }
}