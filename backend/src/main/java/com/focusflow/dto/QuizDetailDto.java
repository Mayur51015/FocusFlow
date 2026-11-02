package com.focusflow.dto;

import java.time.LocalDateTime;
import java.util.List;

public class QuizDetailDto {

    private Long id;
    private String title;
    private String description;
    private Long subjectId;
    private String subjectName;
    private Long topicId;
    private String topicName;
    private int durationMinutes;
    private int totalMarks;
    private int passingMarks;
    private LocalDateTime createdAt;
    private List<QuestionStudentDto> questions;

    public QuizDetailDto() {
    }

    public QuizDetailDto(Long id, String title, String description, Long subjectId, String subjectName,
                         Long topicId, String topicName, int durationMinutes, int totalMarks, int passingMarks,
                         LocalDateTime createdAt, List<QuestionStudentDto> questions) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.topicId = topicId;
        this.topicName = topicName;
        this.durationMinutes = durationMinutes;
        this.totalMarks = totalMarks;
        this.passingMarks = passingMarks;
        this.createdAt = createdAt;
        this.questions = questions;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public int getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(int totalMarks) {
        this.totalMarks = totalMarks;
    }

    public int getPassingMarks() {
        return passingMarks;
    }

    public void setPassingMarks(int passingMarks) {
        this.passingMarks = passingMarks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<QuestionStudentDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionStudentDto> questions) {
        this.questions = questions;
    }
}

