package com.focusflow.repository;

import com.focusflow.entity.SessionStatus;
import com.focusflow.entity.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for StudySession entity.
 * Provides query methods for daily plans, weekly progress, history, and status filtering
 * scoped by user ownership.
 */
@Repository
public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    // Retrieve sessions scheduled for a particular date
    List<StudySession> findByScheduledDateOrderByIdAsc(LocalDate scheduledDate);

    // User-scoped sessions for a date
    List<StudySession> findByUser_IdAndScheduledDateOrderByIdAsc(Long userId, LocalDate scheduledDate);

    default List<StudySession> findByUserIdAndScheduledDateOrderByIdAsc(Long userId, LocalDate scheduledDate) {
        return findByUser_IdAndScheduledDateOrderByIdAsc(userId, scheduledDate);
    }

    // Retrieve sessions between two dates (used for weekly progress analytics)
    List<StudySession> findByScheduledDateBetween(LocalDate startDate, LocalDate endDate);

    // User-scoped sessions between dates
    List<StudySession> findByUser_IdAndScheduledDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    default List<StudySession> findByUserIdAndScheduledDateBetween(Long userId, LocalDate startDate, LocalDate endDate) {
        return findByUser_IdAndScheduledDateBetween(userId, startDate, endDate);
    }

    // Count sessions by date and status
    long countByScheduledDateAndStatus(LocalDate scheduledDate, SessionStatus status);

    // User-scoped session counts
    long countByUser_IdAndScheduledDateAndStatus(Long userId, LocalDate scheduledDate, SessionStatus status);

    default long countByUserIdAndScheduledDateAndStatus(Long userId, LocalDate scheduledDate, SessionStatus status) {
        return countByUser_IdAndScheduledDateAndStatus(userId, scheduledDate, status);
    }

    // Delete existing sessions on a date (used when regenerating a schedule)
    void deleteByScheduledDate(LocalDate scheduledDate);

    // Delete only specific status sessions on a date (safeguards COMPLETED / IN_PROGRESS / LOCKED sessions)
    void deleteByScheduledDateAndStatusInAndLockedFalse(LocalDate scheduledDate, Collection<SessionStatus> statuses);

    // User-scoped schedule regeneration deletion
    void deleteByUser_IdAndScheduledDateAndStatusInAndLockedFalse(Long userId, LocalDate scheduledDate, Collection<SessionStatus> statuses);

    default void deleteByUserIdAndScheduledDateAndStatusInAndLockedFalse(Long userId, LocalDate scheduledDate, Collection<SessionStatus> statuses) {
        deleteByUser_IdAndScheduledDateAndStatusInAndLockedFalse(userId, scheduledDate, statuses);
    }

    // Retrieve study history for completed / partially completed sessions
    List<StudySession> findByStatusInOrderByScheduledDateDescIdDesc(Collection<SessionStatus> statuses);

    // User-scoped study history
    List<StudySession> findByUser_IdAndStatusInOrderByScheduledDateDescIdDesc(Long userId, Collection<SessionStatus> statuses);

    default List<StudySession> findByUserIdAndStatusInOrderByScheduledDateDescIdDesc(Long userId, Collection<SessionStatus> statuses) {
        return findByUser_IdAndStatusInOrderByScheduledDateDescIdDesc(userId, statuses);
    }

    // Retrieve all sessions ordered chronologically for history
    List<StudySession> findAllByOrderByScheduledDateDescIdDesc();

    // User-scoped history ordered
    List<StudySession> findAllByUser_IdOrderByScheduledDateDescIdDesc(Long userId);

    default List<StudySession> findAllByUserIdOrderByScheduledDateDescIdDesc(Long userId) {
        return findAllByUser_IdOrderByScheduledDateDescIdDesc(userId);
    }

    // Find sessions by topic ID
    List<StudySession> findByTopicId(Long topicId);

    // User-scoped sessions by topic ID
    List<StudySession> findByUser_IdAndTopicId(Long userId, Long topicId);

    default List<StudySession> findByUserIdAndTopicId(Long userId, Long topicId) {
        return findByUser_IdAndTopicId(userId, topicId);
    }

    // User-scoped session lookup
    @Query("SELECT ss FROM StudySession ss WHERE ss.id = :id AND ss.user.id = :userId")
    Optional<StudySession> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
