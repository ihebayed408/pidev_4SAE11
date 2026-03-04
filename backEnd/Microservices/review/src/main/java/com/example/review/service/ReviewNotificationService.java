package com.example.review.service;

import com.example.review.client.NotificationClient;
import com.example.review.dto.NotificationRequestDto;
import com.example.review.entity.Review;
import com.example.review.entity.ReviewResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Sends notifications to the Notification microservice when a review response (message)
 * is received. Notifies the reviewee (the person who received the review).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewNotificationService {

    public static final String TYPE_REVIEW_RESPONSE = "REVIEW_RESPONSE";

    private final NotificationClient notificationClient;

    /**
     * Notify the reviewee when someone responds to a review about them.
     * Catches and logs any exception so Review is not affected if Notification is down.
     */
    public void notifyReviewResponseReceived(ReviewResponse response) {
        if (response == null || response.getReview() == null) {
            return;
        }
        Review review = response.getReview();
        String revieweeId = String.valueOf(review.getRevieweeId());
        if (revieweeId == null || revieweeId.isBlank()) {
            return;
        }

        String messagePreview = truncate(response.getMessage(), 100);
        String title = "New response to your review";
        String body = "Someone replied to a review about you: \"" + messagePreview + "\"";

        Map<String, String> data = new HashMap<>();
        data.put("reviewId", String.valueOf(review.getId()));
        data.put("reviewResponseId", String.valueOf(response.getId()));
        data.put("respondentId", String.valueOf(response.getRespondentId()));

        try {
            NotificationRequestDto request = NotificationRequestDto.builder()
                .userId(revieweeId)
                .title(title)
                .body(body)
                .type(TYPE_REVIEW_RESPONSE)
                .data(data)
                .build();
            notificationClient.create(request);
            log.debug("Sent notification for review response {} to reviewee {}", response.getId(), revieweeId);
        } catch (Exception e) {
            log.warn("Failed to send notification to reviewee {} for review response {}: {}",
                revieweeId, response.getId(), e.getMessage());
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
