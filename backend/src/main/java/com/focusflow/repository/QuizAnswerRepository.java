package com.focusflow.repository;

import com.focusflow.entity.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {

    List<QuizAnswer> findByAttemptId(Long attemptId);

    @Query("SELECT qa FROM QuizAnswer qa WHERE qa.question.topic.id = :topicId")
    List<QuizAnswer> findByTopicId(@Param("topicId") Long topicId);

    @Query("SELECT COUNT(qa) FROM QuizAnswer qa WHERE qa.question.topic.id = :topicId")
    long countByTopicId(@Param("topicId") Long topicId);

    @Query("SELECT COUNT(qa) FROM QuizAnswer qa WHERE qa.question.topic.id = :topicId AND qa.isCorrect = true")
    long countCorrectByTopicId(@Param("topicId") Long topicId);
}

