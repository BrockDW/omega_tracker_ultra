package com.tracker.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class KeyBrPracticeResult {
    // Getters (and optionally setters) for JSON serialization
    private double percentage;
    private double totalMinutes;
    private double minutesPracticed;

    public KeyBrPracticeResult(double percentage, double totalMinutes, double minutesPracticed) {
        this.percentage = percentage;
        this.totalMinutes = totalMinutes;
        this.minutesPracticed = minutesPracticed;
    }
}
