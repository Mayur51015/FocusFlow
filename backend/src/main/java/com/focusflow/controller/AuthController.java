package com.focusflow.controller;

import com.focusflow.dto.auth.*;
import com.focusflow.security.UserPrincipal;
import com.focusflow.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

/**
 * REST Controller for FocusFlow 2.0 Authentication.
 * Handles Registration, Login, Token Refresh, Logout, Password Recovery,
 * Password Change, and Current User Profile (/me).
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"}, allowCredentials = "true")
public class AuthController {

    private final AuthService authService;

    @Value("${focusflow.security.cookie.secure:false}")
    private boolean cookieSecure;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/auth/register - Register a new student account.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerStudent(
            @Valid @RequestBody RegisterRequest request,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            String errorMessage = Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage();
            return ResponseEntity.badRequest().body(new AuthResponse(false, errorMessage, null, null, null, null));
        }

        try {
            AuthResponse response = authService.registerStudent(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new AuthResponse(false, ex.getMessage(), null, null, null, null));
        }
    }

    /**
     * POST /api/auth/login - Authenticate student credentials, issue access token and set HttpOnly refresh cookie.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        if (bindingResult.hasErrors()) {
            String errorMessage = Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage();
            return ResponseEntity.badRequest().body(new AuthResponse(false, errorMessage, null, null, null, null));
        }

        String clientIp = extractClientIp(httpRequest);

        try {
            AuthService.AuthResult result = authService.login(request, clientIp);
            setRefreshTokenCookie(response, result.rawRefreshToken(), 7 * 24 * 60 * 60);
            return ResponseEntity.ok(result.authResponse());
        } catch (IllegalArgumentException ex) {
            HttpStatus status = (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("locked"))
                    ? HttpStatus.LOCKED
                    : HttpStatus.UNAUTHORIZED;
            return ResponseEntity.status(status)
                    .body(new AuthResponse(false, ex.getMessage(), null, null, null, null));
        }
    }

    /**
     * POST /api/auth/refresh - Refresh short-lived access token and rotate refresh token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String cookieRefreshToken,
            @RequestBody(required = false) RefreshTokenRequest requestBody,
            HttpServletResponse response) {

        String token = cookieRefreshToken;
        if ((token == null || token.isBlank()) && requestBody != null) {
            token = requestBody.getRefreshToken();
        }

        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(false, "No refresh token provided. Please sign in.", null, null, null, null));
        }

        try {
            AuthService.AuthResult result = authService.refreshAccessToken(token);
            setRefreshTokenCookie(response, result.rawRefreshToken(), 7 * 24 * 60 * 60);
            return ResponseEntity.ok(result.authResponse());
        } catch (IllegalArgumentException ex) {
            clearRefreshTokenCookie(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(false, ex.getMessage(), null, null, null, null));
        }
    }

    /**
     * POST /api/auth/logout - Revoke refresh token and clear HttpOnly cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @CookieValue(name = "refreshToken", required = false) String cookieRefreshToken,
            @RequestBody(required = false) RefreshTokenRequest requestBody,
            HttpServletResponse response) {

        String token = cookieRefreshToken;
        if ((token == null || token.isBlank()) && requestBody != null) {
            token = requestBody.getRefreshToken();
        }

        if (token != null && !token.isBlank()) {
            authService.logout(token);
        }

        clearRefreshTokenCookie(response);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Logged out successfully. Session terminated."
        ));
    }

    /**
     * POST /api/auth/forgot-password - Request a password reset link without disclosing user existence.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<AuthResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest) {

        if (bindingResult.hasErrors()) {
            String errorMessage = Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage();
            return ResponseEntity.badRequest().body(new AuthResponse(false, errorMessage, null, null, null, null));
        }

        String clientIp = extractClientIp(httpRequest);
        AuthResponse response = authService.forgotPassword(request, clientIp);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/reset-password - Reset password using verified token.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            String errorMessage = Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage();
            return ResponseEntity.badRequest().body(new AuthResponse(false, errorMessage, null, null, null, null));
        }

        try {
            AuthResponse response = authService.resetPassword(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new AuthResponse(false, ex.getMessage(), null, null, null, null));
        }
    }

    /**
     * POST /api/auth/change-password - Change password for the currently authenticated user.
     */
    @PostMapping("/change-password")
    public ResponseEntity<AuthResponse> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request,
            BindingResult bindingResult,
            HttpServletResponse response) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(false, "Authentication required.", null, null, null, null));
        }

        if (bindingResult.hasErrors()) {
            String errorMessage = Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage();
            return ResponseEntity.badRequest().body(new AuthResponse(false, errorMessage, null, null, null, null));
        }

        try {
            AuthResponse authResponse = authService.changePassword(principal.getId(), request);
            clearRefreshTokenCookie(response);
            return ResponseEntity.ok(authResponse);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new AuthResponse(false, ex.getMessage(), null, null, null, null));
        }
    }

    /**
     * GET /api/auth/me - Retrieve current authenticated user profile.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(false, "User is not authenticated.", null, null, null, null));
        }

        try {
            UserProfileDto profile = authService.getUserProfile(principal.getId());
            return ResponseEntity.ok(profile);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AuthResponse(false, ex.getMessage(), null, null, null, null));
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/auth")
                .maxAge(maxAgeSeconds)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/auth")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
