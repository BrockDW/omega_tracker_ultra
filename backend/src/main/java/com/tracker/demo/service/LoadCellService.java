package com.tracker.demo.service;

import com.tracker.demo.dto.LoadCellExerciseResult;
import com.tracker.demo.sql.entity.LoadCellSession;
import com.tracker.demo.sql.repository.LoadCellSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LoadCellService {

    @Autowired
    private LoadCellSessionRepository loadCellSessionRepository;

    private LocalDateTime exerciseStartTime;
    private boolean isExerciseActive = false;

    // Threshold to detect exercise (in kg)
    private static final float EXERCISE_THRESHOLD = 1.5f;

    private static final long DEFAULT_GOAL_SECONDS = 300;

    public void processWeightData(double weight) {
        if (weight >= EXERCISE_THRESHOLD && !isExerciseActive) {
            // Exercise started
            exerciseStartTime = LocalDateTime.now();
            isExerciseActive = true;
            System.out.println("Exercise started at: " + exerciseStartTime);
        } else if (weight < EXERCISE_THRESHOLD && isExerciseActive) {
            // Exercise ended
            LocalDateTime exerciseEndTime = LocalDateTime.now();
            long newSessionSeconds = ChronoUnit.SECONDS.between(exerciseStartTime, exerciseEndTime);
            isExerciseActive = false;

            LocalDate today = LocalDate.now();

            // 1) Find existing record for "today"
            LoadCellSession session = loadCellSessionRepository.findByDate(today);

            // 2) If none exists, create a new one
            if (session == null) {
                session = new LoadCellSession();
                session.setDate(today);
                session.setGoal(DEFAULT_GOAL_SECONDS); // or retrieve from user settings, etc.
            }

            // 3) Add to the total
            long updatedDuration = session.getDurationSeconds() + newSessionSeconds;
            session.setDurationSeconds(updatedDuration);

            // If goal > 0, update the percentage
            if (session.getGoal() > 0) {
                float percentage = (float) updatedDuration / session.getGoal() * 100f;
                session.setPercentage(percentage);
            } else {
                session.setPercentage(0);
            }

            // 5) Save back to DB
            loadCellSessionRepository.save(session);

            System.out.println("Exercise ended. This session: " + newSessionSeconds
                    + " seconds; total so far today: " + updatedDuration + " seconds");
        }
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
