package com.focusflow.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Authentication response payload for registration and login operations.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private boolean success;
    private String message;
    private Long userId;
    private String email;
    private String fullName;
    private String role;
    private String accessToken;

    public AuthResponse() {
    }

    public AuthResponse(boolean success, String message, Long userId, String email, String fullName, String role) {
        this.success = success;
        this.message = message;
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    public AuthResponse(boolean success, String message, Long userId, String email, String fullName, String role, String accessToken) {
        this.success = success;
        this.message = message;
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.accessToken = accessToken;
    }

    public static AuthResponse registrationSuccess(Long userId, String email, String fullName, String role) {
        return new AuthResponse(true, "Student registered successfully.", userId, email, fullName, role);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}

