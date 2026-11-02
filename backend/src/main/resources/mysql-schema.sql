-- ============================================================
-- FocusFlow Database Schema Script (MySQL)
-- Run this script in MySQL Workbench or mysql CLI if setting up manually.
-- ============================================================

CREATE DATABASE IF NOT EXISTS focusflow_db;
USE focusflow_db;

-- 1. Table: subjects
CREATE TABLE IF NOT EXISTS subjects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    exam_date DATE NOT NULL,
    importance VARCHAR(20) NOT NULL
);

-- 2. Table: topics
CREATE TABLE IF NOT EXISTS topics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    subject_id BIGINT NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    estimated_minutes INT NOT NULL,
    completed BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_topics_subject FOREIGN KEY (subject_id) 
        REFERENCES subjects(id) ON DELETE CASCADE
);

-- 3. Table: study_sessions (MVP 1 Adaptive Planner)
CREATE TABLE IF NOT EXISTS study_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    CONSTRAINT fk_sessions_topic FOREIGN KEY (topic_id) 
        REFERENCES topics(id) ON DELETE CASCADE
);

-- 4. Table: quizzes (MVP 2 Phase 1)
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

-- 5. Table: quiz_questions (MVP 2 Phase 1)
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

-- 6. Table: quiz_options (MVP 2 Phase 1)
CREATE TABLE IF NOT EXISTS quiz_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    option_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    option_order INT NOT NULL DEFAULT 1,
    CONSTRAINT fk_options_question FOREIGN KEY (question_id) 
        REFERENCES quiz_questions(id) ON DELETE CASCADE
);

-- 7. Table: quiz_attempts (MVP 2 Phase 1)
CREATE TABLE IF NOT EXISTS quiz_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quiz_id BIGINT NOT NULL,
    score INT NOT NULL DEFAULT 0,
    percentage DOUBLE NOT NULL DEFAULT 0.0,
    passed BOOLEAN NOT NULL DEFAULT FALSE,
    started_at DATETIME NOT NULL,
    completed_at DATETIME NOT NULL,
    time_spent_seconds INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_attempts_quiz FOREIGN KEY (quiz_id) 
        REFERENCES quizzes(id) ON DELETE CASCADE
);

-- 8. Table: quiz_answers (MVP 2 Phase 1)
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

-- 9. Table: topic_performances (MVP 2 Phase 2)
CREATE TABLE IF NOT EXISTS topic_performances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    topic_id BIGINT NOT NULL UNIQUE,
    performance_level ENUM('STRONG', 'MODERATE', 'WEAK') NOT NULL,
    average_quiz_score DOUBLE NOT NULL DEFAULT 0.0,
    attempt_count INT NOT NULL DEFAULT 0,
    accuracy_percentage DOUBLE NOT NULL DEFAULT 0.0,
    total_study_minutes INT NOT NULL DEFAULT 0,
    prediction_source VARCHAR(50) NOT NULL DEFAULT 'RULE_BASED',
    confidence_score DOUBLE NOT NULL DEFAULT 1.0,
    recommendation TEXT NULL,
    last_evaluated_at DATETIME NOT NULL,
    CONSTRAINT fk_performances_topic FOREIGN KEY (topic_id) 
        REFERENCES topics(id) ON DELETE CASCADE
);

-- 10. Table: revision_sessions (MVP 2 Phase 3)
CREATE TABLE IF NOT EXISTS revision_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    topic_id BIGINT NOT NULL,
    scheduled_date DATE NOT NULL,
    repetition_number INT NOT NULL DEFAULT 0,
    interval_days INT NOT NULL DEFAULT 1,
    easiness_factor DOUBLE NOT NULL DEFAULT 2.5,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    last_reviewed_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_revisions_topic FOREIGN KEY (topic_id) 
        REFERENCES topics(id) ON DELETE CASCADE
);

-- 11. Table: revision_histories (MVP 2 Phase 3)
CREATE TABLE IF NOT EXISTS revision_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    revision_session_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    rating VARCHAR(20) NOT NULL,
    reviewed_at DATETIME NOT NULL,
    previous_interval_days INT NOT NULL,
    new_interval_days INT NOT NULL,
    previous_ef DOUBLE NOT NULL,
    new_ef DOUBLE NOT NULL,
    notes TEXT NULL,
    CONSTRAINT fk_revhist_session FOREIGN KEY (revision_session_id) 
        REFERENCES revision_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_revhist_topic FOREIGN KEY (topic_id) 
        REFERENCES topics(id) ON DELETE CASCADE
);

-- 12. Table: flashcards (MVP 2 Phase 3)
CREATE TABLE IF NOT EXISTS flashcards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    topic_id BIGINT NOT NULL,
    front_prompt TEXT NOT NULL,
    back_answer TEXT NOT NULL,
    hint VARCHAR(255) NULL,
    repetition_number INT NOT NULL DEFAULT 0,
    interval_days INT NOT NULL DEFAULT 1,
    easiness_factor DOUBLE NOT NULL DEFAULT 2.5,
    next_review_date DATE NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_flashcards_topic FOREIGN KEY (topic_id) 
        REFERENCES topics(id) ON DELETE CASCADE
);

-- 13. Table: flashcard_reviews (MVP 2 Phase 3)
CREATE TABLE IF NOT EXISTS flashcard_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    flashcard_id BIGINT NOT NULL,
    rating VARCHAR(20) NOT NULL,
    reviewed_at DATETIME NOT NULL,
    time_spent_seconds INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_fcreviews_card FOREIGN KEY (flashcard_id) 
        REFERENCES flashcards(id) ON DELETE CASCADE
);

-- 14. Table: exam_readiness (MVP 2 Phase 4)
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
