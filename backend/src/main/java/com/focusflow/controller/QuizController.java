package com.focusflow.controller;

import com.focusflow.dto.*;
import com.focusflow.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * QuizController exposes REST endpoints for quiz authoring, discovery,
 * secure student attempts, objective grading, and historical performance tracking.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    /**
     * POST /api/quizzes - Create a new subject or topic quiz.
     */
    @PostMapping("/quizzes")
    public ResponseEntity<QuizSummaryDto> createQuiz(@Valid @RequestBody QuizCreateRequest request) {
        QuizSummaryDto created = quizService.createQuiz(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * GET /api/quizzes - Get all quizzes (optionally filter by subjectId or topicId).
     */
    @GetMapping("/quizzes")
    public ResponseEntity<List<QuizSummaryDto>> getAllQuizzes(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long topicId) {
        List<QuizSummaryDto> list = quizService.getAllQuizzes(subjectId, topicId);
        return ResponseEntity.ok(list);
    }

    /**
     * GET /api/quizzes/{id} - Get quiz details and sanitized questions for student attempt.
     * Note: Correct answers and explanations are omitted to prevent cheating.
     */
    @GetMapping("/quizzes/{id}")
    public ResponseEntity<QuizDetailDto> getQuizForAttempt(@PathVariable Long id) {
        QuizDetailDto detail = quizService.getQuizForAttempt(id);
        return ResponseEntity.ok(detail);
    }

    /**
     * POST /api/quizzes/{id}/submit - Submit a student's attempt, calculate objective score,
     * record answer details, and return question-by-question breakdown.
     */
    @PostMapping("/quizzes/{id}/submit")
    public ResponseEntity<QuizAttemptResultDto> submitQuizAttempt(
            @PathVariable Long id,
            @Valid @RequestBody QuizAttemptSubmitRequest request) {
        QuizAttemptResultDto result = quizService.submitQuizAttempt(id, request);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/quiz-attempts - Retrieve historical quiz attempts.
     */
    @GetMapping("/quiz-attempts")
    public ResponseEntity<List<QuizAttemptHistoryDto>> getAttemptHistory(
            @RequestParam(required = false) Long quizId) {
        List<QuizAttemptHistoryDto> history = quizService.getAttemptHistory(quizId);
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/topics/{id}/quiz-performance - Retrieve quiz metrics for a specific topic.
     */
    @GetMapping("/topics/{id}/quiz-performance")
    public ResponseEntity<TopicQuizPerformanceDto> getTopicQuizPerformance(@PathVariable Long id) {
        TopicQuizPerformanceDto performance = quizService.getTopicQuizPerformance(id);
        return ResponseEntity.ok(performance);
    }
}

