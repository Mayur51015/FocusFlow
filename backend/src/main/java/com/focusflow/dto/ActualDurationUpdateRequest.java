package com.focusflow.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for updating only the actual duration of a study session.
 */
public class ActualDurationUpdateRequest {

    @NotNull(message = "Actual minutes is required")
    @Min(value = 0, message = "Actual minutes cannot be negative")
    private Integer actualMinutes;

    public ActualDurationUpdateRequest() {
    }

    public ActualDurationUpdateRequest(Integer actualMinutes) {
        this.actualMinutes = actualMinutes;
    }

    public Integer getActualMinutes() {
        return actualMinutes;
    }

    public void setActualMinutes(Integer actualMinutes) {
        this.actualMinutes = actualMinutes;
    }
}

