package com.focusflow.entity;

/**
 * RecallRating represents the student's self-assessed recall performance
 * during an SM-2 spaced repetition review.
 */
public enum RecallRating {
    AGAIN, // Complete blackout / failed recall
    HARD,  // Recalled with significant difficulty
    GOOD,  // Correct response with normal hesitation
    EASY   // Instant, effortless recall
}

