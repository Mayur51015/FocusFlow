package com.focusflow.config;

import com.focusflow.entity.Role;
import com.focusflow.entity.User;
import com.focusflow.repository.UserRepository;
import com.focusflow.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Secure Admin Account Bootstrap Runner.
 * Provisions initial administrative access via environment variables or restricted CLI execution.
 * Avoids any publicly accessible admin registration routes and logs the provisioning event.
 */
@Component
@Order(1)
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Value("${focusflow.admin.email:admin@focusflow.com}")
    private String adminEmail;

    @Value("${focusflow.admin.password:AdminSecret123!}")
    private String adminPassword;

    @Value("${focusflow.admin.name:System Administrator}")
    private String adminName;

    public AdminBootstrapRunner(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        boolean adminExists = userRepository.findAll().stream()
                .anyMatch(u -> u.hasRole(Role.ROLE_ADMIN));

        if (!adminExists) {
            String cleanEmail = adminEmail != null ? adminEmail.toLowerCase().trim() : "admin@focusflow.com";
            User admin = userRepository.findByEmail(cleanEmail).orElse(null);

            if (admin == null) {
                admin = new User(cleanEmail, passwordEncoder.encode(adminPassword), adminName, Role.ROLE_ADMIN);
            }

            admin.setRole(Role.ROLE_ADMIN);
            admin.addRole(Role.ROLE_ADMIN);
            admin.setAccountNonLocked(true);
            admin.setEmailVerified(true);
            admin.setEnabled(true);

            User saved = userRepository.save(admin);
            auditService.logAction(
                    saved.getId(),
                    saved.getEmail(),
                    "ADMIN_BOOTSTRAP",
                    "User",
                    saved.getId().toString(),
                    "Initial administrator provisioned via environment configuration",
                    "127.0.0.1"
            );

            logger.info("==================================================================");
            logger.info("ADMIN BOOTSTRAP: Provisioned administrator: ID={}, Email='{}'", saved.getId(), saved.getEmail());
            logger.info("Role: ROLE_ADMIN | Multi-Factor Authentication (MFA) required before prod");
            logger.info("==================================================================");
        } else {
            logger.info("Administrator account already exists. Skipping bootstrap provisioning.");
        }
    }
}

