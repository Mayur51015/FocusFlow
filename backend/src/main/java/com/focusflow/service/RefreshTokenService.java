package com.focusflow.service;

import com.focusflow.entity.RefreshToken;
import com.focusflow.entity.User;
import com.focusflow.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service managing refresh token lifecycle, secure rotation, and revocation.
 */
@Service
public class RefreshTokenService {

    @Value("${focusflow.security.jwt.refresh-token-expiration-ms:604800000}") // 7 days default
    private long refreshTokenExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Create and persist a new refresh token for the authenticated user, revoking older tokens.
     */
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Revoke active refresh tokens for this user (token rotation policy)
        refreshTokenRepository.revokeAllUserTokens(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString());
        refreshToken.setExpiryDate(LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000));
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * Verify that the refresh token is not expired and has not been revoked.
     */
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isRevoked()) {
            throw new IllegalArgumentException("Refresh token has been revoked. Please sign in again.");
        }
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new IllegalArgumentException("Refresh token was expired. Please sign in again.");
        }
        return token;
    }

    /**
     * Rotate an active refresh token: revoke the old token and issue a fresh one.
     */
    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken oldToken) {
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        RefreshToken newToken = new RefreshToken();
        newToken.setUser(oldToken.getUser());
        newToken.setToken(UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString());
        newToken.setExpiryDate(LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000));
        newToken.setRevoked(false);

        return refreshTokenRepository.save(newToken);
    }

    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    @Transactional
    public void revokeUserTokens(User user) {
        refreshTokenRepository.revokeAllUserTokens(user);
    }
}

