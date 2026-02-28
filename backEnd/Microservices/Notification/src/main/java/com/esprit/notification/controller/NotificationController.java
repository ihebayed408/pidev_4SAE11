package com.esprit.notification.controller;

import com.esprit.notification.dto.NotificationRequest;
import com.esprit.notification.dto.NotificationResponse;
import com.esprit.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "API for creating and managing notifications (callable by other microservices)")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @Operation(summary = "Create notification", description = "Create a notification for a user. Used by other microservices (e.g. planning, contract, offer).")
    public ResponseEntity<NotificationResponse> create(@Valid @RequestBody NotificationRequest request) {
        NotificationResponse created = notificationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "List by user", description = "Get all notifications for a user, newest first.")
    public List<NotificationResponse> findByUserId(@PathVariable String userId) {
        return notificationService.findByUserId(userId);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark as read")
    public NotificationResponse markRead(@PathVariable String id) {
        return notificationService.markRead(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete notification")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        notificationService.delete(id);
    }
}
