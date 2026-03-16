package com.gustavocirino.myday_productivity.repository;

import com.gustavocirino.myday_productivity.model.User;
import com.gustavocirino.myday_productivity.model.UserBattery;
import com.gustavocirino.myday_productivity.model.enums.BurnoutRiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserBatteryRepository extends JpaRepository<UserBattery, Long> {

    /** Histórico completo de snapshots do usuário, do mais recente ao mais antigo. */
    List<UserBattery> findByUserOrderByRecordedAtDesc(User user);

    /** Último snapshot registrado — estado atual da bateria do usuário. */
    Optional<UserBattery> findFirstByUserOrderByRecordedAtDesc(User user);

    /** Snapshots dentro de uma janela de tempo, para análise de tendência. */
    List<UserBattery> findByUserAndRecordedAtBetweenOrderByRecordedAtAsc(
            User user, LocalDateTime from, LocalDateTime to);

    /** Snapshots com risco igual ou acima de um nível — útil para alertas agregados. */
    List<UserBattery> findByUserAndRiskLevel(User user, BurnoutRiskLevel riskLevel);

    /** Média do nível da bateria nos últimos N snapshots — tendência de curto prazo. */
    @Query("""
            SELECT AVG(ub.batteryLevel)
            FROM UserBattery ub
            WHERE ub.user = :user
              AND ub.recordedAt >= :since
            """)
    Double findAverageBatteryLevelSince(@Param("user") User user,
                                        @Param("since") LocalDateTime since);

    /** Média de carga cognitiva do usuário — base para calcular anomalias. */
    @Query("SELECT AVG(ub.cognitiveLoad) FROM UserBattery ub WHERE ub.user = :user")
    Double findAverageCognitiveLoadByUser(@Param("user") User user);
}
