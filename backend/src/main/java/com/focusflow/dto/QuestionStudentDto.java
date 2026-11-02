package com.focusflow.dto;

import java.util.List;

/**
 * Question presentation object for student active quiz attempts.
 * Deliberately conceals correct answers and explanations.
 */
public class QuestionStudentDto {

    private Long id;
    private Long topicId;
    private String topicName;
    private String questionText;
    private int marks;
    private List<OptionStudentDto> options;

    public QuestionStudentDto() {
    }

    public QuestionStudentDto(Long id, Long topicId, String topicName, String questionText, int marks, List<OptionStudentDto> options) {
        this.id = id;
        this.topicId = topicId;
        this.topicName = topicName;
        this.questionText = questionText;
        this.marks = marks;
        this.options = options;
    }

    public static class OptionStudentDto {
        private Long id;
        private String optionText;
        private int optionOrder;

        public OptionStudentDto() {
        }

        public OptionStudentDto(Long id, String optionText, int optionOrder) {
            this.id = id;
            this.optionText = optionText;
            this.optionOrder = optionOrder;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getOptionText() {
            return optionText;
        }

        public void setOptionText(String optionText) {
            this.optionText = optionText;
        }

        public int getOptionOrder() {
            return optionOrder;
        }

        public void setOptionOrder(int optionOrder) {
            this.optionOrder = optionOrder;
        }
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

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public int getMarks() {
        return marks;
    }

    public void setMarks(int marks) {
        this.marks = marks;
    }

    public List<OptionStudentDto> getOptions() {
        return options;
    }

    public void setOptions(List<OptionStudentDto> options) {
        this.options = options;
    }
}

