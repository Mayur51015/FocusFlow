package com.focusflow.dto;

import com.focusflow.entity.SessionStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Request payload for updating the status, duration, and timestamps of a study session.
 */
public class SessionStatusUpdateRequest {

    @NotNull(message = "Session status is required")
    private SessionStatus status;

    @Min(value = 0, message = "Actual minutes cannot be negative")
    private int actualMinutes;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime completedAt;

    public SessionStatusUpdateRequest() {
    }

    public SessionStatusUpdateRequest(SessionStatus status, int actualMinutes) {
        this.status = status;
        this.actualMinutes = actualMinutes;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public int getActualMinutes() {
        return actualMinutes;
    }

    public void setActualMinutes(int actualMinutes) {
        this.actualMinutes = actualMinutes;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
