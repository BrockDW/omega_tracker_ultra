package com.tracker.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LeetCodeApiResponse {
    private String status;
    private String message;
    private int totalSolved;
    private int totalQuestions;
    private int easySolved;
    private int totalEasy;
    private int mediumSolved;
    private int totalMedium;
    private int hardSolved;
    private int totalHard;
    private double acceptanceRate;
    private int ranking;
    private int contributionPoints;
    private int reputation;
    private Object submissionCalendar;
}