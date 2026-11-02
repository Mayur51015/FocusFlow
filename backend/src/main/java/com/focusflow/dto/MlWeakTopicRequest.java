package com.focusflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MlWeakTopicRequest {

    private Long topicId;
    private String topicDifficulty;
    private double averageQuizScore;
    private int attemptCount;
    private int revisionCount;
    private int recentStudyMinutes;
    private Double lastQuizScore;

    public MlWeakTopicRequest() {
    }

    public MlWeakTopicRequest(Long topicId, String topicDifficulty, double averageQuizScore,
                              int attemptCount, int revisionCount, int recentStudyMinutes,
                              Double lastQuizScore) {
        this.topicId = topicId;
        this.topicDifficulty = topicDifficulty;
        this.averageQuizScore = averageQuizScore;
        this.attemptCount = attemptCount;
        this.revisionCount = revisionCount;
        this.recentStudyMinutes = recentStudyMinutes;
        this.lastQuizScore = lastQuizScore;
    }

    public Long getTopicId() {
        return topicId;
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
    }

    public String getTopicDifficulty() {
        return topicDifficulty;
    }

    public void setTopicDifficulty(String topicDifficulty) {
        this.topicDifficulty = topicDifficulty;
    }

    public double getAverageQuizScore() {
        return averageQuizScore;
    }

    public void setAverageQuizScore(double averageQuizScore) {
        this.averageQuizScore = averageQuizScore;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public int getRevisionCount() {
        return revisionCount;
    }

    public void setRevisionCount(int revisionCount) {
        this.revisionCount = revisionCount;
    }

    public int getRecentStudyMinutes() {
        return recentStudyMinutes;
    }

    public void setRecentStudyMinutes(int recentStudyMinutes) {
        this.recentStudyMinutes = recentStudyMinutes;
    }

    public Double getLastQuizScore() {
        return lastQuizScore;
    }

    public void setLastQuizScore(Double lastQuizScore) {
        this.lastQuizScore = lastQuizScore;
    }
}

