package com.gustavocirino.myday_productivity.repository;

import com.gustavocirino.myday_productivity.model.WeeklyReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyReviewRepository extends JpaRepository<WeeklyReview, Long> {

    Optional<WeeklyReview> findByUserIdAndWeekStart(Long userId, LocalDate weekStart);

    List<WeeklyReview> findByUserIdOrderByWeekStartDesc(Long userId);

    List<WeeklyReview> findTop4ByUserIdOrderByWeekStartDesc(Long userId);
}
