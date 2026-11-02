package com.focusflow.dto;

import java.time.LocalDateTime;

public class TopicQuizPerformanceDto {

    private Long topicId;
    private String topicName;
    private String subjectName;
    private long totalQuestionsAttempted;
    private long correctAnswersCount;
    private double accuracyPercentage;
    private long attemptCount;
    private LocalDateTime lastAttemptDate;

    public TopicQuizPerformanceDto() {
    }

    public TopicQuizPerformanceDto(Long topicId, String topicName, String subjectName,
                                   long totalQuestionsAttempted, long correctAnswersCount,
                                   double accuracyPercentage, long attemptCount,
                                   LocalDateTime lastAttemptDate) {
        this.topicId = topicId;
        this.topicName = topicName;
        this.subjectName = subjectName;
        this.totalQuestionsAttempted = totalQuestionsAttempted;
        this.correctAnswersCount = correctAnswersCount;
        this.accuracyPercentage = accuracyPercentage;
        this.attemptCount = attemptCount;
        this.lastAttemptDate = lastAttemptDate;
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

    public long getTotalQuestionsAttempted() {
        return totalQuestionsAttempted;
    }

    public void setTotalQuestionsAttempted(long totalQuestionsAttempted) {
        this.totalQuestionsAttempted = totalQuestionsAttempted;
    }

    public long getCorrectAnswersCount() {
        return correctAnswersCount;
    }

    public void setCorrectAnswersCount(long correctAnswersCount) {
        this.correctAnswersCount = correctAnswersCount;
    }

    public double getAccuracyPercentage() {
        return accuracyPercentage;
    }

    public void setAccuracyPercentage(double accuracyPercentage) {
        this.accuracyPercentage = accuracyPercentage;
    }

    public long getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(long attemptCount) {
        this.attemptCount = attemptCount;
    }

    public LocalDateTime getLastAttemptDate() {
        return lastAttemptDate;
    }

    public void setLastAttemptDate(LocalDateTime lastAttemptDate) {
        this.lastAttemptDate = lastAttemptDate;
    }
}

