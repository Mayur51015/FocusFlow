package com.focusflow.security;

import com.focusflow.config.AdminBootstrapRunner;
import com.focusflow.entity.*;
import com.focusflow.exception.ResourceNotFoundException;
import com.focusflow.repository.AuditLogRepository;
import com.focusflow.repository.ExamReadinessRepository;
import com.focusflow.repository.SubjectRepository;
import com.focusflow.repository.UserRepository;
import com.focusflow.service.AuditService;
import com.focusflow.service.SubjectService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests verifying Phase 3 Role-Based Access Control, Admin Bootstrap,
 * and Cross-User Data Isolation in SubjectService.
 */
@ExtendWith(MockitoExtension.class)
class RbacSecurityTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditService auditService;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private ExamReadinessRepository examReadinessRepository;

    @InjectMocks
    private AdminBootstrapRunner adminBootstrapRunner;

    @InjectMocks
    private SubjectService subjectService;

    @Test
    @DisplayName("AdminBootstrapRunner should provision admin with ROLE_ADMIN when no admin exists")
    void testAdminBootstrap_ProvisionsAdmin() {
        when(userRepository.findAll()).thenReturn(List.of());
        when(userRepository.findByEmail("admin@focusflow.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hashedAdminPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(99L);
            return u;
        });

        adminBootstrapRunner.run();

        verify(userRepository, times(1)).save(any(User.class));
        verify(auditService, times(1)).logAction(
                eq(99L),
                eq("admin@focusflow.com"),
                eq("ADMIN_BOOTSTRAP"),
                eq("User"),
                eq("99"),
                contains("Initial administrator provisioned"),
                anyString()
        );
    }

    @Test
    @DisplayName("AdminBootstrapRunner should skip provisioning when an admin already exists")
    void testAdminBootstrap_SkipsWhenAdminExists() {
        User existingAdmin = new User("existing.admin@focusflow.com", "hash", "Existing Admin", Role.ROLE_ADMIN);
        when(userRepository.findAll()).thenReturn(List.of(existingAdmin));

        adminBootstrapRunner.run();

        verify(userRepository, never()).save(any(User.class));
        verify(auditService, never()).logAction(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("SubjectService should isolate subjects by authenticated user ID")
    void testSubjectService_DataIsolation() {
        User studentA = new User("studentA@focusflow.com", "hash", "Student A", Role.ROLE_USER);
        studentA.setId(10L);

        Subject subjectA = new Subject("Discrete Math", LocalDate.now().plusDays(10), Importance.HIGH, studentA);
        subjectA.setId(101L);

        when(subjectRepository.findByUserId(10L)).thenReturn(List.of(subjectA));
        when(subjectRepository.findByIdAndUserId(101L, 10L)).thenReturn(Optional.of(subjectA));
        when(subjectRepository.findByIdAndUserId(101L, 20L)).thenReturn(Optional.empty());

        // Student A can access their own subject
        List<Subject> subjects = subjectService.getUserSubjects(10L);
        assertEquals(1, subjects.size());
        assertEquals("Discrete Math", subjects.get(0).getName());

        Subject found = subjectService.getSubjectById(101L, 10L);
        assertNotNull(found);
        assertEquals(101L, found.getId());

        // Student B attempting to access Student A's subject is denied (throws ResourceNotFoundException)
        assertThrows(ResourceNotFoundException.class, () -> subjectService.getSubjectById(101L, 20L));
    }
}

