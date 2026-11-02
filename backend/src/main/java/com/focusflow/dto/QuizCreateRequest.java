package com.focusflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QuizCreateRequest {

    @NotBlank(message = "Quiz title is required")
    private String title;

    private String description;

    @NotNull(message = "Subject ID is required")
    private Long subjectId;

    private Long topicId;

    @Min(value = 1, message = "Duration must be at least 1 minute")
    private int durationMinutes = 15;

    @Min(value = 0, message = "Passing marks cannot be negative")
    private int passingMarks = 50;

    @NotEmpty(message = "Quiz must contain at least one question")
    @Valid
    private List<QuestionCreateDto> questions;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuestionCreateDto {
        @NotBlank(message = "Question text cannot be empty")
        private String questionText;

        private Long topicId;

        private String questionType = "MULTIPLE_CHOICE";

        @Min(value = 1, message = "Marks must be at least 1")
        private int marks = 1;

        private String explanation;

        @NotEmpty(message = "Question must have at least two answer options")
        @Valid
        private List<OptionCreateDto> options;

        public String getQuestionText() {
            return questionText;
        }

        public void setQuestionText(String questionText) {
            this.questionText = questionText;
        }

        public Long getTopicId() {
            return topicId;
        }

        public void setTopicId(Long topicId) {
            this.topicId = topicId;
        }

        public String getQuestionType() {
            return questionType;
        }

        public void setQuestionType(String questionType) {
            this.questionType = questionType;
        }

        public int getMarks() {
            return marks;
        }

        public void setMarks(int marks) {
            this.marks = marks;
        }

        public String getExplanation() {
            return explanation;
        }

        public void setExplanation(String explanation) {
            this.explanation = explanation;
        }

        public List<OptionCreateDto> getOptions() {
            return options;
        }

        public void setOptions(List<OptionCreateDto> options) {
            this.options = options;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OptionCreateDto {
        @NotBlank(message = "Option text cannot be empty")
        private String optionText;

        @JsonProperty("isCorrect")
        private boolean isCorrect = false;

        private int optionOrder = 1;

        public String getOptionText() {
            return optionText;
        }

        public void setOptionText(String optionText) {
            this.optionText = optionText;
        }

        @JsonProperty("isCorrect")
        public boolean isCorrect() {
            return isCorrect;
        }

        public boolean getIsCorrect() {
            return isCorrect;
        }

        @JsonProperty("isCorrect")
        public void setIsCorrect(boolean isCorrect) {
            this.isCorrect = isCorrect;
        }

        public void setCorrect(boolean correct) {
            this.isCorrect = correct;
        }

        public int getOptionOrder() {
            return optionOrder;
        }

        public void setOptionOrder(int optionOrder) {
            this.optionOrder = optionOrder;
        }
    }

    // Getters and Setters
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

    public Long getTopicId() {
        return topicId;
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public int getPassingMarks() {
        return passingMarks;
    }

    public void setPassingMarks(int passingMarks) {
        this.passingMarks = passingMarks;
    }

    public List<QuestionCreateDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionCreateDto> questions) {
        this.questions = questions;
    }
}
