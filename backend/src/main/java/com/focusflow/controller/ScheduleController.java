package com.focusflow.controller;

import com.focusflow.dto.ActualDurationUpdateRequest;
import com.focusflow.dto.AdaptiveScheduleSummaryDto;
import com.focusflow.dto.RecalculateScheduleRequest;
import com.focusflow.dto.ScheduleRequest;
import com.focusflow.dto.SessionStatusUpdateRequest;
import com.focusflow.dto.TopicPredictionRequest;
import com.focusflow.dto.TopicPredictionResponse;
import com.focusflow.entity.StudySession;
import com.focusflow.service.AdaptiveScheduleService;
import com.focusflow.service.MlPredictionService;
import com.focusflow.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Study Schedule generation, Session tracking, and duration updates.
 */
@RestController
@RequestMapping("/api/schedule")
@CrossOrigin(origins = "http://localhost:4200")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final AdaptiveScheduleService adaptiveScheduleService;
    private final MlPredictionService mlPredictionService;

    public ScheduleController(ScheduleService scheduleService,
                              AdaptiveScheduleService adaptiveScheduleService,
                              MlPredictionService mlPredictionService) {
        this.scheduleService = scheduleService;
        this.adaptiveScheduleService = adaptiveScheduleService;
        this.mlPredictionService = mlPredictionService;
    }

    /**
     * Predict realistic study duration for a specific topic using ML microservice with fallback.
     */
    @PostMapping("/predict-duration")
    public ResponseEntity<TopicPredictionResponse> predictTopicDuration(
            @Valid @RequestBody TopicPredictionRequest request) {
        TopicPredictionResponse response = mlPredictionService.predictForTopicId(request.getTopicId());
        return ResponseEntity.ok(response);
    }

    /**
     * Check connection health to the Python ML microservice.
     */
    @GetMapping("/ml-health")
    public ResponseEntity<Map<String, Object>> getMlHealth() {
        boolean available = mlPredictionService.isMlServiceAvailable();
        return ResponseEntity.ok(Map.of(
                "mlAvailable", available,
                "service", "FocusFlow ML Duration Predictor",
                "status", available ? "ONLINE" : "OFFLINE"
        ));
    }

    /**
     * Trigger adaptive recalculation of the schedule (handles missed sessions, partial completions, locked sessions).
     */
    @PostMapping("/recalculate")
    public ResponseEntity<AdaptiveScheduleSummaryDto> recalculateSchedule(
            @Valid @RequestBody(required = false) RecalculateScheduleRequest request) {
        AdaptiveScheduleSummaryDto summary = adaptiveScheduleService.recalculateSchedule(request);
        return ResponseEntity.ok(summary);
    }

    /**
     * Trigger greedy schedule generation for a specific date and available study hours.
     */
    @PostMapping("/generate")
    public ResponseEntity<List<StudySession>> generateSchedule(@Valid @RequestBody ScheduleRequest request) {
        List<StudySession> sessions = scheduleService.generateSchedule(request);
        return new ResponseEntity<>(sessions, HttpStatus.CREATED);
    }

    /**
     * Fetch study sessions scheduled for a particular date (defaults to today if omitted).
     */
    @GetMapping
    public ResponseEntity<List<StudySession>> getSessionsByDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate queryDate = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(scheduleService.getSessionsByDate(queryDate));
    }

    /**
     * Update session status (PLANNED, IN_PROGRESS, COMPLETED, MISSED, PARTIALLY_COMPLETED, RESCHEDULED)
     * and study duration/timestamps.
     */
    @PatchMapping("/sessions/{id}/status")
    public ResponseEntity<StudySession> updateSessionStatus(
            @PathVariable Long id,
            @Valid @RequestBody SessionStatusUpdateRequest request) {
        return ResponseEntity.ok(scheduleService.updateSessionStatus(id, request));
    }

    /**
     * Dedicated endpoint for updating only the actual study duration of a session.
     */
    @PatchMapping("/sessions/{id}/actual-duration")
    public ResponseEntity<StudySession> updateActualDuration(
            @PathVariable Long id,
            @Valid @RequestBody ActualDurationUpdateRequest request) {
        return ResponseEntity.ok(scheduleService.updateActualDuration(id, request));
    }

    /**
     * Toggle session lock to protect it from automatic rescheduling.
     */
    @PatchMapping("/sessions/{id}/lock")
    public ResponseEntity<StudySession> toggleSessionLock(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.toggleSessionLock(id));
    }

    /**
     * Delete an individual study session.
     */
    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
        scheduleService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }
}
