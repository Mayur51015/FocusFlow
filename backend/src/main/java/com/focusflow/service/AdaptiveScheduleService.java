package com.focusflow.service;

import com.focusflow.dto.AdaptiveScheduleSummaryDto;
import com.focusflow.dto.RecalculateScheduleRequest;
import com.focusflow.dto.TopicPredictionResponse;
import com.focusflow.entity.SessionStatus;
import com.focusflow.entity.StudySession;
import com.focusflow.entity.Topic;
import com.focusflow.exception.BadRequestException;
import com.focusflow.repository.StudySessionRepository;
import com.focusflow.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AdaptiveScheduleService handles dynamic schedule recalculation, detecting missed
 * or partially completed sessions and redistributing remaining study capacity while
 * strictly preserving completed and locked sessions.
 */
@Service
@Transactional
public class AdaptiveScheduleService {

    private final ScheduleService scheduleService;
    private final TopicRepository topicRepository;
    private final StudySessionRepository studySessionRepository;
    private final MlPredictionService mlPredictionService;

    public AdaptiveScheduleService(ScheduleService scheduleService,
                                   TopicRepository topicRepository,
                                   StudySessionRepository studySessionRepository) {
        this(scheduleService, topicRepository, studySessionRepository, null);
    }

    @Autowired
    public AdaptiveScheduleService(ScheduleService scheduleService,
                                   TopicRepository topicRepository,
                                   StudySessionRepository studySessionRepository,
                                   @Autowired(required = false) MlPredictionService mlPredictionService) {
        this.scheduleService = scheduleService;
        this.topicRepository = topicRepository;
        this.studySessionRepository = studySessionRepository;
        this.mlPredictionService = mlPredictionService;
    }

    /**
     * Recalculates the daily study schedule dynamically based on current progress.
     */
    public AdaptiveScheduleSummaryDto recalculateSchedule(RecalculateScheduleRequest request) {
        LocalDate targetDate = (request != null && request.getDate() != null)
                ? request.getDate()
                : LocalDate.now();

        // Step 1: Inspect all current sessions for targetDate
        List<StudySession> currentSessions = studySessionRepository.findByScheduledDateOrderByIdAsc(targetDate);

        // Classify existing sessions
        List<StudySession> completedSessions = new ArrayList<>();
        List<StudySession> partiallyCompletedSessions = new ArrayList<>();
        List<StudySession> missedSessions = new ArrayList<>();
        List<StudySession> inProgressSessions = new ArrayList<>();
        List<StudySession> lockedSessions = new ArrayList<>();

        for (StudySession session : currentSessions) {
            if (session.isLocked()) {
                lockedSessions.add(session);
            }
            if (session.getStatus() == SessionStatus.COMPLETED) {
                completedSessions.add(session);
            } else if (session.getStatus() == SessionStatus.PARTIALLY_COMPLETED) {
                partiallyCompletedSessions.add(session);
            } else if (session.getStatus() == SessionStatus.MISSED) {
                missedSessions.add(session);
            } else if (session.getStatus() == SessionStatus.IN_PROGRESS) {
                inProgressSessions.add(session);
            }
        }

        // Step 2: Determine total daily study capacity
        int totalCapacityMinutes;
        if (request != null && request.getAvailableHours() != null && request.getAvailableHours() > 0) {
            totalCapacityMinutes = (int) Math.round(request.getAvailableHours() * 60);
        } else if (!currentSessions.isEmpty()) {
            totalCapacityMinutes = currentSessions.stream()
                    .mapToInt(StudySession::getPlannedMinutes)
                    .sum();
        } else {
            totalCapacityMinutes = 180; // 3.0 hours default baseline
        }

        // Step 3: Calculate capacity consumed by preserved sessions
        // Preserved sessions include COMPLETED, PARTIALLY_COMPLETED, IN_PROGRESS, and non-duplicate LOCKED
        Set<Long> preservedSessionIds = new HashSet<>();
        int committedMinutes = 0;

        for (StudySession s : completedSessions) {
            preservedSessionIds.add(s.getId());
            committedMinutes += (s.getActualMinutes() > 0 ? s.getActualMinutes() : s.getPlannedMinutes());
        }

        for (StudySession s : partiallyCompletedSessions) {
            if (!preservedSessionIds.contains(s.getId())) {
                preservedSessionIds.add(s.getId());
                committedMinutes += (s.getActualMinutes() > 0 ? s.getActualMinutes() : s.getPlannedMinutes());
            }
        }

        for (StudySession s : inProgressSessions) {
            if (!preservedSessionIds.contains(s.getId())) {
                preservedSessionIds.add(s.getId());
                committedMinutes += s.getPlannedMinutes();
            }
        }

        for (StudySession s : lockedSessions) {
            if (!preservedSessionIds.contains(s.getId())) {
                preservedSessionIds.add(s.getId());
                committedMinutes += s.getPlannedMinutes();
            }
        }

        int remainingCapacityMinutes = Math.max(0, totalCapacityMinutes - committedMinutes);

        // Step 4: Delete unlocked PLANNED sessions to recalculate fresh allocations
        studySessionRepository.deleteByScheduledDateAndStatusInAndLockedFalse(
                targetDate,
                List.of(SessionStatus.PLANNED)
        );
        studySessionRepository.flush();

        // Step 5: Gather candidate topics for rescheduling
        Set<Long> alreadySatisfiedTopicIds = new HashSet<>();
        for (StudySession s : completedSessions) {
            if (s.getTopic() != null) {
                alreadySatisfiedTopicIds.add(s.getTopic().getId());
            }
        }
        for (StudySession s : lockedSessions) {
            if (s.getTopic() != null) {
                alreadySatisfiedTopicIds.add(s.getTopic().getId());
            }
        }
        for (StudySession s : inProgressSessions) {
            if (s.getTopic() != null) {
                alreadySatisfiedTopicIds.add(s.getTopic().getId());
            }
        }

        // Map topic -> needed duration for adaptive makeup
        Map<Topic, Integer> candidateTopics = new LinkedHashMap<>();

        // Priority A: Missed sessions whose topic is incomplete and exam date is valid
        for (StudySession missed : missedSessions) {
            Topic topic = missed.getTopic();
            if (topic != null && !topic.isCompleted() && !alreadySatisfiedTopicIds.contains(topic.getId())) {
                if (topic.getSubject() != null && topic.getSubject().getExamDate() != null
                        && !topic.getSubject().getExamDate().isBefore(targetDate)) {
                    candidateTopics.put(topic, topic.getEstimatedMinutes());
                }
            }
        }

        // Priority B: Partially completed sessions with remaining topic study time
        for (StudySession partial : partiallyCompletedSessions) {
            Topic topic = partial.getTopic();
            if (topic != null && !topic.isCompleted() && !alreadySatisfiedTopicIds.contains(topic.getId())) {
                if (topic.getSubject() != null && topic.getSubject().getExamDate() != null
                        && !topic.getSubject().getExamDate().isBefore(targetDate)) {
                    int remainingTopicTime = Math.max(15, topic.getEstimatedMinutes() - partial.getActualMinutes());
                    candidateTopics.put(topic, remainingTopicTime);
                }
            }
        }

        // Priority C: Other incomplete topics in database
        List<Topic> allIncompleteTopics = topicRepository.findByCompletedFalse();
        for (Topic topic : allIncompleteTopics) {
            if (!alreadySatisfiedTopicIds.contains(topic.getId()) && !candidateTopics.containsKey(topic)) {
                if (topic.getSubject() != null && topic.getSubject().getExamDate() != null
                        && !topic.getSubject().getExamDate().isBefore(targetDate)) {
                    candidateTopics.put(topic, topic.getEstimatedMinutes());
                }
            }
        }

        // Step 6: Sort candidate topics descending by Priority Score with adaptive makeup bonus
        List<Topic> sortedCandidates = new ArrayList<>(candidateTopics.keySet());
        sortedCandidates.sort((t1, t2) -> {
            double score1 = scheduleService.calculatePriorityScore(t1, targetDate);
            double score2 = scheduleService.calculatePriorityScore(t2, targetDate);

            // Adaptive bonus (+2.0) for topics that were missed or partially completed
            boolean isMakeup1 = missedSessions.stream().anyMatch(m -> m.getTopic() != null && m.getTopic().getId().equals(t1.getId()))
                    || partiallyCompletedSessions.stream().anyMatch(p -> p.getTopic() != null && p.getTopic().getId().equals(t1.getId()));
            boolean isMakeup2 = missedSessions.stream().anyMatch(m -> m.getTopic() != null && m.getTopic().getId().equals(t2.getId()))
                    || partiallyCompletedSessions.stream().anyMatch(p -> p.getTopic() != null && p.getTopic().getId().equals(t2.getId()));

            if (isMakeup1) score1 += 2.0;
            if (isMakeup2) score2 += 2.0;

            return Double.compare(score2, score1);
        });

        // Step 7: Allocate available remaining minutes greedily
        List<StudySession> newlyScheduledSessions = new ArrayList<>();
        int availableMinutesToAllocate = remainingCapacityMinutes;

        for (Topic topic : sortedCandidates) {
            if (availableMinutesToAllocate <= 0) {
                break;
            }

            int neededDuration = candidateTopics.getOrDefault(topic, topic.getEstimatedMinutes());
            int sessionDuration;

            if (neededDuration <= availableMinutesToAllocate) {
                sessionDuration = neededDuration;
            } else {
                // Topic Splitting
                sessionDuration = availableMinutesToAllocate;
            }

            boolean isRescheduledTopic = missedSessions.stream().anyMatch(m -> m.getTopic() != null && m.getTopic().getId().equals(topic.getId()))
                    || partiallyCompletedSessions.stream().anyMatch(p -> p.getTopic() != null && p.getTopic().getId().equals(topic.getId()));

            StudySession session = new StudySession(
                    topic,
                    targetDate,
                    sessionDuration,
                    0,
                    SessionStatus.PLANNED
            );
            session.setRescheduled(isRescheduledTopic);

            if (mlPredictionService != null) {
                TopicPredictionResponse prediction = mlPredictionService.predictForTopic(topic);
                session.setPredictedMinutes(prediction.getPredictedMinutes());
                session.setPredictionSource(prediction.getPredictionSource());
            } else {
                session.setPredictedMinutes(topic.getEstimatedMinutes());
                session.setPredictionSource("HEURISTIC_FALLBACK");
            }

            newlyScheduledSessions.add(session);
            availableMinutesToAllocate -= sessionDuration;
        }

        // Save newly allocated sessions
        if (!newlyScheduledSessions.isEmpty()) {
            studySessionRepository.saveAll(newlyScheduledSessions);
        }

        // Step 8: Retrieve all final sessions for the target date
        List<StudySession> finalSessions = studySessionRepository.findByScheduledDateOrderByIdAsc(targetDate);

        int rescheduledCount = (int) newlyScheduledSessions.stream().filter(StudySession::isRescheduled).count();

        String message = String.format(
                "Adaptive recalculation complete! Preserved %d completed/locked session(s), allocated %d mins across %d new session(s) (%d rescheduled from missed/partial).",
                preservedSessionIds.size(),
                remainingCapacityMinutes - availableMinutesToAllocate,
                newlyScheduledSessions.size(),
                rescheduledCount
        );

        return new AdaptiveScheduleSummaryDto(
                finalSessions,
                totalCapacityMinutes,
                committedMinutes,
                availableMinutesToAllocate,
                completedSessions.size(),
                partiallyCompletedSessions.size(),
                missedSessions.size(),
                rescheduledCount,
                lockedSessions.size(),
                message
        );
    }
}

