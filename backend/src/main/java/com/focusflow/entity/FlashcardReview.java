package com.focusflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * FlashcardReview logs individual flashcard practice outcomes.
 */
@Entity
@Table(name = "flashcard_reviews")
public class FlashcardReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Flashcard reference is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flashcard_id", nullable = false)
    @JsonIgnoreProperties("topic")
    private Flashcard flashcard;

    @NotNull(message = "Rating is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "rating", nullable = false, length = 20)
    private RecallRating rating;

    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt = LocalDateTime.now();

    @Column(name = "time_spent_seconds", nullable = false)
    private int timeSpentSeconds = 0;

    public FlashcardReview() {
    }

    public FlashcardReview(Flashcard flashcard, RecallRating rating, LocalDateTime reviewedAt, int timeSpentSeconds) {
        this.flashcard = flashcard;
        this.rating = rating;
        this.reviewedAt = reviewedAt;
        this.timeSpentSeconds = timeSpentSeconds;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Flashcard getFlashcard() {
        return flashcard;
    }

    public void setFlashcard(Flashcard flashcard) {
        this.flashcard = flashcard;
    }

    public RecallRating getRating() {
        return rating;
    }

    public void setRating(RecallRating rating) {
        this.rating = rating;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public int getTimeSpentSeconds() {
        return timeSpentSeconds;
    }

    public void setTimeSpentSeconds(int timeSpentSeconds) {
        this.timeSpentSeconds = timeSpentSeconds;
    }
}

