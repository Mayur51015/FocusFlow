package com.focusflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Topic Entity represents a unit of study belonging to a Subject.
 * Stores difficulty, estimated study duration, and completion status.
 * Topic ownership is verified through its parent Subject and User.
 */
@Entity
@Table(name = "topics", indexes = {
    @Index(name = "idx_topics_subject", columnList = "subject_id")
})
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Topic name cannot be empty")
    @Column(nullable = false, length = 150)
    private String name;

    @NotNull(message = "Associated subject is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id", nullable = false)
    @JsonIgnoreProperties("topics") // Prevents bidirectional JSON infinite loop
    private Subject subject;

    @NotNull(message = "Difficulty level is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty;

    @Min(value = 1, message = "Estimated duration must be at least 1 minute")
    @Column(name = "estimated_minutes", nullable = false)
    private int estimatedMinutes;

    @Column(nullable = false)
    private boolean completed = false;

    // Default constructor
    public Topic() {
    }

    // Parameterized constructor
    public Topic(String name, Subject subject, Difficulty difficulty, int estimatedMinutes, boolean completed) {
        this.name = name;
        this.subject = subject;
        this.difficulty = difficulty;
        this.estimatedMinutes = estimatedMinutes;
        this.completed = completed;
    }

    @JsonIgnore
    public User getUser() {
        return subject != null ? subject.getUser() : null;
    }

    @JsonProperty("userId")
    public Long getUserId() {
        return getUser() != null ? getUser().getId() : null;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(int estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
