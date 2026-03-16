package com.gustavocirino.myday_productivity.model.enums;

/**
 * Tipo de categoria gerado pela IA durante a Triagem de Tarefas.
 * Inspirado na Matriz de Eisenhower com camada adicional de delegabilidade.
 */
public enum TaskCategoryType {

    /**
     * Urgente + Importante: exige ação imediata. Geralmente crises ou prazos iminentes.
     */
    URGENT,

    /**
     * Não urgente + Importante: tarefas de alto valor que constroem o futuro (planejamento, estudos).
     */
    IMPORTANT,

    /**
     * Urgente + Não importante: distratores urgentes que podem ser delegados ou minimizados.
     */
    ROUTINE,

    /**
     * Não urgente + Não importante: candidatos a eliminação ou delegação total.
     */
    DELEGABLE
}
