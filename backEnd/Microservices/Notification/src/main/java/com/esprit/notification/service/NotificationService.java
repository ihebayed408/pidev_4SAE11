package com.esprit.notification.service;

import com.esprit.notification.dto.NotificationRequest;
import com.esprit.notification.dto.NotificationResponse;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String COLLECTION = "notifications";

    private final Firestore firestore;

    public NotificationResponse create(NotificationRequest request) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", request.getUserId());
        data.put("title", request.getTitle());
        data.put("body", request.getBody() != null ? request.getBody() : "");
        data.put("type", request.getType() != null ? request.getType() : "GENERAL");
        data.put("read", false);
        data.put("createdAt", Instant.now().toString());
        if (request.getData() != null && !request.getData().isEmpty()) {
            data.put("data", request.getData());
        }

        try {
            DocumentReference ref = firestore.collection(COLLECTION).add(data).get();
            DocumentSnapshot snap = ref.get().get();
            return toResponse(ref.getId(), snap.getData());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to create notification", e);
        }
    }

    public List<NotificationResponse> findByUserId(String userId) {
        try {
            return firestore.collection(COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .get()
                .getDocuments()
                .stream()
                .map(d -> toResponse(d.getId(), d.getData()))
                .sorted(Comparator.comparing(NotificationResponse::getCreatedAt).reversed())
                .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to list notifications", e);
        }
    }

    public NotificationResponse markRead(String id) {
        try {
            DocumentReference ref = firestore.collection(COLLECTION).document(id);
            ref.update("read", true).get();
            DocumentSnapshot snap = ref.get().get();
            return toResponse(id, snap.getData());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to mark notification as read", e);
        }
    }

    public void delete(String id) {
        try {
            firestore.collection(COLLECTION).document(id).delete().get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to delete notification", e);
        }
    }

    private static NotificationResponse toResponse(String id, Map<String, Object> data) {
        if (data == null) return null;
        return NotificationResponse.builder()
            .id(id)
            .userId((String) data.get("userId"))
            .title((String) data.get("title"))
            .body((String) data.get("body"))
            .type((String) data.get("type"))
            .read(Boolean.TRUE.equals(data.get("read")))
            .createdAt(Instant.parse((String) data.get("createdAt")))
            .data(data.get("data") != null ? (Map<String, String>) (Map<?, ?>) data.get("data") : null)
            .build();
    }
}
