package com.gustavocirino.myday_productivity.repository;

import com.gustavocirino.myday_productivity.model.EnergyLog;
import com.gustavocirino.myday_productivity.model.enums.DayPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnergyLogRepository extends JpaRepository<EnergyLog, Long> {

    Optional<EnergyLog> findByUserIdAndLogDateAndPeriod(Long userId, LocalDate logDate, DayPeriod period);

    List<EnergyLog> findByUserIdAndLogDateOrderByPeriodAsc(Long userId, LocalDate logDate);

    List<EnergyLog> findByUserIdAndLogDateBetweenOrderByLogDateDescPeriodAsc(
            Long userId, LocalDate start, LocalDate end);

    List<EnergyLog> findByUserIdOrderByLogDateDescPeriodAsc(Long userId);
}
