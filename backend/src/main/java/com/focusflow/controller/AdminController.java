package com.focusflow.controller;

import com.focusflow.dto.auth.UserProfileDto;
import com.focusflow.entity.AuditLog;
import com.focusflow.entity.LoginAttempt;
import com.focusflow.entity.Role;
import com.focusflow.entity.User;
import com.focusflow.repository.AuditLogRepository;
import com.focusflow.repository.LoginAttemptRepository;
import com.focusflow.repository.StudySessionRepository;
import com.focusflow.repository.SubjectRepository;
import com.focusflow.repository.UserRepository;
import com.focusflow.security.UserPrincipal;
import com.focusflow.service.AuditService;
import com.focusflow.service.TrafficMetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST Controller for Administrator operations.
 * Strictly protected with @PreAuthorize("hasRole('ROLE_ADMIN')").
 * Powers the 6 Admin Dashboard modules: Overview, User Management,
 * Traffic Analytics, Service Monitoring, Security Controls, and Audit Logs.
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"}, allowCredentials = "true")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final StudySessionRepository studySessionRepository;
    private final AuditLogRepository auditLogRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final AuditService auditService;
    private final TrafficMetricsService trafficMetricsService;
    private final PasswordEncoder passwordEncoder;

    public AdminController(UserRepository userRepository,
                           SubjectRepository subjectRepository,
                           StudySessionRepository studySessionRepository,
                           AuditLogRepository auditLogRepository,
                           LoginAttemptRepository loginAttemptRepository,
                           AuditService auditService,
                           TrafficMetricsService trafficMetricsService,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.studySessionRepository = studySessionRepository;
        this.auditLogRepository = auditLogRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.auditService = auditService;
        this.trafficMetricsService = trafficMetricsService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * GET /api/admin/overview - Platform-wide account & academic summary KPIs.
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        long totalUsers = userRepository.count();
        List<User> allUsers = userRepository.findAll();
        long activeUsers = allUsers.stream().filter(User::isEnabled).count();
        long disabledUsers = allUsers.stream().filter(u -> !u.isEnabled()).count();
        long lockedUsers = allUsers.stream().filter(u -> !u.isAccountNonLocked()).count();
        long adminCount = allUsers.stream().filter(u -> u.hasRole(Role.ROLE_ADMIN)).count();
        long studentCount = allUsers.stream().filter(u -> u.hasRole(Role.ROLE_USER) || u.hasRole(Role.ROLE_STUDENT)).count();

        long totalSubjects = subjectRepository.count();
        long totalStudySessions = studySessionRepository.count();

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalUsers", totalUsers);
        overview.put("activeUsers", activeUsers);
        overview.put("disabledUsers", disabledUsers);
        overview.put("lockedUsers", lockedUsers);
        overview.put("adminCount", adminCount);
        overview.put("studentCount", studentCount);
        overview.put("totalSubjects", totalSubjects);
        overview.put("totalStudySessions", totalStudySessions);
        overview.put("systemStatus", "HEALTHY");

        return ResponseEntity.ok(overview);
    }

    /**
     * GET /api/admin/users - List all registered users with complete profile metadata.
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserProfileDto>> listUsers() {
        List<UserProfileDto> users = userRepository.findAll().stream()
                .map(u -> {
                    Set<String> roles = u.getRoles() != null
                            ? u.getRoles().stream().map(Enum::name).collect(Collectors.toSet())
                            : new HashSet<>();
                    if (u.getRole() != null) {
                        roles.add(u.getRole().name());
                    }
                    return new UserProfileDto(
                            u.getId(),
                            u.getEmail(),
                            u.getFullName(),
                            u.getRole() != null ? u.getRole().name() : "ROLE_USER",
                            roles,
                            u.isEmailVerified(),
                            u.isAccountNonLocked(),
                            u.isEnabled(),
                            u.getCreatedAt() != null ? u.getCreatedAt().toString() : null
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(users);
    }

    /**
     * PATCH /api/admin/users/{id}/status - Toggle user active or account lock status.
     */
    @PatchMapping("/users/{id}/status")
    @Transactional
    public ResponseEntity<Map<String, Object>> toggleUserStatus(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Boolean accountNonLocked,
            @AuthenticationPrincipal UserPrincipal adminPrincipal) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        String details = "";
        if (enabled != null) {
            user.setEnabled(enabled);
            details += "Enabled set to " + enabled + "; ";
        }
        if (accountNonLocked != null) {
            user.setAccountNonLocked(accountNonLocked);
            if (accountNonLocked) {
                user.resetFailedAttempts();
            }
            details += "AccountNonLocked set to " + accountNonLocked + "; ";
        }

        userRepository.save(user);

        auditService.logAction(
                adminPrincipal != null ? adminPrincipal.getId() : null,
                adminPrincipal != null ? adminPrincipal.getUsername() : "ADMIN",
                "ACCOUNT_STATUS_CHANGE",
                "User",
                id.toString(),
                details,
                "127.0.0.1"
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User status updated: " + details,
                "userId", id,
                "enabled", user.isEnabled(),
                "accountNonLocked", user.isAccountNonLocked()
        ));
    }

    /**
     * PATCH /api/admin/users/{id}/role - Update user role (promote to admin / demote to student).
     */
    @PatchMapping("/users/{id}/role")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateUserRole(
            @PathVariable Long id,
            @RequestParam String role,
            @AuthenticationPrincipal UserPrincipal adminPrincipal) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        Role targetRole = Role.fromString(role);
        user.setRole(targetRole);
        user.addRole(targetRole);
        userRepository.save(user);

        auditService.logAction(
                adminPrincipal != null ? adminPrincipal.getId() : null,
                adminPrincipal != null ? adminPrincipal.getUsername() : "ADMIN",
                "ROLE_CHANGE",
                "User",
                id.toString(),
                "Assigned role: " + targetRole.name(),
                "127.0.0.1"
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Role updated to " + targetRole.name(),
                "userId", id,
                "role", targetRole.name()
        ));
    }

    /**
     * POST /api/admin/users/{id}/reset-password - Administrative reset of user password.
     */
    @PostMapping("/users/{id}/reset-password")
    @Transactional
    public ResponseEntity<Map<String, Object>> adminResetPassword(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal adminPrincipal) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        String temporaryPassword = "FocusFlow@" + (1000 + new Random().nextInt(9000)) + "!";
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setAccountNonLocked(true);
        user.resetFailedAttempts();
        userRepository.save(user);

        auditService.logAction(
                adminPrincipal != null ? adminPrincipal.getId() : null,
                adminPrincipal != null ? adminPrincipal.getUsername() : "ADMIN",
                "ADMIN_PASSWORD_RESET",
                "User",
                id.toString(),
                "Password administratively reset for " + user.getEmail(),
                "127.0.0.1"
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Temporary password generated successfully.",
                "userId", id,
                "temporaryPassword", temporaryPassword
        ));
    }

    /**
     * GET /api/admin/traffic - Real-time traffic analytics and API utilization.
     */
    @GetMapping("/traffic")
    public ResponseEntity<Map<String, Object>> getTraffic() {
        return ResponseEntity.ok(trafficMetricsService.getTrafficAnalytics());
    }

    /**
     * GET /api/admin/service-metrics - JVM memory, threads, and service health diagnostics.
     */
    @GetMapping("/service-metrics")
    public ResponseEntity<Map<String, Object>> getServiceMetrics() {
        return ResponseEntity.ok(trafficMetricsService.getServiceHealthMetrics());
    }

    /**
     * GET /api/admin/security - Security monitoring, failed login attempts, and lockout diagnostics.
     */
    @GetMapping("/security")
    public ResponseEntity<Map<String, Object>> getSecurityDetails() {
        List<LoginAttempt> attempts = loginAttemptRepository.findTop50ByOrderByAttemptedAtDesc();
        long failedAttempts = attempts.stream().filter(a -> !a.isSuccess()).count();
        long lockedAccounts = userRepository.findAll().stream().filter(u -> !u.isAccountNonLocked()).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recentAttempts", attempts);
        result.put("failedAttemptCount", failedAttempts);
        result.put("lockedAccountsCount", lockedAccounts);
        result.put("bruteForceAlert", failedAttempts >= 5);

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/admin/audit-logs - View chronological administrative and security audit trails.
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(auditLogRepository.findAllByOrderByCreatedAtDesc());
    }
}
