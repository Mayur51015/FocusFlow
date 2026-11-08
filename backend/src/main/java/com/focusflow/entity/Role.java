package com.focusflow.entity;

/**
 * Roles for student and administrator accounts in FocusFlow 2.0.
 * Standardized to ROLE_USER (students) and ROLE_ADMIN (administrators).
 * ROLE_STUDENT is retained as a backward-compatible alias.
 */
public enum Role {
    ROLE_USER,
    ROLE_ADMIN,
    ROLE_STUDENT;

    public static Role fromString(String roleName) {
        if (roleName == null) {
            return ROLE_USER;
        }
        String normalized = roleName.trim().toUpperCase();
        if ("ROLE_STUDENT".equals(normalized) || "STUDENT".equals(normalized)) {
            return ROLE_USER;
        }
        for (Role r : values()) {
            if (r.name().equalsIgnoreCase(normalized) || r.name().replace("ROLE_", "").equalsIgnoreCase(normalized)) {
                return r;
            }
        }
        return ROLE_USER;
    }
}
