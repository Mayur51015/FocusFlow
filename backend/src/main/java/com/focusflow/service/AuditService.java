package com.focusflow.service;

import com.focusflow.entity.AuditLog;
import com.focusflow.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for logging sensitive administrative actions, role assignments,
 * account status alterations, and security events.
 */
@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void logAction(Long adminUserId, String adminEmail, String action, String targetEntity, String targetId, String details, String ipAddress) {
        AuditLog auditLog = new AuditLog(adminUserId, adminEmail, action, targetEntity, targetId, details, ipAddress);
        auditLogRepository.save(auditLog);
        logger.info("AUDIT LOG: Admin='{}' (ID={}) executed '{}' on {}[{}] - Details: {}",
                adminEmail, adminUserId, action, targetEntity, targetId, details);
    }
}

