package com.focusflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * RevisionSession represents a scheduled spaced repetition revision for a completed topic.
 * Scoped to individual users via user ownership.
 */
@Entity
@Table(name = "revision_sessions", indexes = {
    @Index(name = "idx_revision_sessions_user", columnList = "user_id"),
    @Index(name = "idx_revision_sessions_topic", columnList = "topic_id"),
    @Index(name = "idx_revision_sessions_date", columnList = "scheduled_date")
})
public class RevisionSession {

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

    @NotNull(message = "Scheduled date is required")
    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "repetition_number", nullable = false)
    private int repetitionNumber = 0;

    @Column(name = "interval_days", nullable = false)
    private int intervalDays = 1;

    @Column(name = "easiness_factor", nullable = false)
    private double easinessFactor = 2.5;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RevisionStatus status = RevisionStatus.PENDING;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public RevisionSession() {
    }

    public RevisionSession(Topic topic, LocalDate scheduledDate, int repetitionNumber,
                           int intervalDays, double easinessFactor, RevisionStatus status) {
        this.topic = topic;
        this.scheduledDate = scheduledDate;
        this.repetitionNumber = repetitionNumber;
        this.intervalDays = intervalDays;
        this.easinessFactor = easinessFactor;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        if (topic != null && topic.getUser() != null) {
            this.user = topic.getUser();
        }
    }

    public RevisionSession(Topic topic, LocalDate scheduledDate, int repetitionNumber,
                           int intervalDays, double easinessFactor, RevisionStatus status, User user) {
        this.topic = topic;
        this.scheduledDate = scheduledDate;
        this.repetitionNumber = repetitionNumber;
        this.intervalDays = intervalDays;
        this.easinessFactor = easinessFactor;
        this.status = status;
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

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
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

    public RevisionStatus getStatus() {
        return status;
    }

    public void setStatus(RevisionStatus status) {
        this.status = status;
    }

    public LocalDateTime getLastReviewedAt() {
        return lastReviewedAt;
    }

    public void setLastReviewedAt(LocalDateTime lastReviewedAt) {
        this.lastReviewedAt = lastReviewedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
