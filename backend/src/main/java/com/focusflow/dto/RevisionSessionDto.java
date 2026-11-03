package com.focusflow.dto;

import com.focusflow.entity.Difficulty;
import com.focusflow.entity.RevisionStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class RevisionSessionDto {

    private Long id;
    private Long topicId;
    private String topicName;
    private Long subjectId;
    private String subjectName;
    private Difficulty difficulty;
    private LocalDate scheduledDate;
    private int repetitionNumber;
    private int intervalDays;
    private double easinessFactor;
    private RevisionStatus status;
    private boolean overdue;
    private long daysOverdue;
    private LocalDateTime lastReviewedAt;
    private LocalDateTime createdAt;

    public RevisionSessionDto() {
    }

    public RevisionSessionDto(Long id, Long topicId, String topicName, Long subjectId, String subjectName,
                              Difficulty difficulty, LocalDate scheduledDate, int repetitionNumber,
                              int intervalDays, double easinessFactor, RevisionStatus status,
                              boolean overdue, long daysOverdue, LocalDateTime lastReviewedAt,
                              LocalDateTime createdAt) {
        this.id = id;
        this.topicId = topicId;
        this.topicName = topicName;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.difficulty = difficulty;
        this.scheduledDate = scheduledDate;
        this.repetitionNumber = repetitionNumber;
        this.intervalDays = intervalDays;
        this.easinessFactor = easinessFactor;
        this.status = status;
        this.overdue = overdue;
        this.daysOverdue = daysOverdue;
        this.lastReviewedAt = lastReviewedAt;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTopicId() {
        return topicId;
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
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

    public boolean isOverdue() {
        return overdue;
    }

    public void setOverdue(boolean overdue) {
        this.overdue = overdue;
    }

    public long getDaysOverdue() {
        return daysOverdue;
    }

    public void setDaysOverdue(long daysOverdue) {
        this.daysOverdue = daysOverdue;
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

