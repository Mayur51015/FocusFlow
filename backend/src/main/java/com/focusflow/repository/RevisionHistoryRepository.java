package com.focusflow.repository;

import com.focusflow.entity.RevisionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RevisionHistoryRepository extends JpaRepository<RevisionHistory, Long> {

    List<RevisionHistory> findAllByOrderByReviewedAtDesc();

    List<RevisionHistory> findByTopicIdOrderByReviewedAtDesc(Long topicId);

    List<RevisionHistory> findByTopicIdIn(java.util.Collection<Long> topicIds);

    List<RevisionHistory> findByRevisionSessionIdOrderByReviewedAtDesc(Long revisionSessionId);
}

