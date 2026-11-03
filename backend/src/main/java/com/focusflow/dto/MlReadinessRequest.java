package com.focusflow.dto;

public class MlReadinessRequest {
    private Long subjectId;
    private String subjectName;
    private double topicCompletionRate;
    private double averageQuizScore;
    private double weakTopicRatio;
    private double revisionCompletionRate;
    private int daysUntilExam;
    private String importance;
    private Double recentQuizTrend;

    public MlReadinessRequest() {
    }

    public MlReadinessRequest(Long subjectId, String subjectName, double topicCompletionRate,
                              double averageQuizScore, double weakTopicRatio,
                              double revisionCompletionRate, int daysUntilExam,
                              String importance, Double recentQuizTrend) {
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.topicCompletionRate = topicCompletionRate;
        this.averageQuizScore = averageQuizScore;
        this.weakTopicRatio = weakTopicRatio;
        this.revisionCompletionRate = revisionCompletionRate;
        this.daysUntilExam = daysUntilExam;
        this.importance = importance;
        this.recentQuizTrend = recentQuizTrend;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public double getTopicCompletionRate() {
        return topicCompletionRate;
    }

    public void setTopicCompletionRate(double topicCompletionRate) {
        this.topicCompletionRate = topicCompletionRate;
    }

    public double getAverageQuizScore() {
        return averageQuizScore;
    }

    public void setAverageQuizScore(double averageQuizScore) {
        this.averageQuizScore = averageQuizScore;
    }

    public double getWeakTopicRatio() {
        return weakTopicRatio;
    }

    public void setWeakTopicRatio(double weakTopicRatio) {
        this.weakTopicRatio = weakTopicRatio;
    }

    public double getRevisionCompletionRate() {
        return revisionCompletionRate;
    }

    public void setRevisionCompletionRate(double revisionCompletionRate) {
        this.revisionCompletionRate = revisionCompletionRate;
    }

    public int getDaysUntilExam() {
        return daysUntilExam;
    }

    public void setDaysUntilExam(int daysUntilExam) {
        this.daysUntilExam = daysUntilExam;
    }

    public String getImportance() {
        return importance;
    }

    public void setImportance(String importance) {
        this.importance = importance;
    }

    public Double getRecentQuizTrend() {
        return recentQuizTrend;
    }

    public void setRecentQuizTrend(Double recentQuizTrend) {
        this.recentQuizTrend = recentQuizTrend;
    }
}

