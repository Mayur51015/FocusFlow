package com.focusflow.repository;

import com.focusflow.entity.SessionStatus;
import com.focusflow.entity.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * Repository interface for StudySession entity.
 * Provides query methods for daily plans, weekly progress, history, and status filtering.
 */
@Repository
public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    // Retrieve sessions scheduled for a particular date
    List<StudySession> findByScheduledDateOrderByIdAsc(LocalDate scheduledDate);

    // Retrieve sessions between two dates (used for weekly progress analytics)
    List<StudySession> findByScheduledDateBetween(LocalDate startDate, LocalDate endDate);

    // Count sessions by date and status
    long countByScheduledDateAndStatus(LocalDate scheduledDate, SessionStatus status);

    // Delete existing sessions on a date (used when regenerating a schedule)
    void deleteByScheduledDate(LocalDate scheduledDate);

    // Delete only specific status sessions on a date (safeguards COMPLETED / IN_PROGRESS / LOCKED sessions)
    void deleteByScheduledDateAndStatusInAndLockedFalse(LocalDate scheduledDate, Collection<SessionStatus> statuses);

    // Retrieve study history for completed / partially completed sessions
    List<StudySession> findByStatusInOrderByScheduledDateDescIdDesc(Collection<SessionStatus> statuses);

    // Retrieve all sessions ordered chronologically for history
    List<StudySession> findAllByOrderByScheduledDateDescIdDesc();

    // Find sessions by topic ID
    List<StudySession> findByTopicId(Long topicId);
}
