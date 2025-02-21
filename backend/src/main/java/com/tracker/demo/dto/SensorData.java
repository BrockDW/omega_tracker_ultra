package com.tracker.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SensorData {
    private long rawValue;
    private double weight; // Optional: Add more fields as needed

    @Override
    public String toString() {
        return "SensorData{" +
                "rawValue=" + rawValue +
                ", weight=" + weight +
                '}';
    }
}