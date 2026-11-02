package com.focusflow.service;

import com.focusflow.dto.DashboardStatsDto;
import com.focusflow.dto.SubjectProgressDto;
import com.focusflow.dto.WeeklyProgressDto;
import com.focusflow.entity.SessionStatus;
import com.focusflow.entity.StudySession;
import com.focusflow.repository.StudySessionRepository;
import com.focusflow.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * Service class for computing Dashboard metrics and Weekly Progress Analytics.
 * Enhanced to support all Phase 1 session statuses.
 */
@Service
@Transactional(readOnly = true)
public class ProgressService {

    private final StudySessionRepository studySessionRepository;
    private final TopicRepository topicRepository;

    public ProgressService(StudySessionRepository studySessionRepository, TopicRepository topicRepository) {
        this.studySessionRepository = studySessionRepository;
        this.topicRepository = topicRepository;
    }

    /**
     * Retrieves summary numbers for the top dashboard cards.
     */
    public DashboardStatsDto getDashboardStats() {
        LocalDate today = LocalDate.now();
        List<StudySession> todaySessions = studySessionRepository.findByScheduledDateOrderByIdAsc(today);

        int totalPlannedMinutesToday = 0;
        long completedToday = 0;
        long pendingToday = 0;

        for (StudySession session : todaySessions) {
            totalPlannedMinutesToday += session.getPlannedMinutes();
            if (session.getStatus() == SessionStatus.COMPLETED) {
                completedToday++;
            } else if (session.getStatus() == SessionStatus.PLANNED || session.getStatus() == SessionStatus.IN_PROGRESS) {
                pendingToday++;
            }
        }

        // Calculate overall topic completion percentage across the entire system
        long totalTopics = topicRepository.count();
        long completedTopics = topicRepository.countByCompletedTrue();
        int overallPercentage = totalTopics > 0 ? (int) Math.round(((double) completedTopics / totalTopics) * 100) : 0;

        return new DashboardStatsDto(
                totalPlannedMinutesToday,
                todaySessions.size(),
                completedToday,
                pendingToday,
                overallPercentage
        );
    }

    /**
     * Calculates weekly study progress and subject-wise distribution.
     * Considers the current week from Monday through Sunday.
     */
    public WeeklyProgressDto getWeeklyProgress() {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        List<StudySession> weekSessions = studySessionRepository.findByScheduledDateBetween(startOfWeek, endOfWeek);

        int totalCompletedMinutes = 0;
        int totalPlannedMinutes = 0;
        long totalCompletedSessions = 0;

        Map<String, int[]> subjectMap = new HashMap<>(); // [0] = completedMinutes, [1] = completedSessions

        for (StudySession session : weekSessions) {
            totalPlannedMinutes += session.getPlannedMinutes();

            if (session.getStatus() == SessionStatus.COMPLETED || session.getStatus() == SessionStatus.PARTIALLY_COMPLETED) {
                int minutes = session.getActualMinutes() > 0 ? session.getActualMinutes() : session.getPlannedMinutes();
                totalCompletedMinutes += minutes;
                totalCompletedSessions++;

                String subjectName = (session.getTopic() != null && session.getTopic().getSubject() != null)
                        ? session.getTopic().getSubject().getName()
                        : "General";

                subjectMap.putIfAbsent(subjectName, new int[]{0, 0});
                subjectMap.get(subjectName)[0] += minutes;
                subjectMap.get(subjectName)[1]++;
            }
        }

        int progressPercentage = totalPlannedMinutes > 0
                ? (int) Math.min(100, Math.round(((double) totalCompletedMinutes / totalPlannedMinutes) * 100))
                : 0;

        List<SubjectProgressDto> subjectSummaries = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : subjectMap.entrySet()) {
            subjectSummaries.add(new SubjectProgressDto(
                    entry.getKey(),
                    entry.getValue()[0],
                    entry.getValue()[1]
            ));
        }

        // Sort subject summaries by most studied minutes first
        subjectSummaries.sort((a, b) -> Integer.compare(b.getCompletedMinutes(), a.getCompletedMinutes()));

        return new WeeklyProgressDto(
                totalCompletedMinutes,
                totalCompletedSessions,
                progressPercentage,
                subjectSummaries
        );
    }
}
