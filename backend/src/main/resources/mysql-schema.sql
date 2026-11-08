-- ============================================================
-- FocusFlow 2.0 Database Schema Script (MySQL)
-- Multi-User Academic Productivity Platform
-- ============================================================

CREATE DATABASE IF NOT EXISTS focusflow_db;
USE focusflow_db;

-- 1. Table: users
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'ROLE_USER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    failed_attempt_count INT NOT NULL DEFAULT 0,
    lock_time DATETIME NULL,
    email_verified BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,
    INDEX idx_users_email (email)
);

-- 2. Table: roles
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

INSERT IGNORE INTO roles (name) VALUES ('ROLE_USER'), ('ROLE_ADMIN');

-- 3. Table: user_roles
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE
);

-- 4. Table: refresh_tokens
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date DATETIME NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_refresh_token (token)
);

-- 5. Table: password_reset_tokens
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

-- 6. Table: login_attempts
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

-- 7. Table: audit_logs
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

-- 8. Table: subjects (Multi-user tenant isolated)
CREATE TABLE IF NOT EXISTS subjects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    exam_date DATE NOT NULL,
    importance VARCHAR(20) NOT NULL,
    CONSTRAINT fk_subjects_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_subjects_user (user_id)
);

-- 9. Table: topics (belongs to Subject and verified owner)
CREATE TABLE IF NOT EXISTS topics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    subject_id BIGINT NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    estimated_minutes INT NOT NULL,
    completed BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_topics_subject FOREIGN KEY (subject_id) 
        REFERENCES subjects(id) ON DELETE CASCADE,
    INDEX idx_topics_subject (subject_id)
);

-- 10. Table: study_sessions (Multi-user tenant isolated)
CREATE TABLE IF NOT EXISTS study_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    scheduled_date DATE NOT NULL,
    planned_minutes INT NOT NULL,
    actual_minutes INT DEFAULT 0,
    status VARCHAR(30) DEFAULT 'PLANNED',
    start_time DATETIME NULL,
    end_time DATETIME NULL,
    completed_at DATETIME NULL,
    is_rescheduled BOOLEAN DEFAULT FALSE,
    locked BOOLEAN DEFAULT FALSE,
    predicted_minutes INT NULL,
    prediction_source VARCHAR(50) NULL,
    is_revision BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_sessions_topic FOREIGN KEY (topic_id) 
        REFERENCES topics(id) ON DELETE CASCADE,
    INDEX idx_sessions_user_date (user_id, scheduled_date)
);

-- 11. Table: quizzes
CREATE TABLE IF NOT EXISTS quizzes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description TEXT NULL,
    subject_id BIGINT NOT NULL,
    topic_id BIGINT NULL,
    duration_minutes INT NOT NULL DEFAULT 15,
    total_marks INT NOT NULL DEFAULT 100,
    passing_marks INT NOT NULL DEFAULT 50,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_quizzes_subject FOREIGN KEY (subject_id) 
        REFERENCES subjects(id) ON DELETE CASCADE,
    CONSTRAINT fk_quizzes_topic FOREIGN KEY (topic_id) 
        REFERENCES topics(id) ON DELETE SET NULL
);

-- 12. Table: quiz_questions
CREATE TABLE IF NOT EXISTS quiz_questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quiz_id BIGINT NOT NULL,
    topic_id BIGINT NULL,
    question_text TEXT NOT NULL,
    marks INT NOT NULL DEFAULT 1,
    explanation TEXT NULL,
    CONSTRAINT fk_questions_quiz FOREIGN KEY (quiz_id) 
        REFERENCES quizzes(id) ON DELETE CASCADE,
    CONSTRAINT fk_questions_topic FOREIGN KEY (topic_id) 
        REFERENCES topics(id) ON DELETE SET NULL
);

-- 13. Table: quiz_options
CREATE TABLE IF NOT EXISTS quiz_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    option_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    option_order INT NOT NULL DEFAULT 1,
    CONSTRAINT fk_options_question FOREIGN KEY (question_id) 
        REFERENCES quiz_questions(id) ON DELETE CASCADE
);

-- 14. Table: quiz_attempts (Multi-user tenant isolated)
CREATE TABLE IF NOT EXISTS quiz_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    quiz_id BIGINT NOT NULL,
    score INT NOT NULL DEFAULT 0,
    percentage DOUBLE NOT NULL DEFAULT 0.0,
    passed BOOLEAN NOT NULL DEFAULT FALSE,
    started_at DATETIME NOT NULL,
    completed_at DATETIME NOT NULL,
    time_spent_seconds INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_attempts_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_attempts_quiz FOREIGN KEY (quiz_id) 
        REFERENCES quizzes(id) ON DELETE CASCADE,
    INDEX idx_attempts_user (user_id)
);

-- 15. Table: quiz_answers
CREATE TABLE IF NOT EXISTS quiz_answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    attempt_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    selected_option_id BIGINT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    marks_awarded INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_answers_attempt FOREIGN KEY (attempt_id) 
        REFERENCES quiz_attempts(id) ON DELETE CASCADE,
    CONSTRAINT fk_answers_question FOREIGN KEY (question_id) 
        REFERENCES quiz_questions(id) ON DELETE CASCADE,
    CONSTRAINT fk_answers_option FOREIGN KEY (selected_option_id) 
        REFERENCES quiz_options(id) ON DELETE SET NULL
);

-- 16. Table: topic_performances
CREATE TABLE IF NOT EXISTS topic_performances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    topic_id BIGINT NOT NULL UNIQUE,
    quiz_score_average DOUBLE NOT NULL DEFAULT 0.0,
    quizzes_taken INT NOT NULL DEFAULT 0,
    study_duration_deviation DOUBLE NOT NULL DEFAULT 0.0,
    sessions_completed INT NOT NULL DEFAULT 0,
    revision_count INT NOT NULL DEFAULT 0,
    performance_level VARCHAR(20) NOT NULL DEFAULT 'GOOD',
    weak BOOLEAN NOT NULL DEFAULT FALSE,
    last_evaluated_at DATETIME NOT NULL,
    CONSTRAINT fk_topic_perf_topic FOREIGN KEY (topic_id) 
        REFERENCES topics(id) ON DELETE CASCADE
);

-- 17. Table: revision_sessions (Multi-user tenant isolated)
CREATE TABLE IF NOT EXISTS revision_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    scheduled_date DATE NOT NULL,
    repetition_number INT NOT NULL DEFAULT 0,
    interval_days INT NOT NULL DEFAULT 1,
    easiness_factor DOUBLE NOT NULL DEFAULT 2.5,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    last_reviewed_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_revisions_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_revisions_topic FOREIGN KEY (topic_id) 
        REFERENCES topics(id) ON DELETE CASCADE,
    INDEX idx_revisions_user (user_id)
);

-- 18. Table: revision_histories
CREATE TABLE IF NOT EXISTS revision_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    revision_session_id BIGINT NOT NULL,
    rating VARCHAR(20) NOT NULL,
    reviewed_at DATETIME NOT NULL,
    interval_days INT NOT NULL,
    easiness_factor DOUBLE NOT NULL,
    repetition_number INT NOT NULL,
    CONSTRAINT fk_revhist_session FOREIGN KEY (revision_session_id) 
        REFERENCES revision_sessions(id) ON DELETE CASCADE
);

-- 19. Table: flashcards (Multi-user tenant isolated)
CREATE TABLE IF NOT EXISTS flashcards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    front_prompt TEXT NOT NULL,
    back_answer TEXT NOT NULL,
    hint VARCHAR(255) NULL,
    repetition_number INT NOT NULL DEFAULT 0,
    interval_days INT NOT NULL DEFAULT 1,
    easiness_factor DOUBLE NOT NULL DEFAULT 2.5,
    next_review_date DATE NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_flashcards_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_flashcards_topic FOREIGN KEY (topic_id) 
        REFERENCES topics(id) ON DELETE CASCADE,
    INDEX idx_flashcards_user (user_id)
);

-- 20. Table: flashcard_reviews
CREATE TABLE IF NOT EXISTS flashcard_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    flashcard_id BIGINT NOT NULL,
    rating VARCHAR(20) NOT NULL,
    reviewed_at DATETIME NOT NULL,
    time_spent_seconds INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_fcreviews_card FOREIGN KEY (flashcard_id) 
        REFERENCES flashcards(id) ON DELETE CASCADE
);

-- 21. Table: exam_readiness
CREATE TABLE IF NOT EXISTS exam_readiness (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_id BIGINT NOT NULL,
    readiness_score DOUBLE NOT NULL,
    readiness_level VARCHAR(20) NOT NULL,
    topic_completion_rate DOUBLE NOT NULL DEFAULT 0.0,
    average_quiz_score DOUBLE NOT NULL DEFAULT 0.0,
    weak_topic_count INT NOT NULL DEFAULT 0,
    weak_topic_ratio DOUBLE NOT NULL DEFAULT 0.0,
    revision_completion_rate DOUBLE NOT NULL DEFAULT 0.0,
    days_until_exam INT NOT NULL,
    prediction_source VARCHAR(50) NOT NULL DEFAULT 'RULE_BASED',
    recommendations TEXT NULL,
    evaluated_at DATETIME NOT NULL,
    CONSTRAINT fk_readiness_subject FOREIGN KEY (subject_id) 
        REFERENCES subjects(id) ON DELETE CASCADE
);
