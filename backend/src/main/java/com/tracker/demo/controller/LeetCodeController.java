package com.tracker.demo.controller;

import com.tracker.demo.dto.LeetCodeResult;
import com.tracker.demo.dto.LoadCellExerciseResult;
import com.tracker.demo.service.LeetCodeStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
public class LeetCodeController {

    @Autowired
    private LeetCodeStatsService leetCodeStatsService;

    // Single-day endpoint remains the same (optional)
    @GetMapping("/leetcode/day/{dateStr}")
    public LeetCodeResult getPracticeForDay(@PathVariable String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        return leetCodeStatsService.getLeetCodePracticeData(date);
    }

    // New range endpoint
    @GetMapping("/leetcode/range")
    public Map<LocalDate, LeetCodeResult> getPracticeForRange(
            @RequestParam String start,
            @RequestParam String end) {
        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(end);
        return leetCodeStatsService.getLeetCodePracticeDataRange(startDate, endDate);
    }
}
