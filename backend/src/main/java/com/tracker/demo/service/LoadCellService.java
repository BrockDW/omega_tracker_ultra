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

    private LocalDateTime exerciseStartTime;
    private boolean isExerciseActive = false;

    // Threshold to detect exercise (in kg) on top of the baseline
    private static final float EXERCISE_THRESHOLD_RELATIVE = 0.5f;

    // Default exercise goal in seconds (e.g., 5 minutes)
    private static final long DEFAULT_GOAL_SECONDS = 300;

    // Number of consecutive readings needed to confirm start/stop
    private static final int START_REQUIRED_CONSECUTIVE = 1;
    private static final int STOP_REQUIRED_CONSECUTIVE  = 1;

    private int consecutiveAboveThresholdCount = 0;
    private int consecutiveBelowThresholdCount = 0;

    // --- Rolling baseline fields ---
    // We'll keep a rolling buffer of some number of the most recent "low" readings.
    // 100 is arbitrary; adjust as needed.
    private static final int BASELINE_WINDOW_SIZE = 100;
    private Deque<Double> baselineReadings = new ArrayDeque<>();
    private double baselineWeight = 0.0;

    /**
     * Process each new weight reading.
     * - Recalibrate baseline when not in the middle of an exercise.
     * - Detect start/stop of exercise based on consecutive readings above/below threshold.
     */
    public void processWeightData(double weight) {

        // 1) Recalibrate the baseline if NOT currently exercising.
        //    (Idea: if you assume the scale is "empty" only when no one is on it,
        //     ignoring readings that are obviously above threshold can help prevent
        //     including "load" in your baseline.)
        if (!isExerciseActive) {
            // Add the reading into a rolling window
            baselineReadings.addLast(weight);
            if (baselineReadings.size() > BASELINE_WINDOW_SIZE) {
                baselineReadings.removeFirst();
            }

            // Recompute the baseline as the average of all readings in the window
            // (You could alternatively filter out any large readings that are definitely not "empty")
            baselineWeight = baselineReadings.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
        }

        // 2) Determine if we're above or below the threshold
        double adjustedThreshold = baselineWeight + EXERCISE_THRESHOLD_RELATIVE;
        if (weight >= adjustedThreshold) {
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

            // Fetch or create today's session record
            LocalDate today = LocalDate.now();
            LoadCellSession session = loadCellSessionRepository.findByDate(today);
            if (session == null) {
                session = new LoadCellSession();
                session.setDate(today);
                session.setGoal(DEFAULT_GOAL_SECONDS);
            }

            // Update total exercise duration for the day
            long updatedDuration = session.getDurationSeconds() + newSessionSeconds;
            session.setDurationSeconds(updatedDuration);

            // Update the percentage if there's a nonzero goal
            if (session.getGoal() > 0) {
                float percentage = (float) updatedDuration / session.getGoal() * 100f;
                session.setPercentage(percentage);
            } else {
                session.setPercentage(0f);
            }

            // Persist
            loadCellSessionRepository.save(session);

            System.out.println("Exercise ended. This session: " + newSessionSeconds
                    + " seconds; total so far today: " + updatedDuration + " seconds");
        }
        System.out.println("baselineWeight: " + baselineWeight);
    }

    /**
     * Calculates the baseline weight as the average of the collected baseline readings.
     */
    private double calculateBaseline() {
        double sum = baselineReadings.stream().mapToDouble(Double::doubleValue).sum();
        return sum / baselineReadings.size();
    }

    /**
     * Resets the baseline calculation (e.g., when the load cell is recalibrated or moved).
     */
    public void resetBaseline() {
        baselineReadings.clear();
        baselineWeight = 0;
    }

    public LoadCellExerciseResult getTotalExerciseTimeToday() {
        LocalDate today = LocalDate.now();
        LoadCellSession sessions = loadCellSessionRepository.findByDate(today);
        return new LoadCellExerciseResult(sessions.getPercentage(), sessions.getGoal(), sessions.getDurationSeconds());
    }

    public LoadCellExerciseResult getTotalExerciseTimeForDay(String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        LoadCellSession sessions = loadCellSessionRepository.findByDate(date);
        return new LoadCellExerciseResult(sessions.getPercentage(), sessions.getGoal(), sessions.getDurationSeconds());
    }

    public Map<LocalDate, LoadCellExerciseResult> getTotalExerciseTimeInRange(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, LoadCellExerciseResult> resultMap = new HashMap<>();
        List<LoadCellSession> sessions = loadCellSessionRepository.findByDateBetween(startDate, endDate);

        for (LoadCellSession session: sessions) {
            resultMap.put(session.getDate(), new LoadCellExerciseResult(session.getPercentage(), session.getGoal(), session.getDurationSeconds()));
        }

        return resultMap;
    }
}
