package com.focusflow.dto;

import java.util.List;

public class LearningSummaryDto {
    private double overallMasteryScore;
    private String overallMasteryLevel;
    private int totalTopics;
    private int strongTopicsCount;
    private int moderateTopicsCount;
    private int weakTopicsCount;
    private double retentionRatePercentage;
    private int totalRevisionReviews;
    private int totalFlashcards;
    private int totalQuizAttempts;
    private double overallQuizAverage;
    private double overallReadinessScore;
    private String overallReadinessLevel;
    private List<DailyStudyFocusDto> recommendedStudyFocus;

    public LearningSummaryDto() {
    }

    public double getOverallMasteryScore() {
        return overallMasteryScore;
    }

    public void setOverallMasteryScore(double overallMasteryScore) {
        this.overallMasteryScore = overallMasteryScore;
    }

    public String getOverallMasteryLevel() {
        return overallMasteryLevel;
    }

    public void setOverallMasteryLevel(String overallMasteryLevel) {
        this.overallMasteryLevel = overallMasteryLevel;
    }

    public int getTotalTopics() {
        return totalTopics;
    }

    public void setTotalTopics(int totalTopics) {
        this.totalTopics = totalTopics;
    }

    public int getStrongTopicsCount() {
        return strongTopicsCount;
    }

    public void setStrongTopicsCount(int strongTopicsCount) {
        this.strongTopicsCount = strongTopicsCount;
    }

    public int getModerateTopicsCount() {
        return moderateTopicsCount;
    }

    public void setModerateTopicsCount(int moderateTopicsCount) {
        this.moderateTopicsCount = moderateTopicsCount;
    }

    public int getWeakTopicsCount() {
        return weakTopicsCount;
    }

    public void setWeakTopicsCount(int weakTopicsCount) {
        this.weakTopicsCount = weakTopicsCount;
    }

    public double getRetentionRatePercentage() {
        return retentionRatePercentage;
    }

    public void setRetentionRatePercentage(double retentionRatePercentage) {
        this.retentionRatePercentage = retentionRatePercentage;
    }

    public int getTotalRevisionReviews() {
        return totalRevisionReviews;
    }

    public void setTotalRevisionReviews(int totalRevisionReviews) {
        this.totalRevisionReviews = totalRevisionReviews;
    }

    public int getTotalFlashcards() {
        return totalFlashcards;
    }

    public void setTotalFlashcards(int totalFlashcards) {
        this.totalFlashcards = totalFlashcards;
    }

    public int getTotalQuizAttempts() {
        return totalQuizAttempts;
    }

    public void setTotalQuizAttempts(int totalQuizAttempts) {
        this.totalQuizAttempts = totalQuizAttempts;
    }

    public double getOverallQuizAverage() {
        return overallQuizAverage;
    }

    public void setOverallQuizAverage(double overallQuizAverage) {
        this.overallQuizAverage = overallQuizAverage;
    }

    public double getOverallReadinessScore() {
        return overallReadinessScore;
    }

    public void setOverallReadinessScore(double overallReadinessScore) {
        this.overallReadinessScore = overallReadinessScore;
    }

    public String getOverallReadinessLevel() {
        return overallReadinessLevel;
    }

    public void setOverallReadinessLevel(String overallReadinessLevel) {
        this.overallReadinessLevel = overallReadinessLevel;
    }

    public List<DailyStudyFocusDto> getRecommendedStudyFocus() {
        return recommendedStudyFocus;
    }

    public void setRecommendedStudyFocus(List<DailyStudyFocusDto> recommendedStudyFocus) {
        this.recommendedStudyFocus = recommendedStudyFocus;
    }
}

