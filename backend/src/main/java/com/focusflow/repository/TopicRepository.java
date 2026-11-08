package com.focusflow.repository;

import com.focusflow.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Topic entity.
 * Provides custom query methods for filtering by completion, subject, and user ownership.
 */
@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    // Find all topics belonging to a specific subject
    List<Topic> findBySubjectId(Long subjectId);

    // User-scoped subject topics
    @Query("SELECT t FROM Topic t WHERE t.subject.id = :subjectId AND t.subject.user.id = :userId")
    List<Topic> findBySubjectIdAndSubjectUserId(@Param("subjectId") Long subjectId, @Param("userId") Long userId);

    // Find all incomplete topics across the platform
    List<Topic> findByCompletedFalse();

    // User-scoped incomplete topics (used by the multi-user scheduling engine)
    @Query("SELECT t FROM Topic t WHERE t.subject.user.id = :userId AND t.completed = false")
    List<Topic> findBySubjectUserIdAndCompletedFalse(@Param("userId") Long userId);

    // User-scoped topic lookup
    @Query("SELECT t FROM Topic t WHERE t.id = :id AND t.subject.user.id = :userId")
    Optional<Topic> findByIdAndSubjectUserId(@Param("id") Long id, @Param("userId") Long userId);

    // Count topics by completion status
    long countByCompletedTrue();
    long countByCompletedFalse();

    // User-scoped completion counts
    @Query("SELECT COUNT(t) FROM Topic t WHERE t.subject.user.id = :userId AND t.completed = true")
    long countBySubjectUserIdAndCompletedTrue(@Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM Topic t WHERE t.subject.user.id = :userId AND t.completed = false")
    long countBySubjectUserIdAndCompletedFalse(@Param("userId") Long userId);
}
