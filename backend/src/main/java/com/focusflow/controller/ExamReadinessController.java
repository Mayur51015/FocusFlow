package com.focusflow.controller;

import com.focusflow.dto.ExamReadinessDto;
import com.focusflow.dto.ExamReadinessOverviewDto;
import com.focusflow.service.ExamReadinessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Academic Exam Readiness Estimation.
 * Provides endpoints for subject-level readiness, calculation triggers, and academic overview.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:8080"})
public class ExamReadinessController {

    private final ExamReadinessService examReadinessService;

    public ExamReadinessController(ExamReadinessService examReadinessService) {
        this.examReadinessService = examReadinessService;
    }

    @GetMapping("/subjects/{id}/exam-readiness")
    public ResponseEntity<ExamReadinessDto> getSubjectReadiness(@PathVariable Long id) {
        return ResponseEntity.ok(examReadinessService.getSubjectReadiness(id));
    }

    @PostMapping("/subjects/{id}/calculate-readiness")
    public ResponseEntity<ExamReadinessDto> calculateSubjectReadiness(@PathVariable Long id) {
        return ResponseEntity.ok(examReadinessService.calculateSubjectReadiness(id));
    }

    @GetMapping("/exam-readiness/overview")
    public ResponseEntity<ExamReadinessOverviewDto> getReadinessOverview() {
        return ResponseEntity.ok(examReadinessService.getReadinessOverview());
    }

    @PostMapping("/exam-readiness/calculate-all")
    public ResponseEntity<List<ExamReadinessDto>> calculateAllReadiness() {
        return ResponseEntity.ok(examReadinessService.calculateAllReadiness());
    }
}

