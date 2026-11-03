package com.focusflow.dto;

import java.time.LocalDate;

public class SubjectMasteryDto {
    private Long subjectId;
    private String subjectName;
    private String importance;
    private LocalDate examDate;
    private int daysUntilExam;
    private int totalTopics;
    private int completedTopics;
    private double topicCompletionRate;
    private int strongTopicsCount;
    private int moderateTopicsCount;
    private int weakTopicsCount;
    private double averageQuizScore;
    private int quizAttemptsCount;
    private double readinessScore;
    private String readinessLevel;
    private String overallMasteryLevel;

    public SubjectMasteryDto() {
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

    public String getImportance() {
        return importance;
    }

    public void setImportance(String importance) {
        this.importance = importance;
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public void setExamDate(LocalDate examDate) {
        this.examDate = examDate;
    }

    public int getDaysUntilExam() {
        return daysUntilExam;
    }

    public void setDaysUntilExam(int daysUntilExam) {
        this.daysUntilExam = daysUntilExam;
    }

    public int getTotalTopics() {
        return totalTopics;
    }

    public void setTotalTopics(int totalTopics) {
        this.totalTopics = totalTopics;
    }

    public int getCompletedTopics() {
        return completedTopics;
    }

    public void setCompletedTopics(int completedTopics) {
        this.completedTopics = completedTopics;
    }

    public double getTopicCompletionRate() {
        return topicCompletionRate;
    }

    public void setTopicCompletionRate(double topicCompletionRate) {
        this.topicCompletionRate = topicCompletionRate;
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

    public double getAverageQuizScore() {
        return averageQuizScore;
    }

    public void setAverageQuizScore(double averageQuizScore) {
        this.averageQuizScore = averageQuizScore;
    }

    public int getQuizAttemptsCount() {
        return quizAttemptsCount;
    }

    public void setQuizAttemptsCount(int quizAttemptsCount) {
        this.quizAttemptsCount = quizAttemptsCount;
    }

    public double getReadinessScore() {
        return readinessScore;
    }

    public void setReadinessScore(double readinessScore) {
        this.readinessScore = readinessScore;
    }

    public String getReadinessLevel() {
        return readinessLevel;
    }

    public void setReadinessLevel(String readinessLevel) {
        this.readinessLevel = readinessLevel;
    }

    public String getOverallMasteryLevel() {
        return overallMasteryLevel;
    }

    public void setOverallMasteryLevel(String overallMasteryLevel) {
        this.overallMasteryLevel = overallMasteryLevel;
    }
}

