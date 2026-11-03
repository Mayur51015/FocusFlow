package com.focusflow.dto;

import com.focusflow.entity.ReadinessLevel;

import java.util.List;

public class ExamReadinessOverviewDto {
    private double overallReadinessScore;
    private ReadinessLevel overallReadinessLevel;
    private int totalSubjects;
    private int highReadinessCount;
    private int moderateReadinessCount;
    private int lowReadinessCount;
    private int urgentExamsCount; // exams within 7 days
    private List<ExamReadinessDto> subjectEvaluations;

    public ExamReadinessOverviewDto() {
    }

    public double getOverallReadinessScore() {
        return overallReadinessScore;
    }

    public void setOverallReadinessScore(double overallReadinessScore) {
        this.overallReadinessScore = overallReadinessScore;
    }

    public ReadinessLevel getOverallReadinessLevel() {
        return overallReadinessLevel;
    }

    public void setOverallReadinessLevel(ReadinessLevel overallReadinessLevel) {
        this.overallReadinessLevel = overallReadinessLevel;
    }

    public int getTotalSubjects() {
        return totalSubjects;
    }

    public void setTotalSubjects(int totalSubjects) {
        this.totalSubjects = totalSubjects;
    }

    public int getHighReadinessCount() {
        return highReadinessCount;
    }

    public void setHighReadinessCount(int highReadinessCount) {
        this.highReadinessCount = highReadinessCount;
    }

    public int getModerateReadinessCount() {
        return moderateReadinessCount;
    }

    public void setModerateReadinessCount(int moderateReadinessCount) {
        this.moderateReadinessCount = moderateReadinessCount;
    }

    public int getLowReadinessCount() {
        return lowReadinessCount;
    }

    public void setLowReadinessCount(int lowReadinessCount) {
        this.lowReadinessCount = lowReadinessCount;
    }

    public int getUrgentExamsCount() {
        return urgentExamsCount;
    }

    public void setUrgentExamsCount(int urgentExamsCount) {
        this.urgentExamsCount = urgentExamsCount;
    }

    public List<ExamReadinessDto> getSubjectEvaluations() {
        return subjectEvaluations;
    }

    public void setSubjectEvaluations(List<ExamReadinessDto> subjectEvaluations) {
        this.subjectEvaluations = subjectEvaluations;
    }
}

