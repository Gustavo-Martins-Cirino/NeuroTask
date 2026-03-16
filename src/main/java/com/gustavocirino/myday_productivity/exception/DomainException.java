package com.gustavocirino.myday_productivity.exception;

/**
 * Exceção base para todas as exceções de domínio da aplicação.
 * Permite tratamento específico no GlobalExceptionHandler.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}