package com.gustavocirino.myday_productivity.repository;

import com.gustavocirino.myday_productivity.model.TaskContextGroup;
import com.gustavocirino.myday_productivity.model.User;
import com.gustavocirino.myday_productivity.model.enums.ContextType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskContextGroupRepository extends JpaRepository<TaskContextGroup, Long> {

    /** Todos os grupos de batching do usuário. */
    List<TaskContextGroup> findByUser(User user);

    /** Grupos do usuário filtrados por tipo de contexto — ex.: todos os blocos de CALLS. */
    List<TaskContextGroup> findByUserAndContextType(User user, ContextType contextType);

    /**
     * Grupo do usuário para um contexto específico que ainda não foi otimizado,
     * ou que foi otimizado antes de uma data de referência.
     * Usado para identificar clusters que precisam ser recalculados.
     */
    @Query("""
            SELECT g FROM TaskContextGroup g
            WHERE g.user = :user
              AND g.contextType = :contextType
              AND (g.lastOptimizedAt IS NULL OR g.lastOptimizedAt < :before)
            """)
    List<TaskContextGroup> findStaleGroupsByUserAndContextType(
            @Param("user") User user,
            @Param("contextType") ContextType contextType,
            @Param("before") LocalDateTime before);

    /**
     * Grupo ativo de um tipo de contexto para um usuário.
     * No modelo MVP, cada usuário tem no máximo um grupo ativo por contexto.
     */
    Optional<TaskContextGroup> findFirstByUserAndContextTypeOrderByCreatedAtDesc(
            User user, ContextType contextType);

    /**
     * Grupos que contêm uma tarefa específica.
     * Uma tarefa pode estar em mais de um grupo temporariamente durante reclusterização.
     */
    @Query("""
            SELECT g FROM TaskContextGroup g
            JOIN g.tasks t
            WHERE t.id = :taskId
            """)
    List<TaskContextGroup> findGroupsContainingTask(@Param("taskId") Long taskId);
}
