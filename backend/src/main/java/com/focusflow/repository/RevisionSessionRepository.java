package com.focusflow.repository;

import com.focusflow.entity.RevisionSession;
import com.focusflow.entity.RevisionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RevisionSessionRepository extends JpaRepository<RevisionSession, Long> {

    // Retrieve revisions due on or before target date with matching status (Today & Overdue)
    List<RevisionSession> findByScheduledDateLessThanEqualAndStatusInOrderByScheduledDateAsc(
            LocalDate date, Collection<RevisionStatus> statuses
    );

    // User-scoped revisions due on or before date
    List<RevisionSession> findByUser_IdAndScheduledDateLessThanEqualAndStatusInOrderByScheduledDateAsc(
            Long userId, LocalDate date, Collection<RevisionStatus> statuses
    );

    default List<RevisionSession> findByUserIdAndScheduledDateLessThanEqualAndStatusInOrderByScheduledDateAsc(
            Long userId, LocalDate date, Collection<RevisionStatus> statuses) {
        return findByUser_IdAndScheduledDateLessThanEqualAndStatusInOrderByScheduledDateAsc(userId, date, statuses);
    }

    // Retrieve future upcoming revisions
    List<RevisionSession> findByScheduledDateGreaterThanAndStatusOrderByScheduledDateAsc(
            LocalDate date, RevisionStatus status
    );

    // User-scoped upcoming revisions
    List<RevisionSession> findByUser_IdAndScheduledDateGreaterThanAndStatusOrderByScheduledDateAsc(
            Long userId, LocalDate date, RevisionStatus status
    );

    default List<RevisionSession> findByUserIdAndScheduledDateGreaterThanAndStatusOrderByScheduledDateAsc(
            Long userId, LocalDate date, RevisionStatus status) {
        return findByUser_IdAndScheduledDateGreaterThanAndStatusOrderByScheduledDateAsc(userId, date, status);
    }

    // Find active (pending or overdue) revision for a topic to prevent duplicates
    Optional<RevisionSession> findFirstByTopicIdAndStatusInOrderByScheduledDateDesc(
            Long topicId, Collection<RevisionStatus> statuses
    );

    // User-scoped first active revision for topic
    Optional<RevisionSession> findFirstByUser_IdAndTopicIdAndStatusInOrderByScheduledDateDesc(
            Long userId, Long topicId, Collection<RevisionStatus> statuses
    );

    default Optional<RevisionSession> findFirstByUserIdAndTopicIdAndStatusInOrderByScheduledDateDesc(
            Long userId, Long topicId, Collection<RevisionStatus> statuses) {
        return findFirstByUser_IdAndTopicIdAndStatusInOrderByScheduledDateDesc(userId, topicId, statuses);
    }

    // Find all revisions for a specific topic
    List<RevisionSession> findByTopicIdOrderByScheduledDateAsc(Long topicId);

    // User-scoped revisions for a topic
    List<RevisionSession> findByUser_IdAndTopicIdOrderByScheduledDateAsc(Long userId, Long topicId);

    default List<RevisionSession> findByUserIdAndTopicIdOrderByScheduledDateAsc(Long userId, Long topicId) {
        return findByUser_IdAndTopicIdOrderByScheduledDateAsc(userId, topicId);
    }

    List<RevisionSession> findByTopicIdIn(Collection<Long> topicIds);

    List<RevisionSession> findByUser_IdAndTopicIdIn(Long userId, Collection<Long> topicIds);

    default List<RevisionSession> findByUserIdAndTopicIdIn(Long userId, Collection<Long> topicIds) {
        return findByUser_IdAndTopicIdIn(userId, topicIds);
    }

    // Count due revisions for dashboard KPIs
    long countByScheduledDateLessThanEqualAndStatusIn(LocalDate date, Collection<RevisionStatus> statuses);

    // User-scoped count
    long countByUser_IdAndScheduledDateLessThanEqualAndStatusIn(Long userId, LocalDate date, Collection<RevisionStatus> statuses);

    default long countByUserIdAndScheduledDateLessThanEqualAndStatusIn(Long userId, LocalDate date, Collection<RevisionStatus> statuses) {
        return countByUser_IdAndScheduledDateLessThanEqualAndStatusIn(userId, date, statuses);
    }

    // User-scoped lookup
    @Query("SELECT rs FROM RevisionSession rs WHERE rs.id = :id AND rs.user.id = :userId")
    Optional<RevisionSession> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
