package com.focusflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Persisted Exam Readiness Evaluation record for a subject.
 * Stores readiness score, classification tier, component metrics,
 * ML vs Rule-based source, and personalized recommendations.
 */
@Entity
@Table(name = "exam_readiness")
public class ExamReadiness {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Subject subject;

    @Column(name = "readiness_score", nullable = false)
    private Double readinessScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "readiness_level", nullable = false, length = 20)
    private ReadinessLevel readinessLevel;

    @Column(name = "topic_completion_rate", nullable = false)
    private Double topicCompletionRate;

    @Column(name = "average_quiz_score", nullable = false)
    private Double averageQuizScore;

    @Column(name = "weak_topic_count", nullable = false)
    private Integer weakTopicCount;

    @Column(name = "weak_topic_ratio", nullable = false)
    private Double weakTopicRatio;

    @Column(name = "revision_completion_rate", nullable = false)
    private Double revisionCompletionRate;

    @Column(name = "days_until_exam", nullable = false)
    private Integer daysUntilExam;

    @Column(name = "prediction_source", nullable = false, length = 50)
    private String predictionSource;

    @Column(name = "recommendations", columnDefinition = "TEXT")
    private String recommendations;

    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt;

    public ExamReadiness() {
    }

    public ExamReadiness(Subject subject, Double readinessScore, ReadinessLevel readinessLevel,
                         Double topicCompletionRate, Double averageQuizScore,
                         Integer weakTopicCount, Double weakTopicRatio,
                         Double revisionCompletionRate, Integer daysUntilExam,
                         String predictionSource, String recommendations,
                         LocalDateTime evaluatedAt) {
        this.subject = subject;
        this.readinessScore = readinessScore;
        this.readinessLevel = readinessLevel;
        this.topicCompletionRate = topicCompletionRate;
        this.averageQuizScore = averageQuizScore;
        this.weakTopicCount = weakTopicCount;
        this.weakTopicRatio = weakTopicRatio;
        this.revisionCompletionRate = revisionCompletionRate;
        this.daysUntilExam = daysUntilExam;
        this.predictionSource = predictionSource;
        this.recommendations = recommendations;
        this.evaluatedAt = evaluatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Double getReadinessScore() {
        return readinessScore;
    }

    public void setReadinessScore(Double readinessScore) {
        this.readinessScore = readinessScore;
    }

    public ReadinessLevel getReadinessLevel() {
        return readinessLevel;
    }

    public void setReadinessLevel(ReadinessLevel readinessLevel) {
        this.readinessLevel = readinessLevel;
    }

    public Double getTopicCompletionRate() {
        return topicCompletionRate;
    }

    public void setTopicCompletionRate(Double topicCompletionRate) {
        this.topicCompletionRate = topicCompletionRate;
    }

    public Double getAverageQuizScore() {
        return averageQuizScore;
    }

    public void setAverageQuizScore(Double averageQuizScore) {
        this.averageQuizScore = averageQuizScore;
    }

    public Integer getWeakTopicCount() {
        return weakTopicCount;
    }

    public void setWeakTopicCount(Integer weakTopicCount) {
        this.weakTopicCount = weakTopicCount;
    }

    public Double getWeakTopicRatio() {
        return weakTopicRatio;
    }

    public void setWeakTopicRatio(Double weakTopicRatio) {
        this.weakTopicRatio = weakTopicRatio;
    }

    public Double getRevisionCompletionRate() {
        return revisionCompletionRate;
    }

    public void setRevisionCompletionRate(Double revisionCompletionRate) {
        this.revisionCompletionRate = revisionCompletionRate;
    }

    public Integer getDaysUntilExam() {
        return daysUntilExam;
    }

    public void setDaysUntilExam(Integer daysUntilExam) {
        this.daysUntilExam = daysUntilExam;
    }

    public String getPredictionSource() {
        return predictionSource;
    }

    public void setPredictionSource(String predictionSource) {
        this.predictionSource = predictionSource;
    }

    public String getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(String recommendations) {
        this.recommendations = recommendations;
    }

    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(LocalDateTime evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
}

