package com.gustavocirino.myday_productivity.model.enums;

/**
 * Contexto de execução da tarefa, usado pelo algoritmo de Batching (K-Means)
 * para agrupar tarefas similares em blocos de foco e manter o usuário em "flow".
 */
public enum ContextType {

    /** Ligações telefônicas, videochamadas, WhatsApp de voz. */
    CALLS,

    /** Responder e-mails, Slack, mensagens de texto. */
    EMAILS,

    /** Reuniões presenciais ou online com mais de uma pessoa. */
    MEETINGS,

    /** Leitura de artigos, livros, relatórios, documentação. */
    READING,

    /** Produção de texto, código, apresentações, documentos. */
    WRITING,

    /** Tarefas burocráticas: formulários, aprovações, arquivamento. */
    ADMINISTRATIVE,

    /** Brainstorming, design, criação de conteúdo, resolução de problemas complexos. */
    CREATIVE,

    /** Exercício, deslocamento, tarefas que exigem presença física. */
    PHYSICAL,

    /** Cursos, tutoriais, estudos, treinamentos. */
    LEARNING
}
