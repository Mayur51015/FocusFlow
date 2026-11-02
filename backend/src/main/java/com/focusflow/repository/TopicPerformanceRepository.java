package com.focusflow.repository;

import com.focusflow.entity.PerformanceLevel;
import com.focusflow.entity.TopicPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicPerformanceRepository extends JpaRepository<TopicPerformance, Long> {

    Optional<TopicPerformance> findByTopicId(Long topicId);

    List<TopicPerformance> findByPerformanceLevel(PerformanceLevel performanceLevel);

    List<TopicPerformance> findByTopicSubjectId(Long subjectId);

    void deleteByTopicId(Long topicId);
}

