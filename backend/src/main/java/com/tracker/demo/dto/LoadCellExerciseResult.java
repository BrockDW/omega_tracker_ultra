package com.tracker.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoadCellExerciseResult {
    // Getters (and optionally setters) for JSON serialization
    private double percentage;
    private double goalSeconds;
    private double secondsPracticed;

    public LoadCellExerciseResult(double percentage, double goalSeconds, double secondsPracticed) {
        this.percentage = percentage;
        this.goalSeconds = goalSeconds;
        this.secondsPracticed = secondsPracticed;
    }
}
