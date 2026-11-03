package com.focusflow.dto.auth;

import java.util.Set;

/**
 * DTO representing user profile returned by GET /api/auth/me and GET /api/admin/users.
 */
public class UserProfileDto {

    private Long id;
    private String email;
    private String fullName;
    private String role;
    private Set<String> roles;
    private boolean emailVerified;
    private boolean accountNonLocked;
    private boolean enabled;
    private String createdAt;

    public UserProfileDto() {
    }

    public UserProfileDto(Long id, String email, String fullName, String role, Set<String> roles,
                          boolean emailVerified, boolean accountNonLocked, boolean enabled, String createdAt) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.roles = roles;
        this.emailVerified = emailVerified;
        this.accountNonLocked = accountNonLocked;
        this.enabled = enabled;
        this.createdAt = createdAt;
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
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    public void setAccountNonLocked(boolean accountNonLocked) {
        this.accountNonLocked = accountNonLocked;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
