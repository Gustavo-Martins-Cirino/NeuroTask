package com.gustavocirino.myday_productivity.repository;

import com.gustavocirino.myday_productivity.model.ProcrastinationRecord;
import com.gustavocirino.myday_productivity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcrastinationRecordRepository extends JpaRepository<ProcrastinationRecord, Long> {

    void deleteByTaskId(Long taskId);

    /** Registro de adiamentos de uma tarefa específica (0 ou 1 por tarefa). */
    Optional<ProcrastinationRecord> findByTaskId(Long taskId);

    /**
     * Todos os registros do usuário — série histórica usada para calcular
     * média (μ) e desvio padrão (σ) do Z-score de procrastinação.
     */
    List<ProcrastinationRecord> findByUser(User user);

    /** Registros onde a anomalia foi confirmada — feed de alertas do dashboard. */
    List<ProcrastinationRecord> findByUserAndIsAnomalyTrue(User user);

    /** Registros anômalos sem sugestão gerada — fila de processamento da IA. */
    List<ProcrastinationRecord> findByUserAndIsAnomalyTrueAndSuggestionGeneratedFalse(User user);

    /**
     * Média de adiamentos do usuário.
     * Nulo quando o usuário não tem nenhum registro ainda.
     */
    @Query("SELECT AVG(p.postponeCount) FROM ProcrastinationRecord p WHERE p.user = :user")
    Double findAveragePostponeCountByUser(@Param("user") User user);

    /**
     * Lista de todos os postponeCount do usuário como doubles.
     * Usada para calcular o desvio padrão em memória sem trazer toda a entidade.
     */
    @Query("SELECT CAST(p.postponeCount AS double) FROM ProcrastinationRecord p WHERE p.user = :user")
    List<Double> findAllPostponeCountValuesByUser(@Param("user") User user);

    /** Total de anomalias detectadas — KPI do módulo de procrastinação. */
    @Query("SELECT COUNT(p) FROM ProcrastinationRecord p WHERE p.user = :user AND p.isAnomaly = true")
    long countAnomaliesByUser(@Param("user") User user);
}
