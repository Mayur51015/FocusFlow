package com.focusflow.repository;

import com.focusflow.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findAllByOrderByCompletedAtDesc();

    List<QuizAttempt> findByUser_IdOrderByCompletedAtDesc(Long userId);

    default List<QuizAttempt> findByUserIdOrderByCompletedAtDesc(Long userId) {
        return findByUser_IdOrderByCompletedAtDesc(userId);
    }

    List<QuizAttempt> findByQuizIdOrderByCompletedAtDesc(Long quizId);

    List<QuizAttempt> findByUser_IdAndQuizIdOrderByCompletedAtDesc(Long userId, Long quizId);

    default List<QuizAttempt> findByUserIdAndQuizIdOrderByCompletedAtDesc(Long userId, Long quizId) {
        return findByUser_IdAndQuizIdOrderByCompletedAtDesc(userId, quizId);
    }

    Optional<QuizAttempt> findTopByQuizIdOrderByScoreDesc(Long quizId);

    Optional<QuizAttempt> findTopByUser_IdAndQuizIdOrderByScoreDesc(Long userId, Long quizId);

    default Optional<QuizAttempt> findTopByUserIdAndQuizIdOrderByScoreDesc(Long userId, Long quizId) {
        return findTopByUser_IdAndQuizIdOrderByScoreDesc(userId, quizId);
    }

    long countByQuizId(Long quizId);

    long countByUser_Id(Long userId);

    default long countByUserId(Long userId) {
        return countByUser_Id(userId);
    }

    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.quiz.topic.id = :topicId ORDER BY qa.completedAt DESC")
    List<QuizAttempt> findByTopicIdOrderByCompletedAtDesc(@Param("topicId") Long topicId);

    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.user.id = :userId AND qa.quiz.topic.id = :topicId ORDER BY qa.completedAt DESC")
    List<QuizAttempt> findByUserIdAndTopicIdOrderByCompletedAtDesc(@Param("userId") Long userId, @Param("topicId") Long topicId);
}
