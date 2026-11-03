package com.focusflow.dto;

import com.focusflow.entity.RecallRating;
import java.time.LocalDateTime;

public class RevisionHistoryDto {

    private Long id;
    private Long revisionSessionId;
    private Long topicId;
    private String topicName;
    private String subjectName;
    private RecallRating rating;
    private LocalDateTime reviewedAt;
    private int previousIntervalDays;
    private int newIntervalDays;
    private double previousEf;
    private double newEf;
    private String notes;

    public RevisionHistoryDto() {
    }

    public RevisionHistoryDto(Long id, Long revisionSessionId, Long topicId, String topicName,
                              String subjectName, RecallRating rating, LocalDateTime reviewedAt,
                              int previousIntervalDays, int newIntervalDays, double previousEf,
                              double newEf, String notes) {
        this.id = id;
        this.revisionSessionId = revisionSessionId;
        this.topicId = topicId;
        this.topicName = topicName;
        this.subjectName = subjectName;
        this.rating = rating;
        this.reviewedAt = reviewedAt;
        this.previousIntervalDays = previousIntervalDays;
        this.newIntervalDays = newIntervalDays;
        this.previousEf = previousEf;
        this.newEf = newEf;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRevisionSessionId() {
        return revisionSessionId;
    }

    public void setRevisionSessionId(Long revisionSessionId) {
        this.revisionSessionId = revisionSessionId;
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

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
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

