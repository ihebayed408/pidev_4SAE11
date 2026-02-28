package com.esprit.planning.client;

import com.esprit.planning.dto.NotificationRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for the Notification microservice.
 * Sends notifications when progress updates or comments are created.
 */
@FeignClient(name = "notification", path = "/api/notifications")
public interface NotificationClient {

    @PostMapping
    ResponseEntity<?> create(@RequestBody NotificationRequestDto request);
}
