package com.tracker.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LeetCodeResult {
    // Getters (and optionally setters) for JSON serialization
    private double percentage;
    private double goalQuestion;
    private double questionFinished;

    public LeetCodeResult(double percentage, double goalQuestion, double questionFinished) {
        this.percentage = percentage;
        this.goalQuestion = goalQuestion;
        this.questionFinished = questionFinished;
    }
}
