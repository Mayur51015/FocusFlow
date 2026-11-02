package com.focusflow.dto;

import java.util.List;
import java.util.Map;

public class MlReadinessResponse {
    private Long subjectId;
    private String subjectName;
    private double readinessScore;
    private String readinessLevel;
    private String predictionSource;
    private int daysUntilExam;
    private Map<String, Double> componentBreakdown;
    private List<String> recommendations;

    public MlReadinessResponse() {
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

    public double getReadinessScore() {
        return readinessScore;
    }

    public void setReadinessScore(double readinessScore) {
        this.readinessScore = readinessScore;
    }

    public String getReadinessLevel() {
        return readinessLevel;
    }

    public void setReadinessLevel(String readinessLevel) {
        this.readinessLevel = readinessLevel;
    }

    public String getPredictionSource() {
        return predictionSource;
    }

    public void setPredictionSource(String predictionSource) {
        this.predictionSource = predictionSource;
    }

    public int getDaysUntilExam() {
        return daysUntilExam;
    }

    public void setDaysUntilExam(int daysUntilExam) {
        this.daysUntilExam = daysUntilExam;
    }

    public Map<String, Double> getComponentBreakdown() {
        return componentBreakdown;
    }

    public void setComponentBreakdown(Map<String, Double> componentBreakdown) {
        this.componentBreakdown = componentBreakdown;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }
}

