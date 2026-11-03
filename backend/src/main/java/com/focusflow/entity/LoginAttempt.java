package com.focusflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity recording authentication attempts to detect brute-force attacks and rate-limit abuse.
 */
@Entity
@Table(name = "login_attempts", indexes = {
    @Index(name = "idx_login_attempt_email_time", columnList = "email, attempted_at"),
    @Index(name = "idx_login_attempt_ip_time", columnList = "ip_address, attempted_at")
})
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 150)
    private String email;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "failure_reason", length = 100)
    private String failureReason;

    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;

    @PrePersist
    protected void onCreate() {
        if (this.attemptedAt == null) {
            this.attemptedAt = LocalDateTime.now();
        }
    }

    public LoginAttempt() {
    }

    public LoginAttempt(String email, String ipAddress, boolean success, String failureReason) {
        this.email = email != null ? email.toLowerCase().trim() : null;
        this.ipAddress = ipAddress;
        this.success = success;
        this.failureReason = failureReason;
        this.attemptedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email != null ? email.toLowerCase().trim() : null;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(LocalDateTime attemptedAt) {
        this.attemptedAt = attemptedAt;
    }
}

