package com.tracker.demo.controller;

import com.tracker.demo.dto.KeyBrPracticeResult;
import com.tracker.demo.dto.LoadCellExerciseResult;
import com.tracker.demo.dto.SensorData;
import com.tracker.demo.service.LoadCellService;
import com.tracker.demo.service.WebSocketService;
import com.tracker.demo.sql.entity.LoadCellSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
public class Esp32Controller {

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private LoadCellService loadCellService;

    // Endpoint to receive data from ESP32
    @PostMapping("/weight/data")
    public String receiveData(@RequestBody SensorData sensorData) {
        loadCellService.processWeightData(sensorData.getWeight());
        // Forward the data to WebSocket clients
        webSocketService.sendDataToClients(sensorData);

        return "Data received and forwarded to WebSocket clients!";
    }

    @GetMapping("/weight/total-time-today")
    public LoadCellExerciseResult getTotalExerciseTimeToday() {
        return loadCellService.getTotalExerciseTimeToday();
    }

    // Single-day endpoint remains the same (optional)
    @GetMapping("/weight/day/{dateStr}")
    public LoadCellExerciseResult getPracticeForDay(@PathVariable String dateStr) {
        return loadCellService.getTotalExerciseTimeForDay(dateStr);
    }

    @GetMapping("/weight/range")
    public Map<LocalDate, LoadCellExerciseResult> getTotalExerciseTimeInRange(@RequestParam String start,
                                                            @RequestParam String end) {
        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(end);

        return loadCellService.getTotalExerciseTimeInRange(startDate, endDate);
    }
}
