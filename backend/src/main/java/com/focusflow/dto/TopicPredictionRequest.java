package com.focusflow.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload to predict study duration for a specific topic.
 */
public class TopicPredictionRequest {

    @NotNull(message = "Topic ID is required")
    private Long topicId;

    public TopicPredictionRequest() {
    }

    public TopicPredictionRequest(Long topicId) {
        this.topicId = topicId;
    }

    public Long getTopicId() {
        return topicId;
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
    }
}

