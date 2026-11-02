package com.focusflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.focusflow.entity.RecallRating;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FlashcardCreateRequest {

    @NotNull(message = "Topic ID is required")
    private Long topicId;

    @NotBlank(message = "Front prompt cannot be empty")
    private String frontPrompt;

    @NotBlank(message = "Back answer cannot be empty")
    private String backAnswer;

    private String hint;

    public FlashcardCreateRequest() {
    }

    public FlashcardCreateRequest(Long topicId, String frontPrompt, String backAnswer, String hint) {
        this.topicId = topicId;
        this.frontPrompt = frontPrompt;
        this.backAnswer = backAnswer;
        this.hint = hint;
    }

    public Long getTopicId() {
        return topicId;
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
    }

    public String getFrontPrompt() {
        return frontPrompt;
    }

    public void setFrontPrompt(String frontPrompt) {
        this.frontPrompt = frontPrompt;
    }

    public String getBackAnswer() {
        return backAnswer;
    }

    public void setBackAnswer(String backAnswer) {
        this.backAnswer = backAnswer;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }
}

