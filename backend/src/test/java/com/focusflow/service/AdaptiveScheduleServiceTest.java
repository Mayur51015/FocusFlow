package com.focusflow.service;

import com.focusflow.dto.AdaptiveScheduleSummaryDto;
import com.focusflow.dto.RecalculateScheduleRequest;
import com.focusflow.entity.*;
import com.focusflow.repository.StudySessionRepository;
import com.focusflow.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AdaptiveScheduleServiceTest {

    @Mock
    private ScheduleService scheduleService;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private StudySessionRepository studySessionRepository;

    @InjectMocks
    private AdaptiveScheduleService adaptiveScheduleService;

    private Subject subjectMath;
    private Subject subjectPhysics;
    private Topic topicCalculus;
    private Topic topicVectors;
    private Topic topicMechanics;

    @BeforeEach
    void setUp() {
        LocalDate today = LocalDate.now();

        subjectMath = new Subject("Mathematics", today.plusDays(5), Importance.HIGH);
        subjectMath.setId(1L);

        subjectPhysics = new Subject("Physics", today.plusDays(10), Importance.MEDIUM);
        subjectPhysics.setId(2L);

        topicCalculus = new Topic("Calculus", subjectMath, Difficulty.HARD, 90, false);
        topicCalculus.setId(101L);

        topicVectors = new Topic("Vectors", subjectMath, Difficulty.MEDIUM, 60, false);
        topicVectors.setId(102L);

        topicMechanics = new Topic("Mechanics", subjectPhysics, Difficulty.MEDIUM, 60, false);
        topicMechanics.setId(103L);
    }

    @Test
    @DisplayName("Recalculate: Missed session is rescheduled with isRescheduled=true")
    void testRecalculate_MissedSessionIsRescheduled() {
        LocalDate today = LocalDate.now();

        StudySession missedSession = new StudySession(topicCalculus, today, 90, 0, SessionStatus.MISSED);
        missedSession.setId(1L);

        List<StudySession> currentSessions = new ArrayList<>(List.of(missedSession));
        when(studySessionRepository.findByScheduledDateOrderByIdAsc(today)).thenReturn(currentSessions);
        when(topicRepository.findByCompletedFalse()).thenReturn(List.of(topicCalculus, topicVectors));
        when(scheduleService.calculatePriorityScore(any(Topic.class), any(LocalDate.class))).thenReturn(25.0);

        List<StudySession> capturedSaved = new ArrayList<>();
        when(studySessionRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<StudySession> saved = inv.getArgument(0);
            capturedSaved.addAll(saved);
            currentSessions.addAll(saved);
            return saved;
        });

        RecalculateScheduleRequest request = new RecalculateScheduleRequest(today, 2.0); // 120 mins
        AdaptiveScheduleSummaryDto summary = adaptiveScheduleService.recalculateSchedule(request);

        assertNotNull(summary);
        assertEquals(1, summary.getMissedSessionsCount());
        assertTrue(capturedSaved.stream().anyMatch(s -> s.isRescheduled() && s.getTopic().getId().equals(101L)),
                "Topic from missed session must be rescheduled with isRescheduled=true");
    }

    @Test
    @DisplayName("Recalculate: Partially completed session schedules remaining duration")
    void testRecalculate_PartiallyCompletedSessionAllocatesRemainingDuration() {
        LocalDate today = LocalDate.now();

        // Topic requires 90 mins, student studied 40 mins
        StudySession partialSession = new StudySession(topicCalculus, today, 90, 40, SessionStatus.PARTIALLY_COMPLETED);
        partialSession.setId(2L);

        List<StudySession> currentSessions = new ArrayList<>(List.of(partialSession));
        when(studySessionRepository.findByScheduledDateOrderByIdAsc(today)).thenReturn(currentSessions);
        when(topicRepository.findByCompletedFalse()).thenReturn(List.of(topicCalculus));
        when(scheduleService.calculatePriorityScore(any(Topic.class), any(LocalDate.class))).thenReturn(30.0);

        List<StudySession> capturedSaved = new ArrayList<>();
        when(studySessionRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<StudySession> saved = inv.getArgument(0);
            capturedSaved.addAll(saved);
            currentSessions.addAll(saved);
            return saved;
        });

        RecalculateScheduleRequest request = new RecalculateScheduleRequest(today, 2.0); // 120 mins available, 40 committed
        AdaptiveScheduleSummaryDto summary = adaptiveScheduleService.recalculateSchedule(request);

        assertNotNull(summary);
        assertEquals(1, summary.getPartiallyCompletedCount());
        assertEquals(40, summary.getCommittedMinutes());
        // Remaining topic duration: 90 - 40 = 50 mins
        assertTrue(capturedSaved.stream().anyMatch(s -> s.getPlannedMinutes() == 50 && s.getTopic().getId().equals(101L)),
                "Should schedule remaining 50 minutes for the partially completed topic");
    }

    @Test
    @DisplayName("Recalculate: Completed and Locked sessions are strictly preserved")
    void testRecalculate_PreservesCompletedAndLockedSessions() {
        LocalDate today = LocalDate.now();

        StudySession completedSession = new StudySession(topicCalculus, today, 60, 60, SessionStatus.COMPLETED);
        completedSession.setId(10L);

        StudySession lockedSession = new StudySession(topicVectors, today, 45, 0, SessionStatus.PLANNED);
        lockedSession.setId(11L);
        lockedSession.setLocked(true);

        List<StudySession> currentSessions = new ArrayList<>(List.of(completedSession, lockedSession));
        when(studySessionRepository.findByScheduledDateOrderByIdAsc(today)).thenReturn(currentSessions);
        when(topicRepository.findByCompletedFalse()).thenReturn(List.of(topicMechanics));
        when(scheduleService.calculatePriorityScore(any(Topic.class), any(LocalDate.class))).thenReturn(20.0);

        // Available 2 hours (120 mins). Committed = 60 + 45 = 105 mins. Remaining capacity = 15 mins.
        RecalculateScheduleRequest request = new RecalculateScheduleRequest(today, 2.0);
        AdaptiveScheduleSummaryDto summary = adaptiveScheduleService.recalculateSchedule(request);

        assertNotNull(summary);
        assertEquals(1, summary.getCompletedSessionsCount());
        assertEquals(1, summary.getLockedSessionsCount());
        assertEquals(105, summary.getCommittedMinutes());

        // Verify deleteByScheduledDateAndStatusInAndLockedFalse was called to only purge unlocked planned sessions
        verify(studySessionRepository).deleteByScheduledDateAndStatusInAndLockedFalse(eq(today), anyList());
    }

    @Test
    @DisplayName("Recalculate: Respects daily capacity limit and splits topic when necessary")
    void testRecalculate_RespectsDailyCapacity() {
        LocalDate today = LocalDate.now();

        List<StudySession> currentSessions = new ArrayList<>();
        when(studySessionRepository.findByScheduledDateOrderByIdAsc(today)).thenReturn(currentSessions);
        when(topicRepository.findByCompletedFalse()).thenReturn(List.of(topicCalculus)); // 90 mins needed
        when(scheduleService.calculatePriorityScore(any(Topic.class), any(LocalDate.class))).thenReturn(35.0);

        List<StudySession> capturedSaved = new ArrayList<>();
        when(studySessionRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<StudySession> saved = inv.getArgument(0);
            capturedSaved.addAll(saved);
            currentSessions.addAll(saved);
            return saved;
        });

        // Only 0.75 hours (45 mins) available
        RecalculateScheduleRequest request = new RecalculateScheduleRequest(today, 0.75);
        AdaptiveScheduleSummaryDto summary = adaptiveScheduleService.recalculateSchedule(request);

        assertNotNull(summary);
        assertEquals(45, summary.getTotalCapacityMinutes());
        assertEquals(1, capturedSaved.size());
        assertEquals(45, capturedSaved.get(0).getPlannedMinutes(),
                "Session duration should be capped at 45 minutes due to available capacity limit");
    }
}
