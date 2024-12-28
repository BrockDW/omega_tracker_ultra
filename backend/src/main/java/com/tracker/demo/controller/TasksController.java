package com.tracker.demo.controller;

import com.tracker.demo.obj.Task;
import com.tracker.demo.service.DailyTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TasksController {

    @Autowired
    private final DailyTaskService dailyTaskService;

    public TasksController(DailyTaskService dailyTaskService) {
        this.dailyTaskService = dailyTaskService;
    }

    // Endpoint: GET /api/tasks/today
    @GetMapping("/api/tasks/today")
    public List<Task> getTodayTasks() {
        // Calls your service to fetch and parse the Markdown file for today
        return dailyTaskService.getTodayTasks();
    }
}
