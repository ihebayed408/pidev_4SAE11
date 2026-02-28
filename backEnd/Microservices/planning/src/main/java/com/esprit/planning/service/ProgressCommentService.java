package com.esprit.planning.service;

import com.esprit.planning.client.UserClient;
import com.esprit.planning.dto.UserDto;
import com.esprit.planning.entity.ProgressComment;
import com.esprit.planning.entity.ProgressUpdate;
import com.esprit.planning.repository.ProgressCommentRepository;
import com.esprit.planning.repository.ProgressUpdateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgressCommentService {

    private final ProgressCommentRepository progressCommentRepository;
    private final ProgressUpdateRepository progressUpdateRepository;
    private final UserClient userClient;
    private final PlanningNotificationService planningNotificationService;

    @Transactional(readOnly = true)
    public List<ProgressComment> findAll() {
        return progressCommentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<ProgressComment> findAllPaged(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        return progressCommentRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public ProgressComment findById(Long id) {
        return progressCommentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ProgressComment not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<ProgressComment> findByProgressUpdateId(Long progressUpdateId) {
        return progressCommentRepository.findByProgressUpdate_Id(progressUpdateId);
    }

    @Transactional(readOnly = true)
    public List<ProgressComment> findByUserId(Long userId) {
        return progressCommentRepository.findByUserId(userId);
    }

    @Transactional
    public ProgressComment create(Long progressUpdateId, Long userId, String message) {
        ProgressUpdate progressUpdate = progressUpdateRepository.findById(progressUpdateId)
                .orElseThrow(() -> new RuntimeException("ProgressUpdate not found with id: " + progressUpdateId));

        // Optionally validate that the user exists in the User microservice.
        // This will throw if the user-service returns an error (4xx/5xx).
        UserDto user = userClient.getUserById(userId);

        ProgressComment comment = ProgressComment.builder()
                .progressUpdate(progressUpdate)
                .userId(user.getId())
                .message(message)
                .build();
        ProgressComment saved = progressCommentRepository.save(comment);
        // Notify the freelancer who owns the progress update (if someone else commented)
        Long freelancerId = progressUpdate.getFreelancerId();
        if (freelancerId != null && !freelancerId.equals(userId)) {
            String body = message != null && message.length() > 200 ? message.substring(0, 200) + "..." : message;
            planningNotificationService.notifyUser(
                String.valueOf(freelancerId),
                "New comment on your progress update",
                body,
                PlanningNotificationService.TYPE_PROGRESS_COMMENT,
                java.util.Map.of(
                    "progressUpdateId", String.valueOf(progressUpdate.getId()),
                    "commentId", String.valueOf(saved.getId()),
                    "projectId", String.valueOf(progressUpdate.getProjectId())
                ));
        }
        return saved;
    }

    @Transactional
    public ProgressComment update(Long id, String message) {
        ProgressComment existing = findById(id);
        existing.setMessage(message);
        ProgressComment saved = progressCommentRepository.save(existing);
        notifyFreelancerAboutComment(saved.getProgressUpdate(), "A comment on your progress update was edited", message);
        return saved;
    }

    @Transactional
    public void deleteById(Long id) {
        ProgressComment existing = findById(id);
        var progressUpdate = existing.getProgressUpdate();
        progressCommentRepository.deleteById(id);
        notifyFreelancerAboutComment(progressUpdate, "A comment was removed from your progress update", null);
    }

    private void notifyFreelancerAboutComment(ProgressUpdate progressUpdate, String title, String body) {
        if (progressUpdate == null) return;
        Long freelancerId = progressUpdate.getFreelancerId();
        if (freelancerId == null) return;
        String bodyText = body != null && body.length() > 200 ? body.substring(0, 200) + "..." : (body != null ? body : "");
        planningNotificationService.notifyUser(
            String.valueOf(freelancerId),
            title,
            bodyText,
            PlanningNotificationService.TYPE_PROGRESS_COMMENT,
            java.util.Map.of(
                "progressUpdateId", String.valueOf(progressUpdate.getId()),
                "projectId", String.valueOf(progressUpdate.getProjectId())
            ));
    }
}