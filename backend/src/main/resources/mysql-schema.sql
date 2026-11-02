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

-- 3. Table: study_sessions
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
    CONSTRAINT fk_sessions_topic FOREIGN KEY (topic_id) 
        REFERENCES topics(id) ON DELETE CASCADE
);
