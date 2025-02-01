package com.tracker.demo.sql.repository;

import com.tracker.demo.sql.entity.LoadCellSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoadCellSessionRepository extends JpaRepository<LoadCellSession, Long> {
    List<LoadCellSession> findByDate(LocalDate date);
}
