package com.focusflow.repository;

import com.focusflow.entity.Flashcard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {

    List<Flashcard> findByTopicId(Long topicId);

    List<Flashcard> findByUser_IdAndTopicId(Long userId, Long topicId);

    default List<Flashcard> findByUserIdAndTopicId(Long userId, Long topicId) {
        return findByUser_IdAndTopicId(userId, topicId);
    }

    List<Flashcard> findByNextReviewDateLessThanEqualOrderByNextReviewDateAsc(LocalDate date);

    List<Flashcard> findByUser_IdAndNextReviewDateLessThanEqualOrderByNextReviewDateAsc(Long userId, LocalDate date);

    default List<Flashcard> findByUserIdAndNextReviewDateLessThanEqualOrderByNextReviewDateAsc(Long userId, LocalDate date) {
        return findByUser_IdAndNextReviewDateLessThanEqualOrderByNextReviewDateAsc(userId, date);
    }

    long countByTopicId(Long topicId);

    long countByUser_Id(Long userId);

    default long countByUserId(Long userId) {
        return countByUser_Id(userId);
    }

    @Query("SELECT fc FROM Flashcard fc WHERE fc.id = :id AND fc.user.id = :userId")
    Optional<Flashcard> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
