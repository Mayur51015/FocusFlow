package com.focusflow.repository;

import com.focusflow.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findAllByOrderByCompletedAtDesc();

    List<QuizAttempt> findByQuizIdOrderByCompletedAtDesc(Long quizId);

    Optional<QuizAttempt> findTopByQuizIdOrderByScoreDesc(Long quizId);

    long countByQuizId(Long quizId);

    @org.springframework.data.jpa.repository.Query("SELECT qa FROM QuizAttempt qa WHERE qa.quiz.topic.id = :topicId ORDER BY qa.completedAt DESC")
    List<QuizAttempt> findByTopicIdOrderByCompletedAtDesc(@org.springframework.data.repository.query.Param("topicId") Long topicId);
}

