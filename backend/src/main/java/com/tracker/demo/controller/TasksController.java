package com.tracker.demo.controller;

import com.tracker.demo.dto.DailyTaskProgressDto;
import com.tracker.demo.dto.ExclusionRequest;
import com.tracker.demo.dto.KeyBrPracticeResult;
import com.tracker.demo.dto.Task;
import com.tracker.demo.service.DailyTaskService;
import com.tracker.demo.service.KeybrScraperServiceV2;
import com.tracker.demo.sql.entity.KeyBrPracticeRecord;
import com.tracker.demo.sql.repository.KeyBrPracticeRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/tasks")
public class TasksController {

    @Autowired
    private final DailyTaskService dailyTaskService;

    @Autowired
    private KeybrScraperServiceV2 keybrScraperServiceV2;

    @Autowired
    private KeyBrPracticeRecordRepository keyBrPracticeRecordRepository;

    public TasksController(DailyTaskService dailyTaskService) {
        this.dailyTaskService = dailyTaskService;
    }

    // Endpoint: GET /api/tasks/today
    @GetMapping("/today")
    public List<Task> getTodayTasks() {
        // Calls your service to fetch and parse the Markdown file for today
        return dailyTaskService.fetchMarkdownLocalDate(LocalDate.now());
    }

    // Single day
    @GetMapping("/day/{dateStr}")
    public List<Task> getTasksForDay(@PathVariable String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        return dailyTaskService.fetchMarkdownLocalDate(date);
    }

    @GetMapping("/day/{dateStr}/v2")
    public DailyTaskProgressDto getTasksForDayV2(@PathVariable String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);

        // 1) Get tasks
        List<Task> tasks = dailyTaskService.fetchMarkdownLocalDate(date);

        KeyBrPracticeResult keyBrResult = new KeyBrPracticeResult(0, 0, 0);
        if (date.equals(LocalDate.now())) {
            keyBrResult = keybrScraperServiceV2.getPracticeTimeWithSession();
        } else {
            // 2) Get practice keyBrRecord
            KeyBrPracticeRecord keyBrRecord = keyBrPracticeRecordRepository.findByPracticeDate(date);
            if (Objects.nonNull(keyBrRecord)) {
                keyBrResult = new KeyBrPracticeResult(keyBrRecord.getPercentage(), keyBrRecord.getTotalMinutes(), keyBrRecord.getMinutesPracticed());
            }
        }
        return new DailyTaskProgressDto(date, tasks, keyBrResult);
    }

    // Range
    @GetMapping("/range")
    public Map<LocalDate, List<Task>> getTasksInRange(
            @RequestParam String start,
            @RequestParam String end
    ) {
        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(end);
        return dailyTaskService.fetchMarkdownInRange(startDate, endDate);
    }

    // Existing GET endpoint:
    @GetMapping("/incomplete-aggregated")
    public Map<String, List<Task>> getIncompleteAggregatedTasks(
            @RequestParam String start,
            @RequestParam String end
    ) {
        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(end);
        return dailyTaskService.fetchAggregatedIncompleteTasks(startDate, endDate);
    }

    // NEW POST endpoint:
    @PostMapping("/exclusion")
    public ResponseEntity<Map<String, String>> setExclusion(@RequestBody ExclusionRequest request) {
        dailyTaskService.setExclusion(request.getDescription(), request.isExcluded());
        return ResponseEntity.ok(Map.of("status", "success"));
    }
}
