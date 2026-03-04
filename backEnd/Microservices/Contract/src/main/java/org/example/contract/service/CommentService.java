package org.example.contract.service;

import org.example.contract.entity.Comment;
import org.example.contract.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;

    @Autowired
    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public List<Comment> getByConflictId(Long conflictId) {
        return commentRepository.findByConflictIdOrderByCreatedAtAsc(conflictId);
    }

    public Comment create(Long conflictId, Comment comment) {
        comment.setConflictId(conflictId);
        return commentRepository.save(comment);
    }

    public Comment update(Long id, String content) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found: " + id));
        comment.setContent(content);
        return commentRepository.save(comment);
    }

    public void delete(Long id) {
        commentRepository.deleteById(id);
    }
}
