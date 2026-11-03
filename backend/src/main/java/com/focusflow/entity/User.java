package com.focusflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * User entity representing registered students and administrators in FocusFlow 2.0.
 * Enforces account status, email verification, brute-force lockout, and multi-role RBAC.
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role = Role.ROLE_USER;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 30)
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "account_non_locked", nullable = false)
    private boolean accountNonLocked = true;

    @Column(name = "failed_attempt_count", nullable = false)
    private int failedAttemptCount = 0;

    @Column(name = "lock_time")
    private LocalDateTime lockTime;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.role == null) {
            this.role = Role.ROLE_USER;
        }
        if (this.roles == null || this.roles.isEmpty()) {
            this.roles = new HashSet<>();
            this.roles.add(this.role);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public User() {
        this.role = Role.ROLE_USER;
        this.roles.add(Role.ROLE_USER);
    }

    public User(String email, String passwordHash, String fullName) {
        setEmail(email);
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = Role.ROLE_USER;
        this.roles.add(Role.ROLE_USER);
        this.enabled = true;
        this.accountNonLocked = true;
        this.failedAttemptCount = 0;
        this.emailVerified = true;
    }

    public User(String email, String passwordHash, String fullName, Role role) {
        setEmail(email);
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = (role != null) ? (role == Role.ROLE_STUDENT ? Role.ROLE_USER : role) : Role.ROLE_USER;
        this.roles.add(this.role);
        this.enabled = true;
        this.accountNonLocked = true;
        this.failedAttemptCount = 0;
        this.emailVerified = true;
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        Role targetRole = (role != null) ? (role == Role.ROLE_STUDENT ? Role.ROLE_USER : role) : Role.ROLE_USER;
        this.role = targetRole;
        if (this.roles == null) {
            this.roles = new HashSet<>();
        }
        this.roles.add(targetRole);
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles != null ? roles : new HashSet<>();
        if (!this.roles.isEmpty()) {
            this.role = this.roles.contains(Role.ROLE_ADMIN) ? Role.ROLE_ADMIN : this.roles.iterator().next();
        }
    }

    public void addRole(Role r) {
        if (r != null) {
            Role targetRole = (r == Role.ROLE_STUDENT) ? Role.ROLE_USER : r;
            if (this.roles == null) {
                this.roles = new HashSet<>();
            }
            this.roles.add(targetRole);
            if (targetRole == Role.ROLE_ADMIN) {
                this.role = Role.ROLE_ADMIN;
            }
        }
    }

    public void removeRole(Role r) {
        if (r != null && this.roles != null) {
            this.roles.remove(r);
            if (r == Role.ROLE_ADMIN && this.role == Role.ROLE_ADMIN) {
                this.role = Role.ROLE_USER;
            }
        }
    }

    public boolean hasRole(Role r) {
        return (this.role == r) || (this.roles != null && this.roles.contains(r));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    public void setAccountNonLocked(boolean accountNonLocked) {
        this.accountNonLocked = accountNonLocked;
    }

    public int getFailedAttemptCount() {
        return failedAttemptCount;
    }

    public void setFailedAttemptCount(int failedAttemptCount) {
        this.failedAttemptCount = failedAttemptCount;
    }

    public void incrementFailedAttempts() {
        this.failedAttemptCount++;
    }

    public void resetFailedAttempts() {
        this.failedAttemptCount = 0;
        this.lockTime = null;
        this.accountNonLocked = true;
    }

    public LocalDateTime getLockTime() {
        return lockTime;
    }

    public void setLockTime(LocalDateTime lockTime) {
        this.lockTime = lockTime;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
