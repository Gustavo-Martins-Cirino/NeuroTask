package com.gustavocirino.myday_productivity.repository;

import com.gustavocirino.myday_productivity.model.FocusSession;
import com.gustavocirino.myday_productivity.model.enums.FocusSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FocusSessionRepository extends JpaRepository<FocusSession, Long> {

    List<FocusSession> findByUserIdAndStatusOrderByStartedAtDesc(Long userId, FocusSessionStatus status);

    Optional<FocusSession> findFirstByUserIdAndStatusOrderByStartedAtDesc(Long userId, FocusSessionStatus status);

    List<FocusSession> findByUserIdAndStartedAtBetweenOrderByStartedAtDesc(
            Long userId, LocalDateTime start, LocalDateTime end);

    List<FocusSession> findByUserIdAndStatusAndStartedAtBetween(
            Long userId, FocusSessionStatus status, LocalDateTime start, LocalDateTime end);

    List<FocusSession> findByUserIdOrderByStartedAtDesc(Long userId);
}
