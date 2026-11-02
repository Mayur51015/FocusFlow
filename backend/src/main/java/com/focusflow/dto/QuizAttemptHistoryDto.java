package com.focusflow.dto;

import java.time.LocalDateTime;

public class QuizAttemptHistoryDto {

    private Long attemptId;
    private Long quizId;
    private String quizTitle;
    private String subjectName;
    private String topicName;
    private int score;
    private int totalMarks;
    private double percentage;
    private boolean passed;
    private LocalDateTime completedAt;
    private int timeSpentSeconds;

    public QuizAttemptHistoryDto() {
    }

    public QuizAttemptHistoryDto(Long attemptId, Long quizId, String quizTitle, String subjectName, String topicName,
                                 int score, int totalMarks, double percentage, boolean passed,
                                 LocalDateTime completedAt, int timeSpentSeconds) {
        this.attemptId = attemptId;
        this.quizId = quizId;
        this.quizTitle = quizTitle;
        this.subjectName = subjectName;
        this.topicName = topicName;
        this.score = score;
        this.totalMarks = totalMarks;
        this.percentage = percentage;
        this.passed = passed;
        this.completedAt = completedAt;
        this.timeSpentSeconds = timeSpentSeconds;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public Long getQuizId() {
        return quizId;
    }

    public void setQuizId(Long quizId) {
        this.quizId = quizId;
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

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(int totalMarks) {
        this.totalMarks = totalMarks;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public int getTimeSpentSeconds() {
        return timeSpentSeconds;
    }

    public void setTimeSpentSeconds(int timeSpentSeconds) {
        this.timeSpentSeconds = timeSpentSeconds;
    }
}

