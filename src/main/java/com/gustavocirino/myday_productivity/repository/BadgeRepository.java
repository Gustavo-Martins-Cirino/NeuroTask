package com.gustavocirino.myday_productivity.repository;

import com.gustavocirino.myday_productivity.model.Badge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BadgeRepository extends JpaRepository<Badge, Long> {

    List<Badge> findByUserIdOrderByUnlockedAtDesc(Long userId);

    Optional<Badge> findByUserIdAndBadgeCode(Long userId, String badgeCode);

    boolean existsByUserIdAndBadgeCode(Long userId, String badgeCode);
}
