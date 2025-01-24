package com.tracker.demo.sql.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
@Entity
public class KeyBrPracticeRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate practiceDate;
    private double minutesPracticed;

    @Column(nullable = false, columnDefinition = "FLOAT8 DEFAULT 0.0")
    private double percentage = 0.0;

    @Column(nullable = false, columnDefinition = "FLOAT8 DEFAULT 0.0")
    private double totalMinutes = 30.0;

    public KeyBrPracticeRecord() {

    }

    public KeyBrPracticeRecord(Long id, LocalDate date, double minutesPracticed, double percentage, double totalMinutes) {
        this.id = id;
        this.practiceDate = date;
        this.minutesPracticed = minutesPracticed;
        this.percentage = percentage;
        this.totalMinutes = totalMinutes;
    }

}
