package com.focusflow.dto.auth;

/**
 * Payload for explicit refresh token requests (fallback when HttpOnly cookie is unavailable).
 */
public class RefreshTokenRequest {

    private String refreshToken;

    public RefreshTokenRequest() {
    }

    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}

