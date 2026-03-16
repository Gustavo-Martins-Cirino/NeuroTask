package com.gustavocirino.myday_productivity.repository;

import com.gustavocirino.myday_productivity.model.HabitStreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HabitStreakRepository extends JpaRepository<HabitStreak, Long> {

    Optional<HabitStreak> findByUserIdAndHabitType(Long userId, String habitType);

    List<HabitStreak> findByUserIdOrderByCurrentStreakDesc(Long userId);
}
