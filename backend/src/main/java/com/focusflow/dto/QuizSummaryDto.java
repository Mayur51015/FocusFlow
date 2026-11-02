package com.focusflow.dto;

import java.time.LocalDateTime;

public class QuizSummaryDto {

    private Long id;
    private String title;
    private String description;
    private Long subjectId;
    private String subjectName;
    private Long topicId;
    private String topicName;
    private int durationMinutes;
    private int totalMarks;
    private int passingMarks;
    private int questionCount;
    private long attemptCount;
    private Double bestScorePercentage;
    private LocalDateTime createdAt;

    public QuizSummaryDto() {
    }

    public QuizSummaryDto(Long id, String title, String description, Long subjectId, String subjectName,
                          Long topicId, String topicName, int durationMinutes, int totalMarks, int passingMarks,
                          int questionCount, long attemptCount, Double bestScorePercentage, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.topicId = topicId;
        this.topicName = topicName;
        this.durationMinutes = durationMinutes;
        this.totalMarks = totalMarks;
        this.passingMarks = passingMarks;
        this.questionCount = questionCount;
        this.attemptCount = attemptCount;
        this.bestScorePercentage = bestScorePercentage;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public int getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(int totalMarks) {
        this.totalMarks = totalMarks;
    }

    public int getPassingMarks() {
        return passingMarks;
    }

    public void setPassingMarks(int passingMarks) {
        this.passingMarks = passingMarks;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
    }

    public long getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(long attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Double getBestScorePercentage() {
        return bestScorePercentage;
    }

    public void setBestScorePercentage(Double bestScorePercentage) {
        this.bestScorePercentage = bestScorePercentage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

