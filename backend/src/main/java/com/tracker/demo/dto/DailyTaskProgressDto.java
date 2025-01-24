package com.tracker.demo.dto;

import com.tracker.demo.sql.entity.KeyBrPracticeRecord;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Setter
@Getter
public class DailyTaskProgressDto {
    // Getters & setters
    private LocalDate date;
    private List<Task> tasks;      // or a TaskDto if you prefer
    private KeyBrPracticeResult keyBrPracticeResult;

    // You can keep adding more daily info fields here as needed
    // e.g. private double moodScore; private double hoursWorked; etc.

    public DailyTaskProgressDto() {}

    public DailyTaskProgressDto(LocalDate date, List<Task> tasks, KeyBrPracticeResult keyBrPracticeResult) {
        this.date = date;
        this.tasks = tasks;
        this.keyBrPracticeResult = keyBrPracticeResult;
    }
}
