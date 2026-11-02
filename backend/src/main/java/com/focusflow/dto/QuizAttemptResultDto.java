package com.focusflow.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class QuizAttemptResultDto {

    private Long attemptId;
    private Long quizId;
    private String quizTitle;
    private String subjectName;
    private String topicName;
    private int score;
    private int totalMarks;
    private double percentage;
    private boolean passed;
    private int passingMarks;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private int timeSpentSeconds;
    private List<AnswerResultDto> questionResults = new ArrayList<>();

    public static class AnswerResultDto {
        private Long questionId;
        private String questionText;
        private Long topicId;
        private String topicName;
        private int marks;
        private Long selectedOptionId;
        private String selectedOptionText;
        private Long correctOptionId;
        private String correctOptionText;
        private boolean isCorrect;
        private int marksAwarded;
        private String explanation;

        public AnswerResultDto() {
        }

        public AnswerResultDto(Long questionId, String questionText, Long topicId, String topicName, int marks,
                               Long selectedOptionId, String selectedOptionText, Long correctOptionId,
                               String correctOptionText, boolean isCorrect, int marksAwarded, String explanation) {
            this.questionId = questionId;
            this.questionText = questionText;
            this.topicId = topicId;
            this.topicName = topicName;
            this.marks = marks;
            this.selectedOptionId = selectedOptionId;
            this.selectedOptionText = selectedOptionText;
            this.correctOptionId = correctOptionId;
            this.correctOptionText = correctOptionText;
            this.isCorrect = isCorrect;
            this.marksAwarded = marksAwarded;
            this.explanation = explanation;
        }

        public Long getQuestionId() {
            return questionId;
        }

        public void setQuestionId(Long questionId) {
            this.questionId = questionId;
        }

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

        public String getTopicName() {
            return topicName;
        }

        public void setTopicName(String topicName) {
            this.topicName = topicName;
        }

        public int getMarks() {
            return marks;
        }

        public void setMarks(int marks) {
            this.marks = marks;
        }

        public Long getSelectedOptionId() {
            return selectedOptionId;
        }

        public void setSelectedOptionId(Long selectedOptionId) {
            this.selectedOptionId = selectedOptionId;
        }

        public String getSelectedOptionText() {
            return selectedOptionText;
        }

        public void setSelectedOptionText(String selectedOptionText) {
            this.selectedOptionText = selectedOptionText;
        }

        public Long getCorrectOptionId() {
            return correctOptionId;
        }

        public void setCorrectOptionId(Long correctOptionId) {
            this.correctOptionId = correctOptionId;
        }

        public String getCorrectOptionText() {
            return correctOptionText;
        }

        public void setCorrectOptionText(String correctOptionText) {
            this.correctOptionText = correctOptionText;
        }

        public boolean isCorrect() {
            return isCorrect;
        }

        public void setCorrect(boolean correct) {
            isCorrect = correct;
        }

        public int getMarksAwarded() {
            return marksAwarded;
        }

        public void setMarksAwarded(int marksAwarded) {
            this.marksAwarded = marksAwarded;
        }

        public String getExplanation() {
            return explanation;
        }

        public void setExplanation(String explanation) {
            this.explanation = explanation;
        }
    }

    public QuizAttemptResultDto() {
    }

    public QuizAttemptResultDto(Long attemptId, Long quizId, String quizTitle, String subjectName, String topicName,
                                int score, int totalMarks, double percentage, boolean passed, int passingMarks,
                                LocalDateTime startedAt, LocalDateTime completedAt, int timeSpentSeconds,
                                List<AnswerResultDto> questionResults) {
        this.attemptId = attemptId;
        this.quizId = quizId;
        this.quizTitle = quizTitle;
        this.subjectName = subjectName;
        this.topicName = topicName;
        this.score = score;
        this.totalMarks = totalMarks;
        this.percentage = percentage;
        this.passed = passed;
        this.passingMarks = passingMarks;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.timeSpentSeconds = timeSpentSeconds;
        this.questionResults = questionResults;
    }

    // Getters and Setters
    public Long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public Long getQuizId() {
        return quizId;
    }

    public void setQuizId(Long quizId) {
        this.quizId = quizId;
    }

    public String getQuizTitle() {
        return quizTitle;
    }

    public void setQuizTitle(String quizTitle) {
        this.quizTitle = quizTitle;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(int totalMarks) {
        this.totalMarks = totalMarks;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public int getPassingMarks() {
        return passingMarks;
    }

    public void setPassingMarks(int passingMarks) {
        this.passingMarks = passingMarks;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public int getTimeSpentSeconds() {
        return timeSpentSeconds;
    }

    public void setTimeSpentSeconds(int timeSpentSeconds) {
        this.timeSpentSeconds = timeSpentSeconds;
    }

    public List<AnswerResultDto> getQuestionResults() {
        return questionResults;
    }

    public void setQuestionResults(List<AnswerResultDto> questionResults) {
        this.questionResults = questionResults;
    }
}

