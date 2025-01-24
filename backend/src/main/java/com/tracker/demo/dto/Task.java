package com.tracker.demo.dto;

public class Task {
    private String description;
    private boolean completed;

    // Constructors
    public Task(String description, boolean completed) {
        this.description = description;
        this.completed = completed;
    }

    // Getters and setters
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}
