package com.focusflow.dto;

import jakarta.validation.constraints.DecimalMin;
import java.time.LocalDate;

/**
 * Request payload for adaptive schedule recalculation.
 */
public class RecalculateScheduleRequest {

    private LocalDate date;

    @DecimalMin(value = "0.25", message = "Available hours must be at least 15 minutes (0.25 hours) if provided")
    private Double availableHours;

    public RecalculateScheduleRequest() {
    }

    public RecalculateScheduleRequest(LocalDate date, Double availableHours) {
        this.date = date;
        this.availableHours = availableHours;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Double getAvailableHours() {
        return availableHours;
    }

    public void setAvailableHours(Double availableHours) {
        this.availableHours = availableHours;
    }
}

