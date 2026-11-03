-- ============================================================
-- FocusFlow 2.0 Database Schema Migration (Phase 1)
-- Multi-User Account Management, RBAC & Academic Data Isolation
-- ============================================================

USE focusflow_db;

-- 1. Ensure Default Starter Student Account Exists (ID = 1)
INSERT IGNORE INTO users (id, email, password_hash, full_name, role, enabled, created_at)
VALUES (1, 'student@focusflow.com', '$2a$10$w85T9LkJ4mQ4uGzH2sPjyeqVf3lG4zZf2sN9tA3gWjQ1w5yK7pZ.u', 'Demo Student', 'ROLE_USER', TRUE, NOW());

-- 2. Update existing 'ROLE_STUDENT' users to standard 'ROLE_USER'
UPDATE users SET role = 'ROLE_USER' WHERE role = 'ROLE_STUDENT';

-- 3. Update users table with account status, lockout, and verification columns if missing
ALTER TABLE users 
    ADD COLUMN IF NOT EXISTS account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS failed_attempt_count INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS lock_time DATETIME NULL,
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT TRUE;

-- 4. Table: roles
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

INSERT IGNORE INTO roles (name) VALUES ('ROLE_USER'), ('ROLE_ADMIN');

-- 5. Table: user_roles (Multi-role RBAC mapping)
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE
);

-- Backfill user_roles from existing users table
INSERT IGNORE INTO user_roles (user_id, role)
SELECT id, role FROM users;

-- 6. Table: password_reset_tokens
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expiry_date DATETIME NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_pwd_reset_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_pwd_reset_token (token_hash)
);

-- 7. Table: login_attempts
CREATE TABLE IF NOT EXISTS login_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(150) NULL,
    ip_address VARCHAR(50) NULL,
    success BOOLEAN NOT NULL,
    failure_reason VARCHAR(100) NULL,
    attempted_at DATETIME NOT NULL,
    INDEX idx_login_attempt_email_time (email, attempted_at),
    INDEX idx_login_attempt_ip_time (ip_address, attempted_at)
);

-- 8. Table: audit_logs
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_user_id BIGINT NULL,
    admin_email VARCHAR(150) NULL,
    action VARCHAR(100) NOT NULL,
    target_entity VARCHAR(100) NULL,
    target_id VARCHAR(100) NULL,
    details VARCHAR(1000) NULL,
    ip_address VARCHAR(50) NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_audit_created_at (created_at),
    INDEX idx_audit_admin (admin_user_id),
    INDEX idx_audit_action (action)
);

-- ============================================================
-- 9. Introduce User Ownership Relationships (Zero Data Loss)
-- ============================================================

-- A. subjects -> users
ALTER TABLE subjects ADD COLUMN IF NOT EXISTS user_id BIGINT NULL;
UPDATE subjects SET user_id = 1 WHERE user_id IS NULL;
ALTER TABLE subjects MODIFY COLUMN user_id BIGINT NOT NULL;
CREATE INDEX IF NOT EXISTS idx_subjects_user ON subjects(user_id);

-- B. study_sessions -> users
ALTER TABLE study_sessions ADD COLUMN IF NOT EXISTS user_id BIGINT NULL;
UPDATE study_sessions SET user_id = 1 WHERE user_id IS NULL;
ALTER TABLE study_sessions MODIFY COLUMN user_id BIGINT NOT NULL;
CREATE INDEX IF NOT EXISTS idx_study_sessions_user ON study_sessions(user_id);

-- C. quiz_attempts -> users
ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS user_id BIGINT NULL;
UPDATE quiz_attempts SET user_id = 1 WHERE user_id IS NULL;
ALTER TABLE quiz_attempts MODIFY COLUMN user_id BIGINT NOT NULL;
CREATE INDEX IF NOT EXISTS idx_quiz_attempts_user ON quiz_attempts(user_id);

-- D. revision_sessions -> users
ALTER TABLE revision_sessions ADD COLUMN IF NOT EXISTS user_id BIGINT NULL;
UPDATE revision_sessions SET user_id = 1 WHERE user_id IS NULL;
ALTER TABLE revision_sessions MODIFY COLUMN user_id BIGINT NOT NULL;
CREATE INDEX IF NOT EXISTS idx_revision_sessions_user ON revision_sessions(user_id);

-- E. flashcards -> users
ALTER TABLE flashcards ADD COLUMN IF NOT EXISTS user_id BIGINT NULL;
UPDATE flashcards SET user_id = 1 WHERE user_id IS NULL;
ALTER TABLE flashcards MODIFY COLUMN user_id BIGINT NOT NULL;
CREATE INDEX IF NOT EXISTS idx_flashcards_user ON flashcards(user_id);

