package com.focusflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * RevisionHistory logs every individual SM-2 recall review event, recording ratings,
 * interval shifts, and easiness factor adjustments.
 */
@Entity
@Table(name = "revision_histories")
public class RevisionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Revision session reference is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revision_session_id", nullable = false)
    @JsonIgnoreProperties("topic")
    private RevisionSession revisionSession;

    @NotNull(message = "Topic reference is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "topic_id", nullable = false)
    @JsonIgnoreProperties("subject")
    private Topic topic;

    @NotNull(message = "Recall rating is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "rating", nullable = false, length = 20)
    private RecallRating rating;

    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt = LocalDateTime.now();

    @Column(name = "previous_interval_days", nullable = false)
    private int previousIntervalDays;

    @Column(name = "new_interval_days", nullable = false)
    private int newIntervalDays;

    @Column(name = "previous_ef", nullable = false)
    private double previousEf;

    @Column(name = "new_ef", nullable = false)
    private double newEf;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public RevisionHistory() {
    }

    public RevisionHistory(RevisionSession revisionSession, Topic topic, RecallRating rating,
                           LocalDateTime reviewedAt, int previousIntervalDays, int newIntervalDays,
                           double previousEf, double newEf, String notes) {
        this.revisionSession = revisionSession;
        this.topic = topic;
        this.rating = rating;
        this.reviewedAt = reviewedAt;
        this.previousIntervalDays = previousIntervalDays;
        this.newIntervalDays = newIntervalDays;
        this.previousEf = previousEf;
        this.newEf = newEf;
        this.notes = notes;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RevisionSession getRevisionSession() {
        return revisionSession;
    }

    public void setRevisionSession(RevisionSession revisionSession) {
        this.revisionSession = revisionSession;
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
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

    public int getPreviousIntervalDays() {
        return previousIntervalDays;
    }

    public void setPreviousIntervalDays(int previousIntervalDays) {
        this.previousIntervalDays = previousIntervalDays;
    }

    public int getNewIntervalDays() {
        return newIntervalDays;
    }

    public void setNewIntervalDays(int newIntervalDays) {
        this.newIntervalDays = newIntervalDays;
    }

    public double getPreviousEf() {
        return previousEf;
    }

    public void setPreviousEf(double previousEf) {
        this.previousEf = previousEf;
    }

    public double getNewEf() {
        return newEf;
    }

    public void setNewEf(double newEf) {
        this.newEf = newEf;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

