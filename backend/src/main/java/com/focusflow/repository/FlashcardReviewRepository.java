package com.focusflow.repository;

import com.focusflow.entity.FlashcardReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlashcardReviewRepository extends JpaRepository<FlashcardReview, Long> {

    List<FlashcardReview> findByFlashcardIdOrderByReviewedAtDesc(Long flashcardId);
}

