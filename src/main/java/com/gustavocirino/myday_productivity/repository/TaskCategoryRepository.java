package com.gustavocirino.myday_productivity.repository;

import com.gustavocirino.myday_productivity.model.TaskCategory;
import com.gustavocirino.myday_productivity.model.enums.TaskCategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskCategoryRepository extends JpaRepository<TaskCategory, Long> {

    /** Classificação ativa de uma tarefa (relacionamento 1:1). */
    Optional<TaskCategory> findByTaskId(Long taskId);

    /** Todas as classificações de um tipo — ex.: quantas tarefas URGENT existem. */
    List<TaskCategory> findByCategoryType(TaskCategoryType categoryType);

    /** Classificações que a IA fez com confiança abaixo do limiar — candidatas a revisão. */
    List<TaskCategory> findByConfidenceScoreLessThan(Double threshold);

    /** Classificações que o usuário já substituiu manualmente. */
    List<TaskCategory> findByManualOverrideTrue();

    /**
     * Distribuição de categorias para um usuário específico.
     * Retorna pares [CategoryType, count] para montar gráficos de triagem.
     */
    @Query("""
            SELECT tc.categoryType, COUNT(tc)
            FROM TaskCategory tc
            JOIN tc.task t
            WHERE t.user.id = :userId
            GROUP BY tc.categoryType
            """)
    List<Object[]> countCategoriesByUserId(@Param("userId") Long userId);

    /** Remove a classificação ao excluir a tarefa pai (cascade manual). */
    void deleteByTaskId(Long taskId);
}
