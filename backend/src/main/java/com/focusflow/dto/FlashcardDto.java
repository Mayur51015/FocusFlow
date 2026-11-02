package com.focusflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.focusflow.entity.RecallRating;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class FlashcardDto {

    private Long id;
    private Long topicId;
    private String topicName;
    private String subjectName;
    private String frontPrompt;
    private String backAnswer;
    private String hint;
    private int repetitionNumber;
    private int intervalDays;
    private double easinessFactor;
    private LocalDate nextReviewDate;
    private boolean due;

    public FlashcardDto() {
    }

    public FlashcardDto(Long id, Long topicId, String topicName, String subjectName,
                        String frontPrompt, String backAnswer, String hint,
                        int repetitionNumber, int intervalDays, double easinessFactor,
                        LocalDate nextReviewDate, boolean due) {
        this.id = id;
        this.topicId = topicId;
        this.topicName = topicName;
        this.subjectName = subjectName;
        this.frontPrompt = frontPrompt;
        this.backAnswer = backAnswer;
        this.hint = hint;
        this.repetitionNumber = repetitionNumber;
        this.intervalDays = intervalDays;
        this.easinessFactor = easinessFactor;
        this.nextReviewDate = nextReviewDate;
        this.due = due;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTopicId() {
        return topicId;
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getFrontPrompt() {
        return frontPrompt;
    }

    public void setFrontPrompt(String frontPrompt) {
        this.frontPrompt = frontPrompt;
    }

    public String getBackAnswer() {
        return backAnswer;
    }

    public void setBackAnswer(String backAnswer) {
        this.backAnswer = backAnswer;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public int getRepetitionNumber() {
        return repetitionNumber;
    }

    public void setRepetitionNumber(int repetitionNumber) {
        this.repetitionNumber = repetitionNumber;
    }

    public int getIntervalDays() {
        return intervalDays;
    }

    public void setIntervalDays(int intervalDays) {
        this.intervalDays = intervalDays;
    }

    public double getEasinessFactor() {
        return easinessFactor;
    }

    public void setEasinessFactor(double easinessFactor) {
        this.easinessFactor = easinessFactor;
    }

    public LocalDate getNextReviewDate() {
        return nextReviewDate;
    }

    public void setNextReviewDate(LocalDate nextReviewDate) {
        this.nextReviewDate = nextReviewDate;
    }

    public boolean isDue() {
        return due;
    }

    public void setDue(boolean due) {
        this.due = due;
    }
}

