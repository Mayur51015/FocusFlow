package com.focusflow.repository;

import com.focusflow.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByQuizIdOrderByIdAsc(Long quizId);

    List<Question> findByTopicId(Long topicId);

    long countByQuizId(Long quizId);
}

