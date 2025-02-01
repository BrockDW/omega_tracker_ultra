package com.tracker.demo.service;

import com.tracker.demo.sql.entity.LoadCellSession;
import com.tracker.demo.sql.repository.LoadCellSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LoadCellService {

    @Autowired
    private LoadCellSessionRepository loadCellSessionRepository;

    private LocalDateTime exerciseStartTime;
    private boolean isExerciseActive = false;

    // Threshold to detect exercise (in kg)
    private static final float EXERCISE_THRESHOLD = 100f;

    public void processWeightData(double weight) {
        if (weight >= EXERCISE_THRESHOLD && !isExerciseActive) {
            // Exercise started
            exerciseStartTime = LocalDateTime.now();
            isExerciseActive = true;
            System.out.println("Exercise started at: " + exerciseStartTime);
        } else if (weight < EXERCISE_THRESHOLD && isExerciseActive) {
            // Exercise ended
            LocalDateTime exerciseEndTime = LocalDateTime.now();
            long durationSeconds = ChronoUnit.SECONDS.between(exerciseStartTime, exerciseEndTime);
            isExerciseActive = false;

            // Save the exercise session
            LoadCellSession session = new LoadCellSession();
            session.setDate(LocalDate.now());
            session.setStartTime(exerciseStartTime);
            session.setEndTime(exerciseEndTime);
            session.setDurationSeconds(durationSeconds);
            loadCellSessionRepository.save(session);

            System.out.println("Exercise ended. Duration: " + durationSeconds + " seconds");
        }
    }

    public long getTotalExerciseTimeToday() {
        LocalDate today = LocalDate.now();
        List<LoadCellSession> sessions = loadCellSessionRepository.findByDate(today);
        return sessions.stream().mapToLong(LoadCellSession::getDurationSeconds).sum();
    }
}
