package com.focusflow.service;

import com.focusflow.entity.LoginAttempt;
import com.focusflow.entity.User;
import com.focusflow.repository.LoginAttemptRepository;
import com.focusflow.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service providing login rate limiting, brute-force defense, and account lockout management.
 */
@Service
public class LoginRateLimiterService {

    private static final Logger logger = LoggerFactory.getLogger(LoginRateLimiterService.class);

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final int LOCKOUT_DURATION_MINUTES = 15;

    private final LoginAttemptRepository loginAttemptRepository;
    private final UserRepository userRepository;

    public LoginRateLimiterService(LoginAttemptRepository loginAttemptRepository, UserRepository userRepository) {
        this.loginAttemptRepository = loginAttemptRepository;
        this.userRepository = userRepository;
    }

    /**
     * Checks if the given email or IP address is currently blocked due to excessive failed attempts.
     */
    @Transactional(readOnly = true)
    public boolean isBlocked(String email, String ipAddress) {
        LocalDateTime window = LocalDateTime.now().minusMinutes(LOCKOUT_DURATION_MINUTES);

        if (email != null && !email.isBlank()) {
            int emailFailures = loginAttemptRepository.countByEmailAndSuccessFalseAndAttemptedAtAfter(email.toLowerCase().trim(), window);
            if (emailFailures >= MAX_FAILED_ATTEMPTS) {
                logger.warn("Rate limiter: Email '{}' has {} failed attempts in last {}m.", email, emailFailures, LOCKOUT_DURATION_MINUTES);
                return true;
            }
        }

        if (ipAddress != null && !ipAddress.isBlank() && !isLoopback(ipAddress)) {
            int ipFailures = loginAttemptRepository.countByIpAddressAndSuccessFalseAndAttemptedAtAfter(ipAddress, window);
            if (ipFailures >= MAX_FAILED_ATTEMPTS * 3) { // Higher threshold for shared NAT IPs
                logger.warn("Rate limiter: IP '{}' has {} failed attempts in last {}m.", ipAddress, ipFailures, LOCKOUT_DURATION_MINUTES);
                return true;
            }
        }

        return false;
    }

    private boolean isLoopback(String ip) {
        return "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip) || "localhost".equalsIgnoreCase(ip);
    }

    /**
     * Records a failed login attempt and locks account if threshold is reached.
     * Uses REQUIRES_NEW propagation to ensure recording commits even when caller throws an exception.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(String email, String ipAddress, String reason) {
        String cleanEmail = email != null ? email.toLowerCase().trim() : null;
        loginAttemptRepository.save(new LoginAttempt(cleanEmail, ipAddress, false, reason));

        if (cleanEmail != null) {
            userRepository.findByEmail(cleanEmail).ifPresent(user -> {
                user.incrementFailedAttempts();
                if (user.getFailedAttemptCount() >= MAX_FAILED_ATTEMPTS) {
                    user.setAccountNonLocked(false);
                    user.setLockTime(LocalDateTime.now());
                    logger.warn("Account for email '{}' has been temporarily locked for {} minutes.", cleanEmail, LOCKOUT_DURATION_MINUTES);
                }
                userRepository.save(user);
            });
        }
    }

    /**
     * Records a successful login attempt and resets failed counter.
     */
    @Transactional
    public void recordSuccessfulAttempt(String email, String ipAddress, User user) {
        String cleanEmail = email != null ? email.toLowerCase().trim() : null;
        loginAttemptRepository.save(new LoginAttempt(cleanEmail, ipAddress, true, null));

        if (user != null && (user.getFailedAttemptCount() > 0 || !user.isAccountNonLocked())) {
            user.resetFailedAttempts();
            userRepository.save(user);
        }
    }

    /**
     * Checks whether an existing lockout on the user has expired, and unlocks if so.
     */
    @Transactional
    public boolean checkAndReleaseLock(User user) {
        if (!user.isAccountNonLocked() && user.getLockTime() != null) {
            if (user.getLockTime().plusMinutes(LOCKOUT_DURATION_MINUTES).isBefore(LocalDateTime.now())) {
                user.resetFailedAttempts();
                userRepository.save(user);
                logger.info("Automatic unlock applied for user '{}' after lockout period elapsed.", user.getEmail());
                return true;
            }
            return false;
        }
        return true;
    }
}

