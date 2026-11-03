package com.focusflow.controller;

import com.focusflow.dto.LearningSummaryDto;
import com.focusflow.dto.RetentionTrendsDto;
import com.focusflow.dto.SubjectMasteryDto;
import com.focusflow.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for Academic Learning Performance Analytics.
 * Serves aggregated academic metrics, subject mastery, and spaced repetition retention trends.
 */
@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:8080"})
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/learning-summary")
    public ResponseEntity<LearningSummaryDto> getLearningSummary() {
        return ResponseEntity.ok(analyticsService.getLearningSummary());
    }

    @GetMapping("/subject-mastery")
    public ResponseEntity<List<SubjectMasteryDto>> getSubjectMastery() {
        return ResponseEntity.ok(analyticsService.getSubjectMastery());
    }

    @GetMapping("/retention-trends")
    public ResponseEntity<RetentionTrendsDto> getRetentionTrends() {
        return ResponseEntity.ok(analyticsService.getRetentionTrends());
    }
}

