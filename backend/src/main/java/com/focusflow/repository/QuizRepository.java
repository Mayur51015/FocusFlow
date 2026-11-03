package com.focusflow.repository;

import com.focusflow.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findAllByOrderByIdDesc();

    List<Quiz> findBySubjectIdOrderByIdDesc(Long subjectId);

    List<Quiz> findByTopicIdOrderByIdDesc(Long topicId);

    List<Quiz> findBySubjectIdAndTopicIdOrderByIdDesc(Long subjectId, Long topicId);
}

