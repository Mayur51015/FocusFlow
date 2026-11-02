package com.focusflow.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * StudySession Entity represents a scheduled study block for a specific date.
 * Enhanced for Phase 1 session tracking and adaptive scheduling.
 */
@Entity
@Table(name = "study_sessions")
public class StudySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Topic is required for a study session")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @NotNull(message = "Scheduled date is required")
    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Min(value = 1, message = "Planned duration must be at least 1 minute")
    @Column(name = "planned_minutes", nullable = false)
    private int plannedMinutes;

    @Min(value = 0, message = "Actual duration cannot be negative")
    @Column(name = "actual_minutes", nullable = false)
    private int actualMinutes = 0;

    @NotNull(message = "Session status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SessionStatus status = SessionStatus.PLANNED;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @JsonProperty("isRescheduled")
    @Column(name = "is_rescheduled", nullable = false)
    private boolean isRescheduled = false;

    @Column(name = "locked", nullable = false)
    private boolean locked = false;

    @Column(name = "predicted_minutes")
    private Integer predictedMinutes;

    @Column(name = "prediction_source", length = 50)
    private String predictionSource;

    // Default constructor (required by JPA/Hibernate)
    public StudySession() {
    }

    // Parameterized constructor
    public StudySession(Topic topic, LocalDate scheduledDate, int plannedMinutes, int actualMinutes, SessionStatus status) {
        this.topic = topic;
        this.scheduledDate = scheduledDate;
        this.plannedMinutes = plannedMinutes;
        this.actualMinutes = actualMinutes;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public int getPlannedMinutes() {
        return plannedMinutes;
    }

    public void setPlannedMinutes(int plannedMinutes) {
        this.plannedMinutes = plannedMinutes;
    }

    public int getActualMinutes() {
        return actualMinutes;
    }

    public void setActualMinutes(int actualMinutes) {
        this.actualMinutes = actualMinutes;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    @JsonProperty("isRescheduled")
    public boolean isRescheduled() {
        return isRescheduled;
    }

    public void setRescheduled(boolean rescheduled) {
        isRescheduled = rescheduled;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public Integer getPredictedMinutes() {
        return predictedMinutes;
    }

    public void setPredictedMinutes(Integer predictedMinutes) {
        this.predictedMinutes = predictedMinutes;
    }

    public String getPredictionSource() {
        return predictionSource;
    }

    public void setPredictionSource(String predictionSource) {
        this.predictionSource = predictionSource;
    }
}
