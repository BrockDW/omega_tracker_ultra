package com.tracker.demo.service;

import com.tracker.demo.dto.LeetCodeApiResponse;
import com.tracker.demo.dto.LeetCodeResult;
import com.tracker.demo.sql.entity.LeetCodeStats;
import com.tracker.demo.sql.repository.LeetCodeStatsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class LeetCodeStatsService {
    @Value("${leetcode.userId}")
    private String username;

    private final int goal = 1;

    @Autowired
    private LeetCodeStatsRepository repository;

    private final RestTemplate restTemplate = new RestTemplate();

    public LeetCodeResult getLeetCodePracticeData(LocalDate date) {
        if (date.equals(LocalDate.now())) {
            // Call the external API
            String url = "https://leetcode-stats-api.herokuapp.com/" + username;
            LeetCodeApiResponse response = restTemplate.getForObject(url, LeetCodeApiResponse.class);

            if (response != null && "success".equals(response.getStatus())) {
                LeetCodeStats stats = repository.findByDate(date);

                int todayTotalSolved = response.getTotalSolved();

                if (stats == null) {
                    // Create new entry for today
                    stats = new LeetCodeStats();
                    stats.setDate(date);
                    stats.setGoal(goal);
                }
                Optional<LeetCodeStats> latestRecord = repository.findTopByDateBeforeOrderByDateDesc(date);

                if (latestRecord.isPresent()) {
                    int todayResolved = todayTotalSolved - latestRecord.get().getTotalSolved();
                    stats.setTotalSolved(todayTotalSolved);
                    stats.setResolved(todayResolved);
                    repository.save(stats);
                    return new LeetCodeResult(((double) todayResolved / goal) * 100f, goal, todayResolved);
                } else {
                    stats.setTotalSolved(todayTotalSolved);
                    stats.setResolved(todayTotalSolved);
                    repository.save(stats);
                    // No previous record found, return today's totalSolved
                    return new LeetCodeResult(100f, goal, todayTotalSolved);
                }
            } else {
                LeetCodeStats stats = repository.findByDate(date);

                if (stats == null) {
                    return new LeetCodeResult(0, goal, 0);
                } else {
                    return new LeetCodeResult(((double) stats.getResolved() / goal) * 100f, goal, stats.getTotalSolved());
                }
            }
        } else {
            LeetCodeStats stats = repository.findByDate(date);

            if (stats == null) {
                return new LeetCodeResult(0, goal, 0);
            } else {
                return new LeetCodeResult(((double) stats.getResolved() / goal) * 100f, goal, stats.getTotalSolved());
            }
        }
    }

    // New method for range data
    public Map<LocalDate, LeetCodeResult> getLeetCodePracticeDataRange(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, LeetCodeResult> resultMap = new HashMap<>();

        // Fetch all stats for the date range
        List<LeetCodeStats> statsList = repository.findByDateBetween(startDate, endDate);

        // Process each day's stats
        for (LeetCodeStats stats : statsList) {
            LocalDate date = stats.getDate();
            int resolved = stats.getResolved();
            int goal = stats.getGoal();

            // Calculate percentage
            double percentage = ((double) resolved / goal) * 100;

            // Create a LeetCodeResult object
            LeetCodeResult result = new LeetCodeResult(percentage, goal, resolved);

            // Add to the result map
            resultMap.put(date, result);
        }

        return resultMap;
    }
}
