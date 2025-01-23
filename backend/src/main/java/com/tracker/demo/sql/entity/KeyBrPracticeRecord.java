package com.tracker.demo.sql.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    public KeyBrPracticeRecord() {

    }

    public KeyBrPracticeRecord(Long id, LocalDate date, double minutesPracticed) {
        this.id = id;
        this.practiceDate = date;
        this.minutesPracticed = minutesPracticed;
    }

}
