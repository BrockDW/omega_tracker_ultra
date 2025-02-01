package com.tracker.demo.service;

import com.tracker.demo.dto.SensorData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Send data to all subscribed WebSocket clients
    public void sendDataToClients(SensorData sensorData) {
        messagingTemplate.convertAndSend("/topic/sensor-data", sensorData);
    }
}