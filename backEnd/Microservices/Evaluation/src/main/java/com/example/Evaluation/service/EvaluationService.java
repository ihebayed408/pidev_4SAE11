package com.example.Evaluation.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Evaluation.dto.AnswerSubmission;
import com.example.Evaluation.dto.TestSubmission;
import com.example.Evaluation.entity.Evaluation;
import com.example.Evaluation.entity.EvaluationTest;
import com.example.Evaluation.entity.Question;
import com.example.Evaluation.entity.Skill;
import com.example.Evaluation.repository.EvaluationRepository;
import com.example.Evaluation.repository.EvaluationTestRepository;
import com.example.Evaluation.repository.SkillRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final SkillRepository skillRepository;
    private final EvaluationTestRepository evaluationTestRepository;

    @Transactional(readOnly = true)
    public List<Evaluation> findAll() {
        return evaluationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Evaluation findById(Long id) {
        return evaluationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evaluation not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Evaluation> findByFreelancerId(Long freelancerId) {
        return evaluationRepository.findByFreelancerId(freelancerId);
    }

    @Transactional
    public Evaluation create(Evaluation evaluation, Long skillId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found with id: " + skillId));
        evaluation.setSkill(skill);
        return evaluationRepository.save(evaluation);
    }

    @Transactional
    public Evaluation submitEvaluation(TestSubmission submission) {
        EvaluationTest test = evaluationTestRepository.findById(submission.getTestId())
                .orElseThrow(() -> new RuntimeException("Test not found with id: " + submission.getTestId()));

        double totalScore = 0;
        int correctAnswers = 0;
        List<Question> questions = test.getQuestions();

        for (AnswerSubmission answer : submission.getAnswers()) {
            if (answer.getQuestionIndex() >= 0 && answer.getQuestionIndex() < questions.size()) {
                Question question = questions.get(answer.getQuestionIndex());
                if (question.getCorrectOption() != null && question.getCorrectOption().equals(answer.getSelectedOption())) {
                    totalScore += question.getPoints() != null ? question.getPoints() : 1;
                    correctAnswers++;
                }
            }
        }

        boolean passed = totalScore >= test.getPassingScore();

        Evaluation evaluation = Evaluation.builder()
                .freelancerId(submission.getFreelancerId())
                .skill(test.getSkill())
                .score(totalScore)
                .passed(passed)
                .testResult("Correct answers: " + correctAnswers + "/" + questions.size())
                .evaluatedAt(LocalDateTime.now())
                .build();

        return evaluationRepository.save(evaluation);
    }

    @Transactional
    public void delete(Long id) {
        if (!evaluationRepository.existsById(id)) {
            throw new RuntimeException("Evaluation not found with id: " + id);
        }
        evaluationRepository.deleteById(id);
    }
}
