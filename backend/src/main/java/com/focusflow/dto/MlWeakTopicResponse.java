package com.focusflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MlWeakTopicResponse {

    private Long topicId;
    private String performanceLevel;
    private double confidence;
    private String predictionSource;
    private Map<String, Double> probabilities;
    private String explanation;

    public MlWeakTopicResponse() {
    }

    public MlWeakTopicResponse(Long topicId, String performanceLevel, double confidence,
                               String predictionSource, Map<String, Double> probabilities,
                               String explanation) {
        this.topicId = topicId;
        this.performanceLevel = performanceLevel;
        this.confidence = confidence;
        this.predictionSource = predictionSource;
        this.probabilities = probabilities;
        this.explanation = explanation;
    }

    public Long getTopicId() {
        return topicId;
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
    }

    public String getPerformanceLevel() {
        return performanceLevel;
    }

    public void setPerformanceLevel(String performanceLevel) {
        this.performanceLevel = performanceLevel;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getPredictionSource() {
        return predictionSource;
    }

    public void setPredictionSource(String predictionSource) {
        this.predictionSource = predictionSource;
    }

    public Map<String, Double> getProbabilities() {
        return probabilities;
    }

    public void setProbabilities(Map<String, Double> probabilities) {
        this.probabilities = probabilities;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}

