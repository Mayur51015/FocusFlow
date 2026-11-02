package com.focusflow.entity;

/**
 * Status of a scheduled study session.
 * Enhanced for Phase 1 session tracking and adaptive scheduling.
 */
public enum SessionStatus {
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    MISSED,
    PARTIALLY_COMPLETED,
    RESCHEDULED,
    INCOMPLETE
}
