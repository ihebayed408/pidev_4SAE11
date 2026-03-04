package org.example.contract.controller;

import org.example.contract.entity.Comment;
import org.example.contract.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    @Autowired
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/conflict/{conflictId}")
    public ResponseEntity<List<Comment>> getByConflict(@PathVariable Long conflictId) {
        return ResponseEntity.ok(commentService.getByConflictId(conflictId));
    }

    @PostMapping("/conflict/{conflictId}")
    public ResponseEntity<Comment> create(@PathVariable Long conflictId, @RequestBody Comment comment) {
        return ResponseEntity.ok(commentService.create(conflictId, comment));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Comment> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(commentService.update(id, body.get("content")));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
