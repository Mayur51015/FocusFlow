package com.focusflow.service;

import com.focusflow.dto.ActualDurationUpdateRequest;
import com.focusflow.dto.ScheduleRequest;
import com.focusflow.dto.SessionStatusUpdateRequest;
import com.focusflow.dto.TopicPredictionResponse;
import com.focusflow.entity.Difficulty;
import com.focusflow.entity.Importance;
import com.focusflow.entity.SessionStatus;
import com.focusflow.entity.StudySession;
import com.focusflow.entity.Topic;
import com.focusflow.exception.BadRequestException;
import com.focusflow.exception.ResourceNotFoundException;
import com.focusflow.repository.StudySessionRepository;
import com.focusflow.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ScheduleService implements the core Greedy Scheduling Algorithm for FocusFlow.
 * Enhanced for Phase 1 session tracking and Phase 4 ML-based duration prediction.
 */
@Service
@Transactional
public class ScheduleService {

    private final TopicRepository topicRepository;
    private final StudySessionRepository studySessionRepository;
    private final MlPredictionService mlPredictionService;

    public ScheduleService(TopicRepository topicRepository, StudySessionRepository studySessionRepository) {
        this(topicRepository, studySessionRepository, null);
    }

    @Autowired
    public ScheduleService(TopicRepository topicRepository,
                           StudySessionRepository studySessionRepository,
                           @Autowired(required = false) MlPredictionService mlPredictionService) {
        this.topicRepository = topicRepository;
        this.studySessionRepository = studySessionRepository;
        this.mlPredictionService = mlPredictionService;
    }

    /**
     * Generates a study schedule for the given date and available study hours.
     * Preserves any existing completed, in-progress, or locked sessions.
     */
    public List<StudySession> generateSchedule(ScheduleRequest request) {
        if (request == null) {
            throw new BadRequestException("Schedule request cannot be null");
        }
        LocalDate targetDate = request.getDate();
        if (targetDate == null) {
            throw new BadRequestException("Target schedule date is required");
        }

        int totalAvailableMinutes = (int) Math.round(request.getAvailableHours() * 60);
        if (totalAvailableMinutes <= 0) {
            throw new BadRequestException("Available study hours must be greater than zero");
        }

        // Step 1: Safeguard completed, in-progress, and locked sessions!
        // Delete ONLY unstarted, unlocked PLANNED sessions on targetDate to prevent duplicate generation
        studySessionRepository.deleteByScheduledDateAndStatusInAndLockedFalse(
                targetDate,
                List.of(SessionStatus.PLANNED)
        );
        studySessionRepository.flush();

        // Step 2: Account for study time already committed to preserved sessions on this date
        List<StudySession> preservedSessions = studySessionRepository.findByScheduledDateOrderByIdAsc(targetDate);
        int committedMinutes = preservedSessions.stream()
                .mapToInt(StudySession::getPlannedMinutes)
                .sum();
        int remainingMinutes = Math.max(0, totalAvailableMinutes - committedMinutes);

        // Track topic IDs that already have preserved sessions on this date
        Set<Long> alreadyScheduledTopicIds = preservedSessions.stream()
                .filter(s -> s.getTopic() != null && s.getTopic().getId() != null)
                .map(s -> s.getTopic().getId())
                .collect(Collectors.toSet());

        if (remainingMinutes <= 0) {
            return preservedSessions; // Available capacity already filled by preserved sessions
        }

        // Step 3: Retrieve all incomplete topics from database
        List<Topic> incompleteTopics = topicRepository.findByCompletedFalse();
        if (incompleteTopics.isEmpty()) {
            return preservedSessions; // No incomplete topics remain
        }

        // Step 4: Filter out topics whose subject exam date has already passed or are already in preserved sessions
        List<Topic> validTopics = new ArrayList<>();
        for (Topic topic : incompleteTopics) {
            if (topic.getId() != null && alreadyScheduledTopicIds.contains(topic.getId())) {
                continue; // Avoid duplicate scheduling of the same topic on the same date
            }
            if (topic.getSubject() != null && topic.getSubject().getExamDate() != null) {
                if (!topic.getSubject().getExamDate().isBefore(targetDate)) {
                    validTopics.add(topic);
                }
            }
        }

        if (validTopics.isEmpty()) {
            return preservedSessions;
        }

        // Step 5: Sort topics descending by Priority Score (Greedy Choice Property)
        validTopics.sort(Comparator.comparingDouble((Topic t) -> calculatePriorityScore(t, targetDate)).reversed());

        // Step 6: Allocate available study time greedily
        List<StudySession> newSessions = new ArrayList<>();

        for (Topic topic : validTopics) {
            if (remainingMinutes <= 0) {
                break; // Student's daily study limit reached
            }

            int baselineEstimate = topic.getEstimatedMinutes();
            if (baselineEstimate <= 0) {
                continue; // Skip invalid topic duration safely
            }

            // Phase 4: Predict realistic duration via ML Service (with graceful heuristic fallback)
            TopicPredictionResponse prediction = (mlPredictionService != null)
                    ? mlPredictionService.predictForTopic(topic)
                    : null;

            int targetDuration = (prediction != null && prediction.getPredictedMinutes() > 0)
                    ? prediction.getPredictedMinutes()
                    : baselineEstimate;

            int sessionDuration;
            if (targetDuration <= remainingMinutes) {
                // Topic fits fully into the remaining daily study time
                sessionDuration = targetDuration;
            } else {
                // Topic Splitting: topic takes longer than remaining time,
                // so we allocate whatever remaining study time is available.
                sessionDuration = remainingMinutes;
            }

            StudySession session = new StudySession(
                    topic,
                    targetDate,
                    sessionDuration,
                    0,
                    SessionStatus.PLANNED
            );

            if (prediction != null) {
                session.setPredictedMinutes(prediction.getPredictedMinutes());
                session.setPredictionSource(prediction.getPredictionSource());
            } else {
                session.setPredictedMinutes(baselineEstimate);
                session.setPredictionSource("HEURISTIC_FALLBACK");
            }

            newSessions.add(session);
            remainingMinutes -= sessionDuration;
        }

        // Save newly planned sessions
        if (!newSessions.isEmpty()) {
            studySessionRepository.saveAll(newSessions);
        }

        // Return all sessions scheduled for this date (preserved + new)
        return studySessionRepository.findByScheduledDateOrderByIdAsc(targetDate);
    }

    /**
     * Calculates the Priority Score for a topic.
     * Formula: (Urgency Score * 3) + (Importance Score * 2) + Difficulty Score
     */
    public double calculatePriorityScore(Topic topic, LocalDate currentDate) {
        if (topic == null || topic.getSubject() == null) {
            return 0.0;
        }
        LocalDate examDate = topic.getSubject().getExamDate();
        if (examDate == null || currentDate == null) {
            return 0.0;
        }
        double urgencyScore = calculateUrgencyScore(examDate, currentDate);
        double importanceScore = calculateImportanceScore(topic.getSubject().getImportance());
        double difficultyScore = calculateDifficultyScore(topic.getDifficulty());

        return (urgencyScore * 3.0) + (importanceScore * 2.0) + difficultyScore;
    }

    /**
     * Urgency Score: Subjects with closer exam dates receive higher priority.
     */
    private double calculateUrgencyScore(LocalDate examDate, LocalDate currentDate) {
        if (examDate == null || currentDate == null) {
            return 0.0;
        }
        long daysUntilExam = ChronoUnit.DAYS.between(currentDate, examDate);

        if (daysUntilExam < 0) {
            return 0.0; // Exam already passed
        } else if (daysUntilExam <= 3) {
            return 10.0; // Urgent: within 3 days
        } else if (daysUntilExam <= 7) {
            return 7.0;  // High urgency: within 1 week
        } else if (daysUntilExam <= 14) {
            return 5.0;  // Moderate urgency: within 2 weeks
        } else if (daysUntilExam <= 30) {
            return 3.0;  // Normal: within 1 month
        } else {
            return 1.0;  // Long term: more than 1 month away
        }
    }

    /**
     * Importance Score: High importance subjects receive higher weight.
     */
    private double calculateImportanceScore(Importance importance) {
        if (importance == null) return 1.0;
        return switch (importance) {
            case HIGH -> 3.0;
            case MEDIUM -> 2.0;
            case LOW -> 1.0;
        };
    }

    /**
     * Difficulty Score: Hard topics receive higher weight.
     */
    private double calculateDifficultyScore(Difficulty difficulty) {
        if (difficulty == null) return 1.0;
        return switch (difficulty) {
            case HARD -> 3.0;
            case MEDIUM -> 2.0;
            case EASY -> 1.0;
        };
    }

    /**
     * Fetch all sessions for a specific date.
     */
    public List<StudySession> getSessionsByDate(LocalDate date) {
        LocalDate queryDate = (date != null) ? date : LocalDate.now();
        return studySessionRepository.findByScheduledDateOrderByIdAsc(queryDate);
    }

    /**
     * Update session status, timestamps, and actual study duration.
     */
    public StudySession updateSessionStatus(Long sessionId, SessionStatusUpdateRequest request) {
        if (sessionId == null) {
            throw new BadRequestException("Session ID cannot be null");
        }
        if (request == null || request.getStatus() == null) {
            throw new BadRequestException("Session status is required");
        }
        if (request.getActualMinutes() < 0) {
            throw new BadRequestException("Actual minutes cannot be negative");
        }

        StudySession session = studySessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Study session not found with id: " + sessionId));

        SessionStatus newStatus = request.getStatus();
        session.setStatus(newStatus);

        LocalDateTime now = LocalDateTime.now();

        if (newStatus == SessionStatus.IN_PROGRESS) {
            if (session.getStartTime() == null) {
                session.setStartTime(request.getStartTime() != null ? request.getStartTime() : now);
            }
        } else if (newStatus == SessionStatus.COMPLETED || newStatus == SessionStatus.PARTIALLY_COMPLETED) {
            if (session.getEndTime() == null) {
                session.setEndTime(request.getEndTime() != null ? request.getEndTime() : now);
            }
            if (session.getCompletedAt() == null) {
                session.setCompletedAt(request.getCompletedAt() != null ? request.getCompletedAt() : now);
            }
            if (request.getActualMinutes() > 0) {
                session.setActualMinutes(request.getActualMinutes());
            } else if (newStatus == SessionStatus.COMPLETED && session.getActualMinutes() == 0) {
                // Default actual minutes to planned minutes if marked completed without explicit duration
                session.setActualMinutes(session.getPlannedMinutes());
            }
        } else if (newStatus == SessionStatus.MISSED) {
            session.setActualMinutes(0);
            if (session.getEndTime() == null) {
                session.setEndTime(now);
            }
        } else if (newStatus == SessionStatus.PLANNED) {
            session.setActualMinutes(0);
            session.setStartTime(null);
            session.setEndTime(null);
            session.setCompletedAt(null);
        }

        return studySessionRepository.save(session);
    }

    /**
     * Update only the actual study duration for a session.
     */
    public StudySession updateActualDuration(Long sessionId, ActualDurationUpdateRequest request) {
        if (sessionId == null) {
            throw new BadRequestException("Session ID cannot be null");
        }
        if (request == null || request.getActualMinutes() == null || request.getActualMinutes() < 0) {
            throw new BadRequestException("Actual study duration cannot be negative");
        }

        StudySession session = studySessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Study session not found with id: " + sessionId));

        session.setActualMinutes(request.getActualMinutes());
        return studySessionRepository.save(session);
    }

    /**
     * Toggle lock status on a session to prevent automatic rescheduling/deletion.
     */
    public StudySession toggleSessionLock(Long sessionId) {
        if (sessionId == null) {
            throw new BadRequestException("Session ID cannot be null");
        }
        StudySession session = studySessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Study session not found with id: " + sessionId));

        session.setLocked(!session.isLocked());
        return studySessionRepository.save(session);
    }

    /**
     * Delete a study session.
     */
    public void deleteSession(Long sessionId) {
        if (sessionId == null) {
            throw new BadRequestException("Session ID cannot be null");
        }
        StudySession session = studySessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Study session not found with id: " + sessionId));
        studySessionRepository.delete(session);
    }
}
