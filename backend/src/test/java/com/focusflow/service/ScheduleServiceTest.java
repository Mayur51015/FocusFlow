package com.focusflow.service;

import com.focusflow.dto.ActualDurationUpdateRequest;
import com.focusflow.dto.ScheduleRequest;
import com.focusflow.dto.SessionStatusUpdateRequest;
import com.focusflow.entity.*;
import com.focusflow.exception.BadRequestException;
import com.focusflow.repository.StudySessionRepository;
import com.focusflow.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ScheduleServiceTest {

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private StudySessionRepository studySessionRepository;

    @InjectMocks
    private ScheduleService scheduleService;

    private Subject subjectUrgent;
    private Subject subjectLater;
    private Topic hardTopicUrgent;
    private Topic easyTopicLater;

    @BeforeEach
    void setUp() {
        LocalDate today = LocalDate.now();

        // Subject with urgent exam (2 days away, HIGH importance)
        subjectUrgent = new Subject("Algorithms", today.plusDays(2), Importance.HIGH);
        subjectUrgent.setId(1L);

        // Subject with exam later (25 days away, LOW importance)
        subjectLater = new Subject("Web Basics", today.plusDays(25), Importance.LOW);
        subjectLater.setId(2L);

        hardTopicUrgent = new Topic("Dynamic Programming", subjectUrgent, Difficulty.HARD, 90, false);
        hardTopicUrgent.setId(101L);

        easyTopicLater = new Topic("HTML Elements", subjectLater, Difficulty.EASY, 30, false);
        easyTopicLater.setId(102L);
    }

    @Test
    @DisplayName("Priority Score: Urgent hard topic should score higher than later easy topic")
    void testCalculatePriorityScore() {
        LocalDate today = LocalDate.now();

        double scoreUrgent = scheduleService.calculatePriorityScore(hardTopicUrgent, today);
        double scoreLater = scheduleService.calculatePriorityScore(easyTopicLater, today);

        // Urgent: (10 * 3) + (3 * 2) + 3 = 30 + 6 + 3 = 39
        // Later:  (3 * 3) + (1 * 2) + 1  = 9 + 2 + 1 = 12
        assertTrue(scoreUrgent > scoreLater, "Urgent subject topic must have a higher priority score");
    }

    @Test
    @DisplayName("Priority Score: Handles null topic or subject defensively")
    void testCalculatePriorityScore_WithNulls() {
        LocalDate today = LocalDate.now();
        assertEquals(0.0, scheduleService.calculatePriorityScore(null, today));

        Topic topicNoSubject = new Topic("Untracked", null, Difficulty.EASY, 30, false);
        assertEquals(0.0, scheduleService.calculatePriorityScore(topicNoSubject, today));
    }

    @Test
    @DisplayName("Generate Schedule: Respects daily limit and splits topic when necessary")
    void testGenerateSchedule_WithTopicSplitting() {
        LocalDate today = LocalDate.now();

        when(topicRepository.findByCompletedFalse()).thenReturn(Arrays.asList(easyTopicLater, hardTopicUrgent));

        List<StudySession> scheduledSessions = new ArrayList<>();
        when(studySessionRepository.findByScheduledDateOrderByIdAsc(today)).thenReturn(scheduledSessions);
        when(studySessionRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<StudySession> saved = inv.getArgument(0);
            scheduledSessions.addAll(saved);
            return saved;
        });

        // Request 1 hour (60 minutes)
        ScheduleRequest request = new ScheduleRequest(today, 1.0);

        List<StudySession> sessions = scheduleService.generateSchedule(request);

        assertNotNull(sessions);
        assertFalse(sessions.isEmpty());

        // First topic scheduled should be the higher priority one (hardTopicUrgent)
        assertEquals(hardTopicUrgent.getId(), sessions.get(0).getTopic().getId());

        // Hard topic needs 90 mins, but only 60 mins available -> split into 60 mins session
        assertEquals(60, sessions.get(0).getPlannedMinutes());

        int totalPlanned = sessions.stream().mapToInt(StudySession::getPlannedMinutes).sum();
        assertEquals(60, totalPlanned);

        verify(studySessionRepository, times(1))
                .deleteByScheduledDateAndStatusInAndLockedFalse(eq(today), anyList());
    }

    @Test
    @DisplayName("Generate Schedule: Rejects null request")
    void testGenerateSchedule_RejectsNull() {
        assertThrows(BadRequestException.class, () -> scheduleService.generateSchedule(null));
        assertThrows(BadRequestException.class, () -> scheduleService.generateSchedule(new ScheduleRequest(null, 1.0)));
    }

    @Test
    @DisplayName("Generate Schedule: Preserves existing completed and locked sessions")
    void testGenerateSchedule_PreservesCompletedSessions() {
        LocalDate today = LocalDate.now();

        StudySession completedSession = new StudySession(hardTopicUrgent, today, 45, 45, SessionStatus.COMPLETED);
        completedSession.setId(999L);

        List<StudySession> currentSessions = new ArrayList<>();
        currentSessions.add(completedSession);

        when(studySessionRepository.findByScheduledDateOrderByIdAsc(today))
                .thenReturn(currentSessions);
        when(topicRepository.findByCompletedFalse()).thenReturn(List.of(easyTopicLater));
        when(studySessionRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<StudySession> saved = inv.getArgument(0);
            currentSessions.addAll(saved);
            return saved;
        });

        // Request 1.5 hours (90 mins total). 45 mins already completed -> 45 mins remaining
        ScheduleRequest request = new ScheduleRequest(today, 1.5);
        List<StudySession> result = scheduleService.generateSchedule(request);

        assertNotNull(result);
        assertTrue(result.contains(completedSession));
    }

    @Test
    @DisplayName("Update Status: Sets timestamps and defaults actual minutes on completion")
    void testUpdateSessionStatus_Completion() {
        StudySession session = new StudySession(hardTopicUrgent, LocalDate.now(), 60, 0, SessionStatus.PLANNED);
        session.setId(50L);

        when(studySessionRepository.findById(50L)).thenReturn(Optional.of(session));
        when(studySessionRepository.save(any(StudySession.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionStatusUpdateRequest request = new SessionStatusUpdateRequest(SessionStatus.COMPLETED, 0);
        StudySession updated = scheduleService.updateSessionStatus(50L, request);

        assertEquals(SessionStatus.COMPLETED, updated.getStatus());
        assertEquals(60, updated.getActualMinutes(), "Should default to planned minutes if actual is 0");
        assertNotNull(updated.getCompletedAt(), "completedAt timestamp must be set");
    }

    @Test
    @DisplayName("Update Status: Rejects negative actual duration")
    void testUpdateSessionStatus_RejectsNegativeMinutes() {
        SessionStatusUpdateRequest request = new SessionStatusUpdateRequest(SessionStatus.COMPLETED, -5);
        assertThrows(BadRequestException.class, () -> scheduleService.updateSessionStatus(1L, request));
    }

    @Test
    @DisplayName("Actual Duration: Rejects negative duration")
    void testUpdateActualDuration_RejectsNegativeMinutes() {
        ActualDurationUpdateRequest request = new ActualDurationUpdateRequest(-10);

        assertThrows(BadRequestException.class, () -> {
            scheduleService.updateActualDuration(1L, request);
        });
    }

    @Test
    @DisplayName("Session Locking: Toggles lock flag")
    void testToggleSessionLock() {
        StudySession session = new StudySession(hardTopicUrgent, LocalDate.now(), 60, 0, SessionStatus.PLANNED);
        session.setId(10L);
        session.setLocked(false);

        when(studySessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(studySessionRepository.save(any(StudySession.class))).thenAnswer(inv -> inv.getArgument(0));

        StudySession locked = scheduleService.toggleSessionLock(10L);
        assertTrue(locked.isLocked());

        StudySession unlocked = scheduleService.toggleSessionLock(10L);
        assertFalse(unlocked.isLocked());
    }
}
