package com.esprit.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Notification record as returned by the API")
public class NotificationResponse {

    @Schema(description = "Firestore document ID")
    private String id;

    @Schema(description = "User ID (recipient)")
    private String userId;

    @Schema(description = "Notification title")
    private String title;

    @Schema(description = "Notification body")
    private String body;

    @Schema(description = "Type/category")
    private String type;

    @Schema(description = "Whether the notification has been read")
    private boolean read;

    @Schema(description = "Creation time (ISO-8601)")
    private Instant createdAt;

    @Schema(description = "Optional data payload")
    private Map<String, String> data;
}
