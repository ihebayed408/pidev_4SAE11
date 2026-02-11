package com.example.Evaluation.controller;

import com.example.Evaluation.entity.EvaluationTest;
import com.example.Evaluation.service.EvaluationTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evaluation-tests")
@RequiredArgsConstructor
public class EvaluationTestController {

    private final EvaluationTestService evaluationTestService;

    @GetMapping
    public ResponseEntity<List<EvaluationTest>> getAllTests() {
        return ResponseEntity.ok(evaluationTestService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EvaluationTest> getTestById(@PathVariable Long id) {
        return ResponseEntity.ok(evaluationTestService.findById(id));
    }

    @GetMapping("/skill/{skillId}")
    public ResponseEntity<List<EvaluationTest>> getTestsBySkill(@PathVariable Long skillId) {
        return ResponseEntity.ok(evaluationTestService.findBySkillId(skillId));
    }

    @PostMapping("/skill/{skillId}")
    public ResponseEntity<EvaluationTest> createTest(@RequestBody EvaluationTest test, @PathVariable Long skillId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(evaluationTestService.create(test, skillId));
    }

    @PostMapping("/generate/{skillId}")
    public ResponseEntity<EvaluationTest> generateTest(@PathVariable Long skillId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(evaluationTestService.generateTestForSkill(skillId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EvaluationTest> updateTest(@PathVariable Long id, @RequestBody EvaluationTest test) {
        return ResponseEntity.ok(evaluationTestService.update(id, test));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTest(@PathVariable Long id) {
        evaluationTestService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
