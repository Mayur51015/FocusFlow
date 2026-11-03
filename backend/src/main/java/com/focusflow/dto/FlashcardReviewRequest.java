package com.focusflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.focusflow.entity.RecallRating;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FlashcardReviewRequest {

    @NotNull(message = "Rating is required (AGAIN, HARD, GOOD, EASY)")
    private RecallRating rating;

    private int timeSpentSeconds = 0;

    public FlashcardReviewRequest() {
    }

    public FlashcardReviewRequest(RecallRating rating, int timeSpentSeconds) {
        this.rating = rating;
        this.timeSpentSeconds = timeSpentSeconds;
    }

    public RecallRating getRating() {
        return rating;
    }

    public void setRating(RecallRating rating) {
        this.rating = rating;
    }

    public int getTimeSpentSeconds() {
        return timeSpentSeconds;
    }

    public void setTimeSpentSeconds(int timeSpentSeconds) {
        this.timeSpentSeconds = timeSpentSeconds;
    }
}

