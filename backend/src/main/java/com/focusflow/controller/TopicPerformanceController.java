package com.focusflow.controller;

import com.focusflow.dto.TopicPerformanceDto;
import com.focusflow.service.TopicPerformanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Topic Performance Tracking and Weak Topic Detection.
 */
@RestController
@RequestMapping("/api/topics")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:8080"})
public class TopicPerformanceController {

    private final TopicPerformanceService topicPerformanceService;

    public TopicPerformanceController(TopicPerformanceService topicPerformanceService) {
        this.topicPerformanceService = topicPerformanceService;
    }

    /**
     * Retrieve performance classification across all topics.
     */
    @GetMapping("/performance")
    public ResponseEntity<List<TopicPerformanceDto>> getAllPerformances() {
        return ResponseEntity.ok(topicPerformanceService.getAllPerformances());
    }

    /**
     * Retrieve all topics currently identified as WEAK.
     */
    @GetMapping("/weak")
    public ResponseEntity<List<TopicPerformanceDto>> getWeakTopics() {
        return ResponseEntity.ok(topicPerformanceService.getWeakTopics());
    }

    /**
     * Retrieve performance for a specific topic.
     */
    @GetMapping("/{id}/performance")
    public ResponseEntity<TopicPerformanceDto> getTopicPerformance(@PathVariable("id") Long id) {
        return ResponseEntity.ok(topicPerformanceService.getPerformanceByTopicId(id));
    }

    /**
     * Trigger on-demand ML evaluation for a specific topic.
     */
    @PostMapping("/{id}/evaluate-performance")
    public ResponseEntity<TopicPerformanceDto> evaluateTopic(@PathVariable("id") Long id) {
        return ResponseEntity.ok(topicPerformanceService.evaluateTopic(id));
    }

    /**
     * Trigger on-demand evaluation across all active topics.
     */
    @PostMapping("/evaluate-all")
    public ResponseEntity<List<TopicPerformanceDto>> evaluateAllTopics() {
        return ResponseEntity.ok(topicPerformanceService.evaluateAllTopics());
    }
}

