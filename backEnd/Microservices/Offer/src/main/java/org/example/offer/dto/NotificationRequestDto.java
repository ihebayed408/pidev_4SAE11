package org.example.offer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request body for the Notification microservice (create notification).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequestDto {

    private String userId;
    private String title;
    private String body;
    private String type;
    private Map<String, String> data;
}
