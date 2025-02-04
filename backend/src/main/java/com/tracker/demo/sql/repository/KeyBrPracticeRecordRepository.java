package com.tracker.demo.sql.repository;

import com.tracker.demo.sql.entity.KeyBrPracticeRecord;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// KeyBrPracticeRecordRepository.java
@Repository
public interface KeyBrPracticeRecordRepository extends JpaRepository<KeyBrPracticeRecord, Long> {
    @Query(value = """
        INSERT INTO key_br_practice_record (practice_date, minutes_practiced, percentage, total_minutes)
        VALUES (:practiceDate, :minutesPracticed, :percentage, :totalMinutes)
        ON CONFLICT (practice_date)
        DO UPDATE 
            SET minutes_practiced = EXCLUDED.minutes_practiced,
                percentage = EXCLUDED.percentage,
                total_minutes = EXCLUDED.total_minutes
        """,
            nativeQuery = true)
    @Modifying
    @Transactional
    void upsertPracticeRecord(@Param("practiceDate") LocalDate practiceDate,
                              @Param("minutesPracticed") double minutesPracticed,
                              @Param("percentage") double percentage,
                              @Param("totalMinutes") double totalMinutes);

    KeyBrPracticeRecord findByPracticeDate(LocalDate practiceDate);

    // [NEW] For the range
    // Spring Data JPA can auto-implement "findAllByPracticeDateBetween"
    List<KeyBrPracticeRecord> findAllByPracticeDateBetween(LocalDate start, LocalDate end);
}

