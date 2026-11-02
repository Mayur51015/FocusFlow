package com.focusflow.repository;

import com.focusflow.entity.ExamReadiness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamReadinessRepository extends JpaRepository<ExamReadiness, Long> {

    Optional<ExamReadiness> findTopBySubjectIdOrderByEvaluatedAtDesc(Long subjectId);

    List<ExamReadiness> findBySubjectIdOrderByEvaluatedAtDesc(Long subjectId);

    List<ExamReadiness> findAllByOrderByEvaluatedAtDesc();

    void deleteBySubjectId(Long subjectId);
}

