package com.focusflow.dto;

import com.focusflow.entity.Difficulty;
import com.focusflow.entity.PerformanceLevel;
import java.time.LocalDateTime;

public class TopicPerformanceDto {

    private Long id;
    private Long topicId;
    private String topicName;
    private Long subjectId;
    private String subjectName;
    private Difficulty difficulty;
    private PerformanceLevel performanceLevel;
    private double averageQuizScore;
    private int attemptCount;
    private double accuracyPercentage;
    private int totalStudyMinutes;
    private String predictionSource;
    private double confidenceScore;
    private String recommendation;
    private LocalDateTime lastEvaluatedAt;

    public TopicPerformanceDto() {
    }

    public TopicPerformanceDto(Long id, Long topicId, String topicName, Long subjectId, String subjectName,
                               Difficulty difficulty, PerformanceLevel performanceLevel, double averageQuizScore,
                               int attemptCount, double accuracyPercentage, int totalStudyMinutes,
                               String predictionSource, double confidenceScore, String recommendation,
                               LocalDateTime lastEvaluatedAt) {
        this.id = id;
        this.topicId = topicId;
        this.topicName = topicName;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.difficulty = difficulty;
        this.performanceLevel = performanceLevel;
        this.averageQuizScore = averageQuizScore;
        this.attemptCount = attemptCount;
        this.accuracyPercentage = accuracyPercentage;
        this.totalStudyMinutes = totalStudyMinutes;
        this.predictionSource = predictionSource;
        this.confidenceScore = confidenceScore;
        this.recommendation = recommendation;
        this.lastEvaluatedAt = lastEvaluatedAt;
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

    public PerformanceLevel getPerformanceLevel() {
        return performanceLevel;
    }

    public void setPerformanceLevel(PerformanceLevel performanceLevel) {
        this.performanceLevel = performanceLevel;
    }

    public double getAverageQuizScore() {
        return averageQuizScore;
    }

    public void setAverageQuizScore(double averageQuizScore) {
        this.averageQuizScore = averageQuizScore;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public double getAccuracyPercentage() {
        return accuracyPercentage;
    }

    public void setAccuracyPercentage(double accuracyPercentage) {
        this.accuracyPercentage = accuracyPercentage;
    }

    public int getTotalStudyMinutes() {
        return totalStudyMinutes;
    }

    public void setTotalStudyMinutes(int totalStudyMinutes) {
        this.totalStudyMinutes = totalStudyMinutes;
    }

    public String getPredictionSource() {
        return predictionSource;
    }

    public void setPredictionSource(String predictionSource) {
        this.predictionSource = predictionSource;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public LocalDateTime getLastEvaluatedAt() {
        return lastEvaluatedAt;
    }

    public void setLastEvaluatedAt(LocalDateTime lastEvaluatedAt) {
        this.lastEvaluatedAt = lastEvaluatedAt;
    }
}

