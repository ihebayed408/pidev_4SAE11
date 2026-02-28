package com.esprit.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to create a notification (used by other microservices)")
public class NotificationRequest {

    @NotBlank
    @Schema(description = "User ID (recipient)", example = "user-123", required = true)
    private String userId;

    @NotBlank
    @Schema(description = "Notification title", example = "New message", required = true)
    private String title;

    @Schema(description = "Notification body", example = "You have a new message from John")
    private String body;

    @Schema(description = "Type/category for filtering", example = "PROJECT_UPDATE")
    private String type;

    @Schema(description = "Optional key-value data payload")
    private Map<String, String> data;
}
