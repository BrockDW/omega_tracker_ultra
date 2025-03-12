package com.tracker.demo.service;

import com.tracker.demo.dto.LoadCellExerciseResult;
import com.tracker.demo.sql.entity.LoadCellSession;
import com.tracker.demo.sql.repository.LoadCellSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class LoadCellService {
    @Autowired
    private LoadCellSessionRepository loadCellSessionRepository;

    @Autowired
    private InMemoryExerciseTrackerService inMemoryTracker; // <<--- new

    private LocalDateTime exerciseStartTime;
    private boolean isExerciseActive = false;

    // Threshold to detect exercise in kg (negative means heavier).
    private static final float EXERCISE_THRESHOLD_RELATIVE = 8.0f;
    private static final long DEFAULT_GOAL_SECONDS = 300; // 5-minute daily goal
    private static final int START_REQUIRED_CONSECUTIVE = 1;
    private static final int STOP_REQUIRED_CONSECUTIVE  = 1;

    // Rolling baseline
    private static final int BASELINE_WINDOW_SIZE = 100;
    private Deque<Double> baselineReadings = new ArrayDeque<>();
    private double baselineWeight = 0.0;

    private int consecutiveAboveThresholdCount = 0;
    private int consecutiveBelowThresholdCount = 0;

    /**
     * Called for each new reading from the load cell.
     * If we detect the start/stop of an "exercise," we store that interval in DB
     * (for daily totals) AND (optionally) in the inMemoryTracker if tracking is enabled.
     */
    public void processWeightData(double weight) {

        // 1) If NOT currently exercising, feed into baseline window
        if (!isExerciseActive) {
            baselineReadings.addLast(weight);
            if (baselineReadings.size() > BASELINE_WINDOW_SIZE) {
                baselineReadings.removeFirst();
            }
            baselineWeight = baselineReadings.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
        }

        // 2) Compare to adjusted threshold
        double adjustedThreshold = baselineWeight - EXERCISE_THRESHOLD_RELATIVE;
        if (weight <= adjustedThreshold) {
            consecutiveAboveThresholdCount++;
            consecutiveBelowThresholdCount = 0;
        } else {
            consecutiveBelowThresholdCount++;
            consecutiveAboveThresholdCount = 0;
        }

        // 3) Check if exercise should start
        if (!isExerciseActive && consecutiveAboveThresholdCount >= START_REQUIRED_CONSECUTIVE) {
            exerciseStartTime = LocalDateTime.now();
            isExerciseActive = true;
            System.out.println("Exercise started at: " + exerciseStartTime);
        }

        // 4) Check if exercise should stop
        if (isExerciseActive && consecutiveBelowThresholdCount >= STOP_REQUIRED_CONSECUTIVE) {
            LocalDateTime exerciseEndTime = LocalDateTime.now();
            long newSessionSeconds =
                    ChronoUnit.SECONDS.between(exerciseStartTime, exerciseEndTime);

            isExerciseActive = false;

            // ========== NEW: Add to inMemoryTracker if tracking is enabled =============
            inMemoryTracker.addSession(exerciseStartTime, exerciseEndTime, newSessionSeconds);

            // ========== OPTIONAL: DB logic for daily total (unchanged) =================
            LocalDate today = LocalDate.now();
            LoadCellSession session = loadCellSessionRepository.findByDate(today);
            if (session == null) {
                session = new LoadCellSession();
                session.setDate(today);
                session.setGoal(DEFAULT_GOAL_SECONDS);
            }

            long updatedDuration = session.getDurationSeconds() + newSessionSeconds;
            session.setDurationSeconds(updatedDuration);
            if (session.getGoal() > 0) {
                float percentage = (float) updatedDuration / session.getGoal() * 100f;
                session.setPercentage(percentage);
            } else {
                session.setPercentage(0f);
            }
            loadCellSessionRepository.save(session);

            System.out.println("Exercise ended. This session: " + newSessionSeconds
                    + " seconds; total so far today: " + updatedDuration + " seconds");
        }

        // Debug
        System.out.println("baselineWeight: " + baselineWeight);
    }

    /**
     * Reset baseline if needed
     */
    public void resetBaseline() {
        baselineReadings.clear();
        baselineWeight = 0.0;
    }

    public LoadCellExerciseResult getTotalExerciseTimeToday() {
        LocalDate today = LocalDate.now();
        LoadCellSession sessions = loadCellSessionRepository.findByDate(today);
        if (sessions == null) {
            return new LoadCellExerciseResult(0f, DEFAULT_GOAL_SECONDS, 0L);
        }
        return new LoadCellExerciseResult(
                sessions.getPercentage(),
                sessions.getGoal(),
                sessions.getDurationSeconds());
    }

    public LoadCellExerciseResult getTotalExerciseTimeForDay(String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        LoadCellSession sessions = loadCellSessionRepository.findByDate(date);
        if (sessions == null) {
            return new LoadCellExerciseResult(0f, DEFAULT_GOAL_SECONDS, 0L);
        }
        return new LoadCellExerciseResult(
                sessions.getPercentage(),
                sessions.getGoal(),
                sessions.getDurationSeconds());
    }

    public Map<LocalDate, LoadCellExerciseResult> getTotalExerciseTimeInRange(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, LoadCellExerciseResult> resultMap = new HashMap<>();
        List<LoadCellSession> sessions = loadCellSessionRepository.findByDateBetween(startDate, endDate);

        for (LoadCellSession session : sessions) {
            resultMap.put(session.getDate(),
                    new LoadCellExerciseResult(session.getPercentage(),
                            session.getGoal(),
                            session.getDurationSeconds()));
        }
        return resultMap;
    }
}
