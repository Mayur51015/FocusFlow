package com.focusflow.dto;

/**
 * Response payload containing predicted study duration and model details.
 */
public class TopicPredictionResponse {

    private Long topicId;
    private String topicName;
    private int estimatedMinutes;
    private int predictedMinutes;
    private Double confidenceScore;
    private Integer estimatedErrorRangeMinutes;
    private String modelVersion;
    private String predictionSource; // "EXPERIMENTAL_ML" or "HEURISTIC_FALLBACK"
    private String message;

    public TopicPredictionResponse() {
    }

    public TopicPredictionResponse(Long topicId, String topicName, int estimatedMinutes, int predictedMinutes,
                                   Double confidenceScore, Integer estimatedErrorRangeMinutes, String modelVersion,
                                   String predictionSource, String message) {
        this.topicId = topicId;
        this.topicName = topicName;
        this.estimatedMinutes = estimatedMinutes;
        this.predictedMinutes = predictedMinutes;
        this.confidenceScore = confidenceScore;
        this.estimatedErrorRangeMinutes = estimatedErrorRangeMinutes;
        this.modelVersion = modelVersion;
        this.predictionSource = predictionSource;
        this.message = message;
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

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(int estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public int getPredictedMinutes() {
        return predictedMinutes;
    }

    public void setPredictedMinutes(int predictedMinutes) {
        this.predictedMinutes = predictedMinutes;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public Integer getEstimatedErrorRangeMinutes() {
        return estimatedErrorRangeMinutes;
    }

    public void setEstimatedErrorRangeMinutes(Integer estimatedErrorRangeMinutes) {
        this.estimatedErrorRangeMinutes = estimatedErrorRangeMinutes;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getPredictionSource() {
        return predictionSource;
    }

    public void setPredictionSource(String predictionSource) {
        this.predictionSource = predictionSource;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

