package com.focusflow.dto;

import com.focusflow.entity.Importance;
import com.focusflow.entity.ReadinessLevel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ExamReadinessDto {
    private Long id;
    private Long subjectId;
    private String subjectName;
    private Importance importance;
    private LocalDate examDate;
    private int daysUntilExam;
    private double readinessScore;
    private ReadinessLevel readinessLevel;
    private double topicCompletionRate;
    private double averageQuizScore;
    private int weakTopicCount;
    private double weakTopicRatio;
    private double revisionCompletionRate;
    private String predictionSource;
    private List<String> recommendations;
    private LocalDateTime evaluatedAt;

    public ExamReadinessDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Importance getImportance() {
        return importance;
    }

    public void setImportance(Importance importance) {
        this.importance = importance;
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public void setExamDate(LocalDate examDate) {
        this.examDate = examDate;
    }

    public int getDaysUntilExam() {
        return daysUntilExam;
    }

    public void setDaysUntilExam(int daysUntilExam) {
        this.daysUntilExam = daysUntilExam;
    }

    public double getReadinessScore() {
        return readinessScore;
    }

    public void setReadinessScore(double readinessScore) {
        this.readinessScore = readinessScore;
    }

    public ReadinessLevel getReadinessLevel() {
        return readinessLevel;
    }

    public void setReadinessLevel(ReadinessLevel readinessLevel) {
        this.readinessLevel = readinessLevel;
    }

    public double getTopicCompletionRate() {
        return topicCompletionRate;
    }

    public void setTopicCompletionRate(double topicCompletionRate) {
        this.topicCompletionRate = topicCompletionRate;
    }

    public double getAverageQuizScore() {
        return averageQuizScore;
    }

    public void setAverageQuizScore(double averageQuizScore) {
        this.averageQuizScore = averageQuizScore;
    }

    public int getWeakTopicCount() {
        return weakTopicCount;
    }

    public void setWeakTopicCount(int weakTopicCount) {
        this.weakTopicCount = weakTopicCount;
    }

    public double getWeakTopicRatio() {
        return weakTopicRatio;
    }

    public void setWeakTopicRatio(double weakTopicRatio) {
        this.weakTopicRatio = weakTopicRatio;
    }

    public double getRevisionCompletionRate() {
        return revisionCompletionRate;
    }

    public void setRevisionCompletionRate(double revisionCompletionRate) {
        this.revisionCompletionRate = revisionCompletionRate;
    }

    public String getPredictionSource() {
        return predictionSource;
    }

    public void setPredictionSource(String predictionSource) {
        this.predictionSource = predictionSource;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(LocalDateTime evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
}

