package com.focusflow.dto;

import java.time.LocalDateTime;

public class QuizScoreTrendDto {
    private Long attemptId;
    private String quizTitle;
    private String subjectName;
    private double scorePercentage;
    private boolean passed;
    private LocalDateTime attemptedAt;

    public QuizScoreTrendDto() {
    }

    public QuizScoreTrendDto(Long attemptId, String quizTitle, String subjectName,
                             double scorePercentage, boolean passed, LocalDateTime attemptedAt) {
        this.attemptId = attemptId;
        this.quizTitle = quizTitle;
        this.subjectName = subjectName;
        this.scorePercentage = scorePercentage;
        this.passed = passed;
        this.attemptedAt = attemptedAt;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public String getQuizTitle() {
        return quizTitle;
    }

    public void setQuizTitle(String quizTitle) {
        this.quizTitle = quizTitle;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public double getScorePercentage() {
        return scorePercentage;
    }

    public void setScorePercentage(double scorePercentage) {
        this.scorePercentage = scorePercentage;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public LocalDateTime getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(LocalDateTime attemptedAt) {
        this.attemptedAt = attemptedAt;
    }
}

