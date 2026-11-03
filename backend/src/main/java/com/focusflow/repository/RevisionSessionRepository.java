package com.focusflow.repository;

import com.focusflow.entity.RevisionSession;
import com.focusflow.entity.RevisionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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

    // Retrieve future upcoming revisions
    List<RevisionSession> findByScheduledDateGreaterThanAndStatusOrderByScheduledDateAsc(
            LocalDate date, RevisionStatus status
    );

    // Find active (pending or overdue) revision for a topic to prevent duplicates
    Optional<RevisionSession> findFirstByTopicIdAndStatusInOrderByScheduledDateDesc(
            Long topicId, Collection<RevisionStatus> statuses
    );

    // Find all revisions for a specific topic
    List<RevisionSession> findByTopicIdOrderByScheduledDateAsc(Long topicId);

    List<RevisionSession> findByTopicIdIn(Collection<Long> topicIds);

    // Count due revisions for dashboard KPIs
    long countByScheduledDateLessThanEqualAndStatusIn(LocalDate date, Collection<RevisionStatus> statuses);
}

