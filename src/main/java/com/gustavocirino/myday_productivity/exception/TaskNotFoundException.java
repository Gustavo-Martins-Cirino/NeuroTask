package com.gustavocirino.myday_productivity.exception;

/**
 * Exceção lançada quando uma tarefa não é encontrada.
 */
public class TaskNotFoundException extends DomainException {

    public TaskNotFoundException(Long id) {
        super("Tarefa não encontrada com ID: " + id);
    }

    public TaskNotFoundException(String message) {
        super(message);
    }
}