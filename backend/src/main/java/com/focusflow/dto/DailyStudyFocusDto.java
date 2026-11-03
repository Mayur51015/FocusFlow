package com.focusflow.dto;

public class DailyStudyFocusDto {
    private Long topicId;
    private String topicName;
    private Long subjectId;
    private String subjectName;
    private String difficulty;
    private String reason;
    private int priorityScore;
    private String actionType; // STUDY, QUIZ, REVISION

    public DailyStudyFocusDto() {
    }

    public DailyStudyFocusDto(Long topicId, String topicName, Long subjectId, String subjectName,
                             String difficulty, String reason, int priorityScore, String actionType) {
        this.topicId = topicId;
        this.topicName = topicName;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.difficulty = difficulty;
        this.reason = reason;
        this.priorityScore = priorityScore;
        this.actionType = actionType;
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

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public int getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(int priorityScore) {
        this.priorityScore = priorityScore;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }
}

