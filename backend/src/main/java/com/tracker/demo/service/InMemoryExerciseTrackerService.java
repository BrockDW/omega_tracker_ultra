package com.tracker.demo.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Tracks "exercise sessions" in memory, but only when "trackingEnabled" is true.
 */
@Service
public class InMemoryExerciseTrackerService {

    /**
     * At 6:15 AM PST, enable in-memory tracking and clear old sessions.
     * This starts the hour window (6:15 - 7:15).
     */
    @Scheduled(cron = "0 15 6 * * ?", zone = "America/Los_Angeles")
    public void enableTrackingAt6_15AM() {
        startTrackingWindow();
    }

    /**
     * At 7:15 AM PST, check if we had >=30s of load-cell exercise in the last hour,
     * then disable/clear the in-memory sessions to free up memory.
     */
    @Scheduled(cron = "0 15 7 * * ?", zone = "America/Los_Angeles")
    public void checkAt7_15AM() {
        boolean hadActivity = hasThirtySecondsInLastHour();

        if (hadActivity) {
            System.out.println("[7:15 AM Check] Found >= 30s of activity between 6:15–7:15!");
            // TODO: do something if we found enough activity
        } else {
            System.out.println("[7:15 AM Check] Did NOT find >= 30s in the last hour.");
            // TODO: do something else (e.g., notify user)
        }

        // Now free up memory and disable tracking
        stopTrackingWindow();
    }

    /**
     * A simple record of one exercise session:
     *   - startTime
     *   - endTime
     *   - total duration in seconds
     */
    public static class ExerciseRecord {
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final long durationSeconds;

        public ExerciseRecord(LocalDateTime startTime, LocalDateTime endTime, long durationSeconds) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.durationSeconds = durationSeconds;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }
        public LocalDateTime getEndTime() {
            return endTime;
        }
        public long getDurationSeconds() {
            return durationSeconds;
        }
    }

    /**
     * Flag to indicate if we should capture sessions in-memory right now.
     */
    private boolean trackingEnabled = false;

    /**
     * Holds all in-memory sessions (only those that occur while tracking is enabled).
     */
    private final List<ExerciseRecord> sessions = new ArrayList<>();

    public boolean didWakeUpToday;

    /**
     * Called when an exercise stops (from LoadCellService).
     * We only add if trackingEnabled == true.
     */
    public synchronized void addSession(LocalDateTime start, LocalDateTime end, long duration) {
        if (trackingEnabled) {
            sessions.add(new ExerciseRecord(start, end, duration));
        }
    }

    /**
     * Start the one-hour tracking window (e.g. at 6:15 AM).
     */
    public synchronized void startTrackingWindow() {
        // Clear anything old, then enable tracking
        sessions.clear();
        trackingEnabled = true;
        System.out.println("[InMemoryExerciseTrackerService] Tracking window started. (List cleared, tracking enabled)");
    }

    /**
     * Stop tracking (e.g. at 7:15 AM) and clear the data to free memory.
     */
    public synchronized void stopTrackingWindow() {
        sessions.clear();
        trackingEnabled = false;
        System.out.println("[InMemoryExerciseTrackerService] Tracking window stopped. (List cleared, tracking disabled)");
    }

    /**
     * Returns whether we are currently capturing sessions.
     */
    public synchronized boolean isTrackingEnabled() {
        return trackingEnabled;
    }

    /**
     * Check if there's at least 30s of "exercise" in the last hour of sessions stored.
     *
     * Because we only track from 6:15–7:15, you could do a direct sum. But to be robust,
     * this method calculates overlap with "LocalDateTime.now() minus 1 hour."
     */
    public synchronized boolean hasThirtySecondsInLastHour() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);

        long totalSeconds = 0;

        for (ExerciseRecord record : sessions) {
            // If the session ends after oneHourAgo AND starts before now,
            // there's some overlap with the last hour.
            if (record.getEndTime().isAfter(oneHourAgo) && record.getStartTime().isBefore(now)) {
                // Overlap start = the later of (session.start, oneHourAgo)
                LocalDateTime overlapStart = record.getStartTime().isBefore(oneHourAgo)
                        ? oneHourAgo : record.getStartTime();
                // Overlap end = the earlier of (session.end, now)
                LocalDateTime overlapEnd = record.getEndTime().isAfter(now)
                        ? now : record.getEndTime();

                long overlapSecs = ChronoUnit.SECONDS.between(overlapStart, overlapEnd);
                if (overlapSecs > 0) {
                    totalSeconds += overlapSecs;
                    if (totalSeconds >= 30) {
                        didWakeUpToday = true;
                        return true;
                    }
                }
            }
        }
        didWakeUpToday = false;
        return false;
    }
}
