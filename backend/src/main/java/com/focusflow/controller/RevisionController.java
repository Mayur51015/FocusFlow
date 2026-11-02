package com.focusflow.controller;

import com.focusflow.dto.*;
import com.focusflow.service.RevisionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Spaced Repetition Revisions and Flashcard Reviews (MVP 2 Phase 3).
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:8080"})
public class RevisionController {

    private final RevisionService revisionService;

    public RevisionController(RevisionService revisionService) {
        this.revisionService = revisionService;
    }

    /**
     * Retrieve revision sessions due today or overdue.
     */
    @GetMapping("/revisions/today")
    public ResponseEntity<List<RevisionSessionDto>> getTodayRevisions() {
        return ResponseEntity.ok(revisionService.getTodayRevisions());
    }

    /**
     * Retrieve future upcoming revision sessions.
     */
    @GetMapping("/revisions/upcoming")
    public ResponseEntity<List<RevisionSessionDto>> getUpcomingRevisions() {
        return ResponseEntity.ok(revisionService.getUpcomingRevisions());
    }

    /**
     * Submit an SM-2 recall review for a scheduled revision session.
     */
    @PostMapping("/revisions/{id}/review")
    public ResponseEntity<RevisionSessionDto> reviewRevision(
            @PathVariable("id") Long id,
            @Valid @RequestBody RevisionReviewRequest request) {
        return ResponseEntity.ok(revisionService.reviewRevision(id, request));
    }

    /**
     * Retrieve complete audit history of all past recall reviews.
     */
    @GetMapping("/revisions/history")
    public ResponseEntity<List<RevisionHistoryDto>> getRevisionHistory() {
        return ResponseEntity.ok(revisionService.getRevisionHistory());
    }

    // --- Flashcards Endpoints ---

    /**
     * Retrieve all concept flashcards.
     */
    @GetMapping("/flashcards")
    public ResponseEntity<List<FlashcardDto>> getAllFlashcards() {
        return ResponseEntity.ok(revisionService.getAllFlashcards());
    }

    /**
     * Retrieve flashcards due for review.
     */
    @GetMapping("/flashcards/due")
    public ResponseEntity<List<FlashcardDto>> getDueFlashcards() {
        return ResponseEntity.ok(revisionService.getDueFlashcards());
    }

    /**
     * Retrieve flashcards for a specific topic.
     */
    @GetMapping("/topics/{topicId}/flashcards")
    public ResponseEntity<List<FlashcardDto>> getFlashcardsByTopic(@PathVariable("topicId") Long topicId) {
        return ResponseEntity.ok(revisionService.getFlashcardsByTopic(topicId));
    }

    /**
     * Create a new concept flashcard for a topic.
     */
    @PostMapping("/flashcards")
    public ResponseEntity<FlashcardDto> createFlashcard(@Valid @RequestBody FlashcardCreateRequest request) {
        FlashcardDto created = revisionService.createFlashcard(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Review a flashcard using SM-2 recall rating.
     */
    @PostMapping("/flashcards/{id}/review")
    public ResponseEntity<FlashcardDto> reviewFlashcard(
            @PathVariable("id") Long id,
            @Valid @RequestBody FlashcardReviewRequest request) {
        return ResponseEntity.ok(revisionService.reviewFlashcard(id, request));
    }
}

