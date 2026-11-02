package com.focusflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QuizAttemptSubmitRequest {

    @Min(value = 0, message = "Time spent cannot be negative")
    private int timeSpentSeconds = 0;

    private List<AnswerSubmissionDto> answers = new ArrayList<>();

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AnswerSubmissionDto {
        private Long questionId;
        private Long selectedOptionId;

        public AnswerSubmissionDto() {
        }

        public AnswerSubmissionDto(Long questionId, Long selectedOptionId) {
            this.questionId = questionId;
            this.selectedOptionId = selectedOptionId;
        }

        public Long getQuestionId() {
            return questionId;
        }

        public void setQuestionId(Long questionId) {
            this.questionId = questionId;
        }

        public Long getSelectedOptionId() {
            return selectedOptionId;
        }

        public void setSelectedOptionId(Long selectedOptionId) {
            this.selectedOptionId = selectedOptionId;
        }
    }

    public QuizAttemptSubmitRequest() {
    }

    public QuizAttemptSubmitRequest(int timeSpentSeconds, List<AnswerSubmissionDto> answers) {
        this.timeSpentSeconds = timeSpentSeconds;
        this.answers = answers;
    }

    public int getTimeSpentSeconds() {
        return timeSpentSeconds;
    }

    public void setTimeSpentSeconds(int timeSpentSeconds) {
        this.timeSpentSeconds = timeSpentSeconds;
    }

    @JsonProperty("timeTakenSeconds")
    public void setTimeTakenSeconds(int timeTakenSeconds) {
        this.timeSpentSeconds = timeTakenSeconds;
    }

    public List<AnswerSubmissionDto> getAnswers() {
        return answers;
    }

    public void setAnswers(List<AnswerSubmissionDto> answers) {
        this.answers = answers;
    }
}
