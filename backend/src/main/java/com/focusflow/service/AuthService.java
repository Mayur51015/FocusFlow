package com.focusflow.service;

import com.focusflow.dto.auth.*;
import com.focusflow.entity.*;
import com.focusflow.repository.PasswordResetTokenRepository;
import com.focusflow.repository.RefreshTokenRepository;
import com.focusflow.repository.UserRepository;
import com.focusflow.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service handling student registration, authentication, rate-limited login,
 * password recovery, password change, and token rotation workflows.
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final LoginRateLimiterService loginRateLimiterService;

    public record AuthResult(AuthResponse authResponse, String rawRefreshToken) {}

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       LoginRateLimiterService loginRateLimiterService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.loginRateLimiterService = loginRateLimiterService;
    }

    /**
     * Register a new student account with validated credentials and BCrypt hashed password.
     * Always assigns ROLE_USER to prevent unauthorized privilege escalation.
     */
    @Transactional
    public AuthResponse registerStudent(RegisterRequest request) {
        String cleanEmail = request.getEmail() != null ? request.getEmail().toLowerCase().trim() : "";

        if (userRepository.existsByEmail(cleanEmail)) {
            logger.warn("Registration rejected: Email '{}' is already registered.", cleanEmail);
            throw new IllegalArgumentException("An account with this email address already exists.");
        }

        if (request.getPassword() == null || !request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match. Please re-enter.");
        }

        if (request.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long.");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User newUser = new User(
                cleanEmail,
                hashedPassword,
                request.getFullName().trim(),
                Role.ROLE_USER
        );

        User saved = userRepository.save(newUser);
        logger.info("Successfully registered student account: ID={}, Email='{}', Role={}", 
                saved.getId(), saved.getEmail(), saved.getRole());

        return AuthResponse.registrationSuccess(
                saved.getId(),
                saved.getEmail(),
                saved.getFullName(),
                saved.getRole().name()
        );
    }

    /**
     * Backward-compatible login method.
     */
    @Transactional
    public AuthResult login(LoginRequest request) {
        return login(request, "127.0.0.1");
    }

    /**
     * Authenticate student credentials with rate limiting and brute force protection.
     */
    @Transactional
    public AuthResult login(LoginRequest request, String ipAddress) {
        String cleanEmail = request.getEmail() != null ? request.getEmail().toLowerCase().trim() : "";

        // 1. Check Rate Limiter
        if (loginRateLimiterService != null && loginRateLimiterService.isBlocked(cleanEmail, ipAddress)) {
            logger.warn("Authentication blocked for email '{}' / IP '{}': Exceeded failed attempt threshold.", cleanEmail, ipAddress);
            throw new IllegalArgumentException("Account is temporarily locked due to multiple failed login attempts. Please try again in 15 minutes.");
        }

        User user = userRepository.findByEmail(cleanEmail).orElse(null);

        // 2. Check Account Lock Status
        if (user != null && !user.isAccountNonLocked()) {
            boolean released = loginRateLimiterService != null && loginRateLimiterService.checkAndReleaseLock(user);
            if (!released) {
                logger.warn("Login rejected for email '{}': Account locked until lock duration expires.", cleanEmail);
                throw new IllegalArgumentException("Account is temporarily locked due to multiple failed login attempts. Please try again later.");
            }
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(cleanEmail, request.getPassword())
            );

            if (user == null) {
                user = userRepository.findByEmail(cleanEmail)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));
            }

            if (!user.isEnabled()) {
                throw new IllegalArgumentException("Student account has been disabled. Please contact support.");
            }

            // 3. Record Successful Attempt & Reset Counters
            if (loginRateLimiterService != null) {
                loginRateLimiterService.recordSuccessfulAttempt(cleanEmail, ipAddress, user);
            }

            // Generate short-lived access token (15 mins)
            String accessToken = jwtService.generateAccessToken(
                    user.getEmail(),
                    user.getId(),
                    user.getFullName(),
                    user.getRole().name()
            );

            // Generate long-lived refresh token with single-device rotation (7 days)
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            AuthResponse response = new AuthResponse(
                    true,
                    "Login successful.",
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole().name(),
                    accessToken
            );

            logger.info("Successful student login: ID={}, Email='{}'", user.getId(), user.getEmail());
            return new AuthResult(response, refreshToken.getToken());

        } catch (BadCredentialsException ex) {
            logger.warn("Failed login attempt for email '{}': Bad credentials", cleanEmail);
            if (loginRateLimiterService != null) {
                loginRateLimiterService.recordFailedAttempt(cleanEmail, ipAddress, "Bad credentials");
            }
            throw new IllegalArgumentException("Invalid email or password.");
        } catch (LockedException ex) {
            throw new IllegalArgumentException("Account is locked. Please try again later.");
        }
    }

    /**
     * Refresh access token via token rotation. Issues a new access token and a rotated refresh token.
     */
    @Transactional
    public AuthResult refreshAccessToken(String rawRefreshToken) {
        RefreshToken refreshToken = refreshTokenService.findByToken(rawRefreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token. Please sign in again."));

        RefreshToken validToken = refreshTokenService.verifyExpiration(refreshToken);
        User user = validToken.getUser();

        if (!user.isEnabled()) {
            throw new IllegalArgumentException("Account has been disabled.");
        }

        // Rotate refresh token
        RefreshToken rotatedToken = refreshTokenService.rotateRefreshToken(validToken);

        // Generate new short-lived access token
        String newAccessToken = jwtService.generateAccessToken(
                user.getEmail(),
                user.getId(),
                user.getFullName(),
                user.getRole().name()
        );

        AuthResponse response = new AuthResponse(
                true,
                "Token refreshed successfully.",
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                newAccessToken
        );

        logger.info("Access token successfully refreshed for User ID={}", user.getId());
        return new AuthResult(response, rotatedToken.getToken());
    }

    /**
     * Revoke refresh token upon user logout.
     */
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeToken(rawRefreshToken);
        logger.info("Refresh token revoked during logout.");
    }

    /**
     * Initiate password recovery.
     * Never reveals whether an email is registered to prevent user enumeration attacks.
     */
    @Transactional
    public AuthResponse forgotPassword(ForgotPasswordRequest request, String ipAddress) {
        String cleanEmail = request.getEmail() != null ? request.getEmail().toLowerCase().trim() : "";
        String genericSuccessMessage = "If an account with that email exists, password reset instructions have been dispatched.";

        User user = userRepository.findByEmail(cleanEmail).orElse(null);
        if (user == null) {
            logger.info("Password reset requested for non-existent email '{}' from IP '{}'. Returning generic response.", cleanEmail, ipAddress);
            return new AuthResponse(true, genericSuccessMessage, null, null, null, null);
        }

        // Clean up previous tokens for this user
        passwordResetTokenRepository.deleteByUser(user);

        // Generate single-use secure token valid for 30 minutes
        String rawToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(30);

        PasswordResetToken resetToken = new PasswordResetToken(user, rawToken, expiry);
        passwordResetTokenRepository.save(resetToken);

        // For local development and demonstration, log the token
        logger.info("==================================================================");
        logger.info("PASSWORD RECOVERY TOKEN FOR '{}': {}", cleanEmail, rawToken);
        logger.info("==================================================================");

        return new AuthResponse(true, genericSuccessMessage, null, null, null, null);
    }

    /**
     * Complete password reset using a verified reset token.
     * Revokes all active refresh tokens upon password reset.
     */
    @Transactional
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match. Please re-enter.");
        }

        if (request.getNewPassword().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long.");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired password reset token. Please request a new link."));

        if (resetToken.isUsed() || resetToken.isExpired()) {
            throw new IllegalArgumentException("Invalid or expired password reset token. Please request a new link.");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.resetFailedAttempts();
        userRepository.save(user);

        // Mark token as consumed
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Revoke all active sessions upon password reset for security
        if (refreshTokenRepository != null) {
            refreshTokenRepository.revokeAllUserTokens(user);
        }

        logger.info("Password reset successfully completed for User ID={}, Email='{}'", user.getId(), user.getEmail());

        return new AuthResponse(
                true,
                "Password has been reset successfully. Please log in with your new password.",
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name()
        );
    }

    /**
     * Authenticated password change workflow.
     */
    @Transactional
    public AuthResponse changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password does not match.");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New passwords do not match. Please re-enter.");
        }

        if (request.getNewPassword().length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters long.");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("New password cannot be identical to your current password.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke existing refresh tokens
        if (refreshTokenRepository != null) {
            refreshTokenRepository.revokeAllUserTokens(user);
        }

        logger.info("Password successfully changed for User ID={}, Email='{}'", user.getId(), user.getEmail());

        return new AuthResponse(
                true,
                "Password updated successfully. All other active sessions have been terminated.",
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name()
        );
    }

    /**
     * Retrieve the authenticated user's current profile.
     */
    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        Set<String> roleStrings = user.getRoles() != null
                ? user.getRoles().stream().map(Enum::name).collect(Collectors.toSet())
                : new HashSet<>();
        if (user.getRole() != null) {
            roleStrings.add(user.getRole().name());
        }

        return new UserProfileDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                roleStrings,
                user.isEmailVerified(),
                user.isAccountNonLocked(),
                user.isEnabled(),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null
        );
    }
}
