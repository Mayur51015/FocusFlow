package com.focusflow.service;

import com.focusflow.dto.auth.*;
import com.focusflow.entity.PasswordResetToken;
import com.focusflow.entity.RefreshToken;
import com.focusflow.entity.Role;
import com.focusflow.entity.User;
import com.focusflow.repository.PasswordResetTokenRepository;
import com.focusflow.repository.RefreshTokenRepository;
import com.focusflow.repository.UserRepository;
import com.focusflow.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService covering Student Registration, Login, Token Refresh,
 * Rate Limiting, Forgot/Reset Password, and Profile lookups.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private LoginRateLimiterService loginRateLimiterService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        lenient().when(passwordEncoder.encode(any())).thenReturn("$2a$10$mockHashedPasswordStringForTestingPurposesOnly");
    }

    @Test
    @DisplayName("Should successfully register a new student with BCrypt hashed password and ROLE_USER")
    void testRegisterStudent_Success() {
        RegisterRequest request = new RegisterRequest(
                "Jane Doe",
                "jane.doe@university.edu",
                "StrongPassword123!",
                "StrongPassword123!"
        );

        when(userRepository.existsByEmail("jane.doe@university.edu")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(101L);
            return u;
        });

        AuthResponse response = authService.registerStudent(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(101L, response.getUserId());
        assertEquals("jane.doe@university.edu", response.getEmail());
        assertEquals("Jane Doe", response.getFullName());
        assertEquals("ROLE_USER", response.getRole());
    }

    @Test
    @DisplayName("Should reject registration if email is already registered")
    void testRegisterStudent_DuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "Existing User",
                "existing@university.edu",
                "Password123!",
                "Password123!"
        );

        when(userRepository.existsByEmail("existing@university.edu")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.registerStudent(request));

        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("Should successfully authenticate student and issue access + refresh tokens")
    void testLogin_Success() {
        LoginRequest request = new LoginRequest("student@university.edu", "Password123!");
        User user = new User("student@university.edu", passwordEncoder.encode("Password123!"), "Student Jane", Role.ROLE_USER);
        user.setId(55L);

        RefreshToken mockRefreshToken = new RefreshToken(user, "mock-refresh-token-uuid", LocalDateTime.now().plusDays(7));

        when(loginRateLimiterService.isBlocked(anyString(), anyString())).thenReturn(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByEmail("student@university.edu")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken("student@university.edu", 55L, "Student Jane", "ROLE_USER"))
                .thenReturn("mock-access-token-jwt");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(mockRefreshToken);

        AuthService.AuthResult result = authService.login(request, "127.0.0.1");

        assertNotNull(result);
        assertEquals("mock-refresh-token-uuid", result.rawRefreshToken());
        AuthResponse response = result.authResponse();
        assertTrue(response.isSuccess());
        assertEquals("mock-access-token-jwt", response.getAccessToken());
        assertEquals(55L, response.getUserId());
        assertEquals("student@university.edu", response.getEmail());
        verify(loginRateLimiterService, times(1)).recordSuccessfulAttempt(eq("student@university.edu"), eq("127.0.0.1"), eq(user));
    }

    @Test
    @DisplayName("Should reject login when rate limited by brute force protection")
    void testLogin_RateLimited() {
        LoginRequest request = new LoginRequest("student@university.edu", "Password123!");
        when(loginRateLimiterService.isBlocked(anyString(), anyString())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login(request, "192.168.1.50"));

        assertTrue(ex.getMessage().contains("Account is temporarily locked"));
    }

    @Test
    @DisplayName("Should reject login when credentials are invalid and record failed attempt")
    void testLogin_BadCredentials() {
        LoginRequest request = new LoginRequest("student@university.edu", "WrongPassword");

        when(loginRateLimiterService.isBlocked(anyString(), anyString())).thenReturn(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login(request, "127.0.0.1"));

        assertEquals("Invalid email or password.", ex.getMessage());
        verify(loginRateLimiterService, times(1)).recordFailedAttempt(eq("student@university.edu"), eq("127.0.0.1"), eq("Bad credentials"));
    }

    @Test
    @DisplayName("Should successfully refresh access token and rotate refresh token")
    void testRefreshAccessToken_Success() {
        User user = new User("student@university.edu", "hash", "Student Jane", Role.ROLE_USER);
        user.setId(55L);

        RefreshToken oldToken = new RefreshToken(user, "old-token", LocalDateTime.now().plusDays(5));
        RefreshToken rotatedToken = new RefreshToken(user, "new-rotated-token", LocalDateTime.now().plusDays(7));

        when(refreshTokenService.findByToken("old-token")).thenReturn(Optional.of(oldToken));
        when(refreshTokenService.verifyExpiration(oldToken)).thenReturn(oldToken);
        when(refreshTokenService.rotateRefreshToken(oldToken)).thenReturn(rotatedToken);
        when(jwtService.generateAccessToken("student@university.edu", 55L, "Student Jane", "ROLE_USER"))
                .thenReturn("new-access-token-jwt");

        AuthService.AuthResult result = authService.refreshAccessToken("old-token");

        assertNotNull(result);
        assertEquals("new-rotated-token", result.rawRefreshToken());
        assertEquals("new-access-token-jwt", result.authResponse().getAccessToken());
    }

    @Test
    @DisplayName("Should revoke refresh token on logout")
    void testLogout_Success() {
        authService.logout("valid-refresh-token");
        verify(refreshTokenService, times(1)).revokeToken("valid-refresh-token");
    }

    @Test
    @DisplayName("Should return generic success response on forgot-password without revealing user existence")
    void testForgotPassword_GenericResponse() {
        ForgotPasswordRequest requestNonExistent = new ForgotPasswordRequest("ghost@example.com");
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        AuthResponse respGhost = authService.forgotPassword(requestNonExistent, "127.0.0.1");
        assertTrue(respGhost.isSuccess());
        assertTrue(respGhost.getMessage().contains("password reset instructions"));

        User user = new User("alice@example.com", "hash", "Alice Student");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        AuthResponse respUser = authService.forgotPassword(new ForgotPasswordRequest("alice@example.com"), "127.0.0.1");
        assertTrue(respUser.isSuccess());
        assertEquals(respGhost.getMessage(), respUser.getMessage());
        verify(passwordResetTokenRepository, times(1)).save(any(PasswordResetToken.class));
    }

    @Test
    @DisplayName("Should successfully reset password and revoke active sessions")
    void testResetPassword_Success() {
        User user = new User("alice@example.com", "oldHash", "Alice Student");
        PasswordResetToken token = new PasswordResetToken(user, "valid-token-hash", LocalDateTime.now().plusMinutes(20));

        when(passwordResetTokenRepository.findByTokenHash("valid-token-hash")).thenReturn(Optional.of(token));

        ResetPasswordRequest request = new ResetPasswordRequest("valid-token-hash", "NewSecurePassword123!", "NewSecurePassword123!");
        AuthResponse response = authService.resetPassword(request);

        assertTrue(response.isSuccess());
        assertTrue(token.isUsed());
        verify(refreshTokenRepository, times(1)).revokeAllUserTokens(user);
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Should retrieve user profile accurately for /api/auth/me")
    void testGetUserProfile_Success() {
        User user = new User("alice@example.com", "hash", "Alice Student", Role.ROLE_USER);
        user.setId(77L);

        when(userRepository.findById(77L)).thenReturn(Optional.of(user));

        UserProfileDto profile = authService.getUserProfile(77L);
        assertNotNull(profile);
        assertEquals(77L, profile.getId());
        assertEquals("alice@example.com", profile.getEmail());
        assertEquals("ROLE_USER", profile.getRole());
        assertTrue(profile.getRoles().contains("ROLE_USER"));
    }
}
