package com.focusflow.service;

import com.focusflow.dto.StudyHistoryDto;
import com.focusflow.entity.SessionStatus;
import com.focusflow.entity.StudySession;
import com.focusflow.repository.StudySessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service handling study history retrieval and aggregation for analytics and ML training.
 */
@Service
@Transactional(readOnly = true)
public class StudyHistoryService {

    private final StudySessionRepository studySessionRepository;

    public StudyHistoryService(StudySessionRepository studySessionRepository) {
        this.studySessionRepository = studySessionRepository;
    }

    /**
     * Retrieve all historical study sessions ordered by scheduled date descending.
     */
    public List<StudyHistoryDto> getAllStudyHistory() {
        return studySessionRepository.findAllByOrderByScheduledDateDescIdDesc()
                .stream()
                .filter(Objects::nonNull)
                .map(StudyHistoryDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve completed and partially completed study sessions.
     */
    public List<StudyHistoryDto> getCompletedStudyHistory() {
        return studySessionRepository.findByStatusInOrderByScheduledDateDescIdDesc(
                List.of(SessionStatus.COMPLETED, SessionStatus.PARTIALLY_COMPLETED)
        ).stream()
                .filter(Objects::nonNull)
                .map(StudyHistoryDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve study history for a specific topic.
     */
    public List<StudyHistoryDto> getTopicHistory(Long topicId) {
        if (topicId == null) {
            return List.of();
        }
        return studySessionRepository.findByTopicId(topicId)
                .stream()
                .filter(Objects::nonNull)
                .map(StudyHistoryDto::fromEntity)
                .collect(Collectors.toList());
    }
}
