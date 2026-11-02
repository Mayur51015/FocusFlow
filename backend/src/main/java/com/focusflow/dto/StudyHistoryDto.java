package com.focusflow.dto;

import com.focusflow.entity.Difficulty;
import com.focusflow.entity.SessionStatus;
import com.focusflow.entity.StudySession;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO representing a historical study session record for analytics and ML training.
 */
public class StudyHistoryDto {

    private Long id;
    private Long topicId;
    private String topicName;
    private Long subjectId;
    private String subjectName;
    private Difficulty difficulty;
    private LocalDate scheduledDate;
    private int plannedMinutes;
    private int actualMinutes;
    private SessionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime completedAt;
    private boolean isRescheduled;
    private boolean locked;
    private Integer predictedMinutes;
    private String predictionSource;

    public StudyHistoryDto() {
    }

    public static StudyHistoryDto fromEntity(StudySession session) {
        StudyHistoryDto dto = new StudyHistoryDto();
        dto.setId(session.getId());
        dto.setScheduledDate(session.getScheduledDate());
        dto.setPlannedMinutes(session.getPlannedMinutes());
        dto.setActualMinutes(session.getActualMinutes());
        dto.setStatus(session.getStatus());
        dto.setStartTime(session.getStartTime());
        dto.setEndTime(session.getEndTime());
        dto.setCompletedAt(session.getCompletedAt());
        dto.setRescheduled(session.isRescheduled());
        dto.setLocked(session.isLocked());
        dto.setPredictedMinutes(session.getPredictedMinutes());
        dto.setPredictionSource(session.getPredictionSource());

        if (session.getTopic() != null) {
            dto.setTopicId(session.getTopic().getId());
            dto.setTopicName(session.getTopic().getName());
            dto.setDifficulty(session.getTopic().getDifficulty());
            if (session.getTopic().getSubject() != null) {
                dto.setSubjectId(session.getTopic().getSubject().getId());
                dto.setSubjectName(session.getTopic().getSubject().getName());
            }
        }
        return dto;
    }

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

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public int getPlannedMinutes() {
        return plannedMinutes;
    }

    public void setPlannedMinutes(int plannedMinutes) {
        this.plannedMinutes = plannedMinutes;
    }

    public int getActualMinutes() {
        return actualMinutes;
    }

    public void setActualMinutes(int actualMinutes) {
        this.actualMinutes = actualMinutes;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public boolean isRescheduled() {
        return isRescheduled;
    }

    public void setRescheduled(boolean rescheduled) {
        isRescheduled = rescheduled;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public Integer getPredictedMinutes() {
        return predictedMinutes;
    }

    public void setPredictedMinutes(Integer predictedMinutes) {
        this.predictedMinutes = predictedMinutes;
    }

    public String getPredictionSource() {
        return predictionSource;
    }

    public void setPredictionSource(String predictionSource) {
        this.predictionSource = predictionSource;
    }
}

