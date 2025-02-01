package com.tracker.demo.dto;

public class SensorData {
    private long rawValue;
    private double weight;  // Optional: Add more fields as needed

    // Getters and setters
    public long getRawValue() {
        return rawValue;
    }

    public void setRawValue(long rawValue) {
        this.rawValue = rawValue;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "SensorData{" +
                "rawValue=" + rawValue +
                ", weight=" + weight +
                '}';
    }
}