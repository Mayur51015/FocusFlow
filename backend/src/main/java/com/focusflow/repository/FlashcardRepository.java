package com.focusflow.repository;

import com.focusflow.entity.Flashcard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {

    List<Flashcard> findByTopicId(Long topicId);

    List<Flashcard> findByNextReviewDateLessThanEqualOrderByNextReviewDateAsc(LocalDate date);

    long countByTopicId(Long topicId);
}

