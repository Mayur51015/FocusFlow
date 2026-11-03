package com.focusflow.controller;

import com.focusflow.entity.Topic;
import com.focusflow.security.UserPrincipal;
import com.focusflow.service.TopicService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Topic management endpoints.
 * Enforces Role-Based Access Control and student data isolation.
 */
@RestController
@RequestMapping("/api/topics")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"}, allowCredentials = "true")
@PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN', 'ROLE_STUDENT')")
public class TopicController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping
    public ResponseEntity<List<Topic>> getAllTopics(
            @RequestParam(required = false) Long subjectId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        if (subjectId != null) {
            return ResponseEntity.ok(topicService.getTopicsBySubjectId(subjectId, userId));
        }
        return ResponseEntity.ok(topicService.getAllTopics());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Topic> getTopicById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(topicService.getTopicById(id, userId));
    }

    @PostMapping
    public ResponseEntity<Topic> createTopic(
            @Valid @RequestBody Topic topic,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        Topic created = topicService.createTopic(topic, userId);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Topic> updateTopic(
            @PathVariable Long id,
            @Valid @RequestBody Topic topic,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(topicService.updateTopic(id, topic, userId));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<Topic> toggleTopicCompletion(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(topicService.toggleTopicCompletion(id, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTopic(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        topicService.deleteTopic(id, userId);
        return ResponseEntity.noContent().build();
    }
}
