package com.focusflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Flashcard entity represents a concept flashcard with front prompt, back answer,
 * and per-card SM-2 spaced repetition state.
 * Scoped to individual users via user ownership.
 */
@Entity
@Table(name = "flashcards", indexes = {
    @Index(name = "idx_flashcards_user", columnList = "user_id"),
    @Index(name = "idx_flashcards_topic", columnList = "topic_id"),
    @Index(name = "idx_flashcards_review_date", columnList = "next_review_date")
})
public class Flashcard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @NotNull(message = "Topic reference is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "topic_id", nullable = false)
    @JsonIgnoreProperties("subject")
    private Topic topic;

    @NotBlank(message = "Front prompt cannot be empty")
    @Column(name = "front_prompt", columnDefinition = "TEXT", nullable = false)
    private String frontPrompt;

    @NotBlank(message = "Back answer cannot be empty")
    @Column(name = "back_answer", columnDefinition = "TEXT", nullable = false)
    private String backAnswer;

    @Column(name = "hint")
    private String hint;

    @Column(name = "repetition_number", nullable = false)
    private int repetitionNumber = 0;

    @Column(name = "interval_days", nullable = false)
    private int intervalDays = 1;

    @Column(name = "easiness_factor", nullable = false)
    private double easinessFactor = 2.5;

    @Column(name = "next_review_date", nullable = false)
    private LocalDate nextReviewDate = LocalDate.now();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Flashcard() {
    }

    public Flashcard(Topic topic, String frontPrompt, String backAnswer, String hint) {
        this.topic = topic;
        this.frontPrompt = frontPrompt;
        this.backAnswer = backAnswer;
        this.hint = hint;
        this.repetitionNumber = 0;
        this.intervalDays = 1;
        this.easinessFactor = 2.5;
        this.nextReviewDate = LocalDate.now();
        this.createdAt = LocalDateTime.now();
        if (topic != null && topic.getUser() != null) {
            this.user = topic.getUser();
        }
    }

    public Flashcard(Topic topic, String frontPrompt, String backAnswer, String hint, User user) {
        this.topic = topic;
        this.frontPrompt = frontPrompt;
        this.backAnswer = backAnswer;
        this.hint = hint;
        this.repetitionNumber = 0;
        this.intervalDays = 1;
        this.easinessFactor = 2.5;
        this.nextReviewDate = LocalDate.now();
        this.createdAt = LocalDateTime.now();
        this.user = user != null ? user : (topic != null ? topic.getUser() : null);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user != null ? user : (topic != null ? topic.getUser() : null);
    }

    public void setUser(User user) {
        this.user = user;
    }

    @JsonProperty("userId")
    public Long getUserId() {
        User u = getUser();
        return u != null ? u.getId() : null;
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
        if (this.user == null && topic != null && topic.getUser() != null) {
            this.user = topic.getUser();
        }
    }

    public String getFrontPrompt() {
        return frontPrompt;
    }

    public void setFrontPrompt(String frontPrompt) {
        this.frontPrompt = frontPrompt;
    }

    public String getBackAnswer() {
        return backAnswer;
    }

    public void setBackAnswer(String backAnswer) {
        this.backAnswer = backAnswer;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public int getRepetitionNumber() {
        return repetitionNumber;
    }

    public void setRepetitionNumber(int repetitionNumber) {
        this.repetitionNumber = repetitionNumber;
    }

    public int getIntervalDays() {
        return intervalDays;
    }

    public void setIntervalDays(int intervalDays) {
        this.intervalDays = intervalDays;
    }

    public double getEasinessFactor() {
        return easinessFactor;
    }

    public void setEasinessFactor(double easinessFactor) {
        this.easinessFactor = easinessFactor;
    }

    public LocalDate getNextReviewDate() {
        return nextReviewDate;
    }

    public void setNextReviewDate(LocalDate nextReviewDate) {
        this.nextReviewDate = nextReviewDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
