package com.tracker.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Task {
    private String description;
    private boolean completed;
    private boolean excluded;

    // NEW FIELD
    private int frequency;

    public Task(String description, boolean completed) {
        this.description = description;
        this.completed = completed;
        this.excluded = false;
        this.frequency = 1; // default to 1
    }
}
