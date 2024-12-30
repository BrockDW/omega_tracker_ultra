package com.tracker.demo.controller;

import com.tracker.demo.obj.Task;
import com.tracker.demo.service.DailyTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tasks")
public class TasksController {

    @Autowired
    private final DailyTaskService dailyTaskService;

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
}
