package com.focusflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity recording administrative actions, role assignments, status modifications,
 * and security events for governance and compliance.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_created_at", columnList = "created_at"),
    @Index(name = "idx_audit_admin", columnList = "admin_user_id"),
    @Index(name = "idx_audit_action", columnList = "action")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_user_id")
    private Long adminUserId;

    @Column(name = "admin_email", length = 150)
    private String adminEmail;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "target_entity", length = 100)
    private String targetEntity;

    @Column(name = "target_id", length = 100)
    private String targetId;

    @Column(length = 1000)
    private String details;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public AuditLog() {
    }

    public AuditLog(Long adminUserId, String adminEmail, String action, String targetEntity, String targetId, String details, String ipAddress) {
        this.adminUserId = adminUserId;
        this.adminEmail = adminEmail;
        this.action = action;
        this.targetEntity = targetEntity;
        this.targetId = targetId;
        this.details = details;
        this.ipAddress = ipAddress;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAdminUserId() {
        return adminUserId;
    }

    public void setAdminUserId(Long adminUserId) {
        this.adminUserId = adminUserId;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetEntity() {
        return targetEntity;
    }

    public void setTargetEntity(String targetEntity) {
        this.targetEntity = targetEntity;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

