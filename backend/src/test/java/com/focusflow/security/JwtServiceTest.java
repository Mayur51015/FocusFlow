package com.focusflow.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", 900000L); // 15 mins
    }

    @Test
    @DisplayName("Should generate valid JWT access token and extract claims accurately")
    void testGenerateAndValidateToken() {
        String token = jwtService.generateAccessToken("student@university.edu", 42L, "Alice Student", "ROLE_USER");

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals("student@university.edu", jwtService.extractUsername(token));
        assertEquals(42L, jwtService.extractUserId(token));
    }

    @Test
    @DisplayName("Should detect expired JWT tokens")
    void testExpiredTokenValidation() {
        // Set expiration to -1000ms (already expired)
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", -1000L);
        String expiredToken = jwtService.generateAccessToken("student@university.edu", 42L, "Alice Student", "ROLE_USER");

        assertNotNull(expiredToken);
        assertFalse(jwtService.validateToken(expiredToken));
    }
}
