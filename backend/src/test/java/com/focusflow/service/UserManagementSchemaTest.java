package com.focusflow.service;

import com.focusflow.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying Phase 1 User Management, RBAC roles, and academic entity ownership.
 */
class UserManagementSchemaTest {

    @Test
    @DisplayName("User entity should initialize with ROLE_USER, enabled, and unlocked status")
    void testUserInitialization() {
        User user = new User("student@example.com", "hashedPass", "Alice Student");

        assertEquals("student@example.com", user.getEmail());
        assertEquals("hashedPass", user.getPasswordHash());
        assertEquals("Alice Student", user.getFullName());
        assertEquals(Role.ROLE_USER, user.getRole());
        assertTrue(user.hasRole(Role.ROLE_USER));
        assertTrue(user.isEnabled());
        assertTrue(user.isAccountNonLocked());
        assertEquals(0, user.getFailedAttemptCount());
        assertTrue(user.isEmailVerified());
    }

    @Test
    @DisplayName("User lockout counters should increment and reset properly")
    void testUserLockoutManagement() {
        User user = new User("locked@example.com", "hash", "Bob Student");
        assertEquals(0, user.getFailedAttemptCount());

        user.incrementFailedAttempts();
        user.incrementFailedAttempts();
        assertEquals(2, user.getFailedAttemptCount());

        user.setAccountNonLocked(false);
        user.setLockTime(LocalDateTime.now());
        assertFalse(user.isAccountNonLocked());
        assertNotNull(user.getLockTime());

        user.resetFailedAttempts();
        assertEquals(0, user.getFailedAttemptCount());
        assertTrue(user.isAccountNonLocked());
        assertNull(user.getLockTime());
    }

    @Test
    @DisplayName("Subject and Topic should establish verified owner relationship")
    void testSubjectAndTopicOwnership() {
        User owner = new User("owner@example.com", "hash", "Charlie Owner");
        owner.setId(10L);

        Subject subject = new Subject("Computer Networks", LocalDate.now().plusDays(14), Importance.HIGH, owner);
        subject.setId(101L);

        assertEquals(owner, subject.getUser());
        assertEquals(10L, subject.getUserId());

        Topic topic = new Topic("TCP Handshake", subject, Difficulty.MEDIUM, 45, false);
        topic.setId(501L);

        assertEquals(subject, topic.getSubject());
        assertEquals(owner, topic.getUser());
        assertEquals(10L, topic.getUserId());
    }

    @Test
    @DisplayName("StudySession should establish direct and fallback verified owner")
    void testStudySessionOwnership() {
        User owner = new User("owner@example.com", "hash", "Charlie Owner");
        owner.setId(10L);

        Subject subject = new Subject("Computer Networks", LocalDate.now().plusDays(14), Importance.HIGH, owner);
        Topic topic = new Topic("TCP Handshake", subject, Difficulty.MEDIUM, 45, false);

        // Fallback to topic's subject owner
        StudySession session1 = new StudySession(topic, LocalDate.now(), 60, 0, SessionStatus.PLANNED);
        assertEquals(owner, session1.getUser());
        assertEquals(10L, session1.getUserId());

        // Explicit user ownership
        User customUser = new User("custom@example.com", "hash", "Dave Student");
        customUser.setId(25L);

        StudySession session2 = new StudySession(topic, LocalDate.now(), 60, 0, SessionStatus.PLANNED, customUser);
        assertEquals(customUser, session2.getUser());
        assertEquals(25L, session2.getUserId());
    }

    @Test
    @DisplayName("PasswordResetToken should accurately compute expiration status")
    void testPasswordResetTokenExpiration() {
        User user = new User("student@example.com", "hash", "Alice Student");
        LocalDateTime pastExpiry = LocalDateTime.now().minusHours(1);
        LocalDateTime futureExpiry = LocalDateTime.now().plusHours(2);

        PasswordResetToken expiredToken = new PasswordResetToken(user, "hash-expired", pastExpiry);
        assertTrue(expiredToken.isExpired());
        assertFalse(expiredToken.isUsed());

        PasswordResetToken validToken = new PasswordResetToken(user, "hash-valid", futureExpiry);
        assertFalse(validToken.isExpired());
    }

    @Test
    @DisplayName("LoginAttempt and AuditLog entities should preserve security and event audit metadata")
    void testSecurityEntities() {
        LoginAttempt attempt = new LoginAttempt("target@example.com", "192.168.1.100", false, "Bad credentials");
        assertEquals("target@example.com", attempt.getEmail());
        assertEquals("192.168.1.100", attempt.getIpAddress());
        assertFalse(attempt.isSuccess());
        assertEquals("Bad credentials", attempt.getFailureReason());

        AuditLog log = new AuditLog(1L, "admin@focusflow.com", "ACCOUNT_STATUS_CHANGE", "User", "55", "Disabled account", "127.0.0.1");
        assertEquals(1L, log.getAdminUserId());
        assertEquals("admin@focusflow.com", log.getAdminEmail());
        assertEquals("ACCOUNT_STATUS_CHANGE", log.getAction());
        assertEquals("User", log.getTargetEntity());
        assertEquals("55", log.getTargetId());
        assertEquals("Disabled account", log.getDetails());
    }
}

