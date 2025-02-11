package com.tracker.demo.sql.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
@Entity
public class LoadCellSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;
    private long durationSeconds;   // total accumulated duration in seconds for that day

    private long goal;             // daily target in seconds
    private float percentage;      // (durationSeconds / goal) * 100

    // getters and setters ...
}
