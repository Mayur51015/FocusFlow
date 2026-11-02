package com.focusflow.dto;

import java.util.List;

public class RetentionTrendsDto {
    private int totalReviews;
    private int againCount;
    private int hardCount;
    private int goodCount;
    private int easyCount;
    private double retentionRatePercentage;
    private double averageEasinessFactor;
    private List<RevisionHistoryDto> recentReviews;
    private List<QuizScoreTrendDto> historicalQuizScores;

    public RetentionTrendsDto() {
    }

    public int getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(int totalReviews) {
        this.totalReviews = totalReviews;
    }

    public int getAgainCount() {
        return againCount;
    }

    public void setAgainCount(int againCount) {
        this.againCount = againCount;
    }

    public int getHardCount() {
        return hardCount;
    }

    public void setHardCount(int hardCount) {
        this.hardCount = hardCount;
    }

    public int getGoodCount() {
        return goodCount;
    }

    public void setGoodCount(int goodCount) {
        this.goodCount = goodCount;
    }

    public int getEasyCount() {
        return easyCount;
    }

    public void setEasyCount(int easyCount) {
        this.easyCount = easyCount;
    }

    public double getRetentionRatePercentage() {
        return retentionRatePercentage;
    }

    public void setRetentionRatePercentage(double retentionRatePercentage) {
        this.retentionRatePercentage = retentionRatePercentage;
    }

    public double getAverageEasinessFactor() {
        return averageEasinessFactor;
    }

    public void setAverageEasinessFactor(double averageEasinessFactor) {
        this.averageEasinessFactor = averageEasinessFactor;
    }

    public List<RevisionHistoryDto> getRecentReviews() {
        return recentReviews;
    }

    public void setRecentReviews(List<RevisionHistoryDto> recentReviews) {
        this.recentReviews = recentReviews;
    }

    public List<QuizScoreTrendDto> getHistoricalQuizScores() {
        return historicalQuizScores;
    }

    public void setHistoricalQuizScores(List<QuizScoreTrendDto> historicalQuizScores) {
        this.historicalQuizScores = historicalQuizScores;
    }
}

