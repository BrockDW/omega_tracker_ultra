package com.tracker.demo.dto;

public class Task {
    private String description;
    private boolean completed;
    private boolean excluded;  // <-- New field

    public Task(String description, boolean completed) {
        this.description = description;
        this.completed = completed;
        this.excluded = false; // default to false
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public boolean isExcluded() { return excluded; }
    public void setExcluded(boolean excluded) { this.excluded = excluded; }
}