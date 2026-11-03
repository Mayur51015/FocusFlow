package com.focusflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Subject Entity represents a course or subject a student is studying.
 * Multi-user ownership is enforced via the user foreign key.
 */
@Entity
@Table(name = "subjects", indexes = {
    @Index(name = "idx_subjects_user", columnList = "user_id")
})
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @NotBlank(message = "Subject name cannot be empty")
    @Column(nullable = false, length = 100)
    private String name;

    @NotNull(message = "Exam date is required")
    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @NotNull(message = "Importance level is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Importance importance;

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("subject") // Prevents bidirectional JSON infinite recursion
    private List<Topic> topics = new ArrayList<>();

    // Default constructor (required by JPA/Hibernate)
    public Subject() {
    }

    // Parameterized constructor for backward compatibility
    public Subject(String name, LocalDate examDate, Importance importance) {
        this.name = name;
        this.examDate = examDate;
        this.importance = importance;
    }

    // Parameterized constructor with User ownership
    public Subject(String name, LocalDate examDate, Importance importance, User user) {
        this.name = name;
        this.examDate = examDate;
        this.importance = importance;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public void setExamDate(LocalDate examDate) {
        this.examDate = examDate;
    }

    public Importance getImportance() {
        return importance;
    }

    public void setImportance(Importance importance) {
        this.importance = importance;
    }

    public List<Topic> getTopics() {
        return topics;
    }

    public void setTopics(List<Topic> topics) {
        this.topics = topics;
    }
}
