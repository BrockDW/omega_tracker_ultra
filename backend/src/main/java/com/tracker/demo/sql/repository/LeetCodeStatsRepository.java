package com.tracker.demo.sql.repository;

import com.tracker.demo.sql.entity.LeetCodeStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LeetCodeStatsRepository extends JpaRepository<LeetCodeStats, Long> {
    LeetCodeStats findByDate(LocalDate date);

    Optional<LeetCodeStats> findTopByDateBeforeOrderByDateDesc(LocalDate date);

    List<LeetCodeStats> findByDateBetween(LocalDate startDate, LocalDate endDate);
}