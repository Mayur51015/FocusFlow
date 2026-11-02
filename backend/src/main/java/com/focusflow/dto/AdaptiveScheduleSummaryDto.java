package com.focusflow.dto;

import com.focusflow.entity.StudySession;
import java.util.List;

/**
 * Summary DTO returned after adaptive schedule recalculation.
 */
public class AdaptiveScheduleSummaryDto {

    private List<StudySession> sessions;
    private int totalCapacityMinutes;
    private int committedMinutes;
    private int remainingCapacityMinutes;
    private int completedSessionsCount;
    private int partiallyCompletedCount;
    private int missedSessionsCount;
    private int rescheduledSessionsCount;
    private int lockedSessionsCount;
    private String message;

    public AdaptiveScheduleSummaryDto() {
    }

    public AdaptiveScheduleSummaryDto(List<StudySession> sessions, int totalCapacityMinutes, int committedMinutes,
                                      int remainingCapacityMinutes, int completedSessionsCount,
                                      int partiallyCompletedCount, int missedSessionsCount,
                                      int rescheduledSessionsCount, int lockedSessionsCount, String message) {
        this.sessions = sessions;
        this.totalCapacityMinutes = totalCapacityMinutes;
        this.committedMinutes = committedMinutes;
        this.remainingCapacityMinutes = remainingCapacityMinutes;
        this.completedSessionsCount = completedSessionsCount;
        this.partiallyCompletedCount = partiallyCompletedCount;
        this.missedSessionsCount = missedSessionsCount;
        this.rescheduledSessionsCount = rescheduledSessionsCount;
        this.lockedSessionsCount = lockedSessionsCount;
        this.message = message;
    }

    public List<StudySession> getSessions() {
        return sessions;
    }

    public void setSessions(List<StudySession> sessions) {
        this.sessions = sessions;
    }

    public int getTotalCapacityMinutes() {
        return totalCapacityMinutes;
    }

    public void setTotalCapacityMinutes(int totalCapacityMinutes) {
        this.totalCapacityMinutes = totalCapacityMinutes;
    }

    public int getCommittedMinutes() {
        return committedMinutes;
    }

    public void setCommittedMinutes(int committedMinutes) {
        this.committedMinutes = committedMinutes;
    }

    public int getRemainingCapacityMinutes() {
        return remainingCapacityMinutes;
    }

    public void setRemainingCapacityMinutes(int remainingCapacityMinutes) {
        this.remainingCapacityMinutes = remainingCapacityMinutes;
    }

    public int getCompletedSessionsCount() {
        return completedSessionsCount;
    }

    public void setCompletedSessionsCount(int completedSessionsCount) {
        this.completedSessionsCount = completedSessionsCount;
    }

    public int getPartiallyCompletedCount() {
        return partiallyCompletedCount;
    }

    public void setPartiallyCompletedCount(int partiallyCompletedCount) {
        this.partiallyCompletedCount = partiallyCompletedCount;
    }

    public int getMissedSessionsCount() {
        return missedSessionsCount;
    }

    public void setMissedSessionsCount(int missedSessionsCount) {
        this.missedSessionsCount = missedSessionsCount;
    }

    public int getRescheduledSessionsCount() {
        return rescheduledSessionsCount;
    }

    public void setRescheduledSessionsCount(int rescheduledSessionsCount) {
        this.rescheduledSessionsCount = rescheduledSessionsCount;
    }

    public int getLockedSessionsCount() {
        return lockedSessionsCount;
    }

    public void setLockedSessionsCount(int lockedSessionsCount) {
        this.lockedSessionsCount = lockedSessionsCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

