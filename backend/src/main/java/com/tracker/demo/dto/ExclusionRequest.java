package com.tracker.demo.dto;

public class ExclusionRequest {
    private String description;
    private boolean excluded;

    // Constructors
    public ExclusionRequest() { }

    public ExclusionRequest(String description, boolean excluded) {
        this.description = description;
        this.excluded = excluded;
    }

    // Getters/setters
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isExcluded() {
        return excluded;
    }

    public void setExcluded(boolean excluded) {
        this.excluded = excluded;
    }
}