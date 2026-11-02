package com.focusflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Payload received from the FastAPI ML Microservice /ml/predict-duration endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FastApiPredictionResponse {

    private int predictedMinutes;
    private Double confidenceScore;
    private Integer estimatedErrorRangeMinutes;
    private String modelVersion;
    private String predictionSource;
    private String details;

    public FastApiPredictionResponse() {
    }

    public FastApiPredictionResponse(int predictedMinutes, Double confidenceScore,
                                    Integer estimatedErrorRangeMinutes, String modelVersion,
                                    String predictionSource, String details) {
        this.predictedMinutes = predictedMinutes;
        this.confidenceScore = confidenceScore;
        this.estimatedErrorRangeMinutes = estimatedErrorRangeMinutes;
        this.modelVersion = modelVersion;
        this.predictionSource = predictionSource;
        this.details = details;
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

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}

