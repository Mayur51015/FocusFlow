package com.focusflow.controller;

import com.focusflow.dto.StudyHistoryDto;
import com.focusflow.service.StudyHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for retrieving historical study sessions and performance records.
 */
@RestController
@RequestMapping("/api/study-history")
@CrossOrigin(origins = "http://localhost:4200")
public class StudyHistoryController {

    private final StudyHistoryService studyHistoryService;

    public StudyHistoryController(StudyHistoryService studyHistoryService) {
        this.studyHistoryService = studyHistoryService;
    }

    /**
     * Retrieve all historical study sessions or filter to completed/partially completed only.
     */
    @GetMapping
    public ResponseEntity<List<StudyHistoryDto>> getStudyHistory(
            @RequestParam(required = false, defaultValue = "false") boolean completedOnly) {
        if (completedOnly) {
            return ResponseEntity.ok(studyHistoryService.getCompletedStudyHistory());
        }
        return ResponseEntity.ok(studyHistoryService.getAllStudyHistory());
    }

    /**
     * Retrieve historical sessions for a specific topic.
     */
    @GetMapping("/topic/{topicId}")
    public ResponseEntity<List<StudyHistoryDto>> getTopicHistory(@PathVariable Long topicId) {
        return ResponseEntity.ok(studyHistoryService.getTopicHistory(topicId));
    }
}

