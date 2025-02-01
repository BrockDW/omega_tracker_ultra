package com.tracker.demo.controller;

import com.tracker.demo.dto.SensorData;
import com.tracker.demo.service.LoadCellService;
import com.tracker.demo.service.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Esp32Controller {

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private LoadCellService loadCellService;

    // Endpoint to receive data from ESP32
    @PostMapping("/weight/data")
    public String receiveData(@RequestBody SensorData sensorData) {
        // Log the received data
        System.out.println("Received data: " + sensorData);

        loadCellService.processWeightData(sensorData.getWeight());

        // Forward the data to WebSocket clients
        webSocketService.sendDataToClients(sensorData);

        return "Data received and forwarded to WebSocket clients!";
    }

    @GetMapping("/weight/total-time-today")
    public long getTotalExerciseTimeToday() {
        return loadCellService.getTotalExerciseTimeToday();
    }


}
