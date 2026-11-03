package com.focusflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * TopicPerformance entity stores aggregated understanding metrics and ML/rule-based
 * mastery classifications for individual topics.
 */
@Entity
@Table(name = "topic_performances")
public class TopicPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Topic reference is required")
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "topic_id", nullable = false, unique = true)
    @JsonIgnoreProperties("subject")
    private Topic topic;

    @NotNull(message = "Performance level is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "performance_level", nullable = false, length = 20)
    private PerformanceLevel performanceLevel = PerformanceLevel.MODERATE;

    @Column(name = "average_quiz_score", nullable = false)
    private double averageQuizScore = 0.0;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "accuracy_percentage", nullable = false)
    private double accuracyPercentage = 0.0;

    @Column(name = "total_study_minutes", nullable = false)
    private int totalStudyMinutes = 0;

    @Column(name = "prediction_source", nullable = false, length = 50)
    private String predictionSource = "RULE_BASED";

    @Column(name = "confidence_score", nullable = false)
    private double confidenceScore = 1.0;

    @Column(name = "recommendation", columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "last_evaluated_at", nullable = false)
    private LocalDateTime lastEvaluatedAt;

    public TopicPerformance() {
    }

    public TopicPerformance(Topic topic, PerformanceLevel performanceLevel, double averageQuizScore,
                            int attemptCount, double accuracyPercentage, int totalStudyMinutes,
                            String predictionSource, double confidenceScore, String recommendation,
                            LocalDateTime lastEvaluatedAt) {
        this.topic = topic;
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

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
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

