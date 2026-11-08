package com.focusflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * QuizAttempt entity tracks a completed student attempt for a Quiz.
 * Scoped to individual users via user ownership.
 */
@Entity
@Table(name = "quiz_attempts", indexes = {
    @Index(name = "idx_quiz_attempts_user", columnList = "user_id"),
    @Index(name = "idx_quiz_attempts_quiz", columnList = "quiz_id")
})
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @NotNull(message = "Quiz reference is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "quiz_id", nullable = false)
    @JsonIgnoreProperties("questions")
    private Quiz quiz;

    @Column(nullable = false)
    private int score = 0;

    @Column(nullable = false)
    private double percentage = 0.0;

    @Column(nullable = false)
    private boolean passed = false;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt = LocalDateTime.now();

    @Column(name = "time_spent_seconds", nullable = false)
    private int timeSpentSeconds = 0;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizAnswer> answers = new ArrayList<>();

    public QuizAttempt() {
    }

    public QuizAttempt(Quiz quiz, int score, double percentage, boolean passed, LocalDateTime startedAt, LocalDateTime completedAt, int timeSpentSeconds) {
        this.quiz = quiz;
        this.score = score;
        this.percentage = percentage;
        this.passed = passed;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.timeSpentSeconds = timeSpentSeconds;
    }

    public QuizAttempt(Quiz quiz, int score, double percentage, boolean passed, LocalDateTime startedAt, LocalDateTime completedAt, int timeSpentSeconds, User user) {
        this.quiz = quiz;
        this.score = score;
        this.percentage = percentage;
        this.passed = passed;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.timeSpentSeconds = timeSpentSeconds;
        this.user = user;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @JsonProperty("userId")
    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
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

    public List<QuizAnswer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<QuizAnswer> answers) {
        this.answers = answers;
    }
}
