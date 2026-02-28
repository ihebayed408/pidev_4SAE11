package com.esprit.notification.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class NotificationHealthController {

    @Value("${welcome.message:Notification microservice}")
    private String welcomeMessage;

    @GetMapping("/welcome")
    public ResponseEntity<Map<String, String>> welcome() {
        return ResponseEntity.ok(Map.of(
            "service", "notification",
            "message", welcomeMessage
        ));
    }
}
