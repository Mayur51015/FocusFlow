package com.focusflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.focusflow.entity.RecallRating;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RevisionReviewRequest {

    @NotNull(message = "Recall rating is required (AGAIN, HARD, GOOD, EASY)")
    private RecallRating rating;

    private String notes;

    public RevisionReviewRequest() {
    }

    public RevisionReviewRequest(RecallRating rating, String notes) {
        this.rating = rating;
        this.notes = notes;
    }

    public RecallRating getRating() {
        return rating;
    }

    public void setRating(RecallRating rating) {
        this.rating = rating;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

