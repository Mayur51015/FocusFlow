package com.focusflow.dto;

/**
 * Payload sent to the FastAPI ML Microservice /ml/predict-duration endpoint.
 */
public class FastApiPredictionRequest {

    private String topicDifficulty;
    private int estimatedMinutes;
    private Double historicalAverageMinutes;
    private int previousStudySessions;
    private String subjectCategory;

    public FastApiPredictionRequest() {
    }

    public FastApiPredictionRequest(String topicDifficulty, int estimatedMinutes, Double historicalAverageMinutes,
                                   int previousStudySessions, String subjectCategory) {
        this.topicDifficulty = topicDifficulty;
        this.estimatedMinutes = estimatedMinutes;
        this.historicalAverageMinutes = historicalAverageMinutes;
        this.previousStudySessions = previousStudySessions;
        this.subjectCategory = subjectCategory;
    }

    public String getTopicDifficulty() {
        return topicDifficulty;
    }

    public void setTopicDifficulty(String topicDifficulty) {
        this.topicDifficulty = topicDifficulty;
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(int estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public Double getHistoricalAverageMinutes() {
        return historicalAverageMinutes;
    }

    public void setHistoricalAverageMinutes(Double historicalAverageMinutes) {
        this.historicalAverageMinutes = historicalAverageMinutes;
    }

    public int getPreviousStudySessions() {
        return previousStudySessions;
    }

    public void setPreviousStudySessions(int previousStudySessions) {
        this.previousStudySessions = previousStudySessions;
    }

    public String getSubjectCategory() {
        return subjectCategory;
    }

    public void setSubjectCategory(String subjectCategory) {
        this.subjectCategory = subjectCategory;
    }
}

