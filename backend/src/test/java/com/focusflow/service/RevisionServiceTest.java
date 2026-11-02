package com.focusflow.service;

import com.focusflow.dto.RevisionReviewRequest;
import com.focusflow.dto.RevisionSessionDto;
import com.focusflow.entity.*;
import com.focusflow.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RevisionServiceTest {

    @Mock
    private RevisionSessionRepository revisionSessionRepository;

    @Mock
    private RevisionHistoryRepository revisionHistoryRepository;

    @Mock
    private FlashcardRepository flashcardRepository;

    @Mock
    private FlashcardReviewRepository flashcardReviewRepository;

    @Mock
    private TopicRepository topicRepository;

    private RevisionService revisionService;

    private Topic sampleTopic;
    private Subject sampleSubject;

    @BeforeEach
    void setUp() {
        revisionService = new RevisionService(
                revisionSessionRepository,
                revisionHistoryRepository,
                flashcardRepository,
                flashcardReviewRepository,
                topicRepository
        );

        sampleSubject = new Subject("Data Structures", LocalDate.now().plusDays(10), Importance.HIGH);
        sampleSubject.setId(1L);

        sampleTopic = new Topic("Binary Search Trees", sampleSubject, Difficulty.HARD, 90, true);
        sampleTopic.setId(10L);
    }

    @Test
    @DisplayName("Create initial revision schedules session 1 day out with EF 2.5")
    void testCreateInitialRevision_Success() {
        when(revisionSessionRepository.findFirstByTopicIdAndStatusInOrderByScheduledDateDesc(eq(10L), any()))
                .thenReturn(Optional.empty());

        when(revisionSessionRepository.save(any(RevisionSession.class))).thenAnswer(i -> {
            RevisionSession s = i.getArgument(0);
            s.setId(100L);
            return s;
        });

        RevisionSessionDto dto = revisionService.createInitialRevision(sampleTopic);

        assertNotNull(dto);
        assertEquals(1, dto.getIntervalDays());
        assertEquals(2.5, dto.getEasinessFactor());
        assertEquals(LocalDate.now().plusDays(1), dto.getScheduledDate());
        assertEquals(RevisionStatus.PENDING, dto.getStatus());
    }

    @Test
    @DisplayName("Create initial revision prevents duplicate if active session exists")
    void testCreateInitialRevision_DuplicatePrevented() {
        RevisionSession existing = new RevisionSession(sampleTopic, LocalDate.now().plusDays(1), 0, 1, 2.5, RevisionStatus.PENDING);
        existing.setId(50L);

        when(revisionSessionRepository.findFirstByTopicIdAndStatusInOrderByScheduledDateDesc(eq(10L), any()))
                .thenReturn(Optional.of(existing));

        RevisionSessionDto dto = revisionService.createInitialRevision(sampleTopic);

        assertNotNull(dto);
        assertEquals(50L, dto.getId());
        verify(revisionSessionRepository, never()).save(any(RevisionSession.class));
    }

    @Test
    @DisplayName("Mark revision as GOOD advances interval using SM-2")
    void testReviewRevision_Good() {
        RevisionSession session = new RevisionSession(sampleTopic, LocalDate.now(), 1, 1, 2.5, RevisionStatus.PENDING);
        session.setId(101L);

        when(revisionSessionRepository.findById(101L)).thenReturn(Optional.of(session));
        when(revisionSessionRepository.save(any(RevisionSession.class))).thenAnswer(i -> i.getArgument(0));

        RevisionReviewRequest req = new RevisionReviewRequest(RecallRating.GOOD, "Solid recall");
        RevisionSessionDto updated = revisionService.reviewRevision(101L, req);

        assertNotNull(updated);
        assertEquals(2, updated.getRepetitionNumber());
        assertEquals(3, updated.getIntervalDays()); // 2nd repetition -> 3 days
        assertEquals(2.5, updated.getEasinessFactor());
        assertEquals(LocalDate.now().plusDays(3), updated.getScheduledDate());

        verify(revisionHistoryRepository, times(1)).save(any(RevisionHistory.class));
    }

    @Test
    @DisplayName("Mark revision as HARD reduces easiness factor and sets shorter interval")
    void testReviewRevision_Hard() {
        RevisionSession session = new RevisionSession(sampleTopic, LocalDate.now(), 1, 1, 2.5, RevisionStatus.PENDING);
        session.setId(102L);

        when(revisionSessionRepository.findById(102L)).thenReturn(Optional.of(session));
        when(revisionSessionRepository.save(any(RevisionSession.class))).thenAnswer(i -> i.getArgument(0));

        RevisionReviewRequest req = new RevisionReviewRequest(RecallRating.HARD, "Struggled with tree rotations");
        RevisionSessionDto updated = revisionService.reviewRevision(102L, req);

        assertNotNull(updated);
        assertEquals(2, updated.getRepetitionNumber());
        assertEquals(2, updated.getIntervalDays()); // HARD 2nd repetition -> 2 days instead of 3
        assertEquals(2.35, updated.getEasinessFactor()); // 2.5 - 0.15 = 2.35
        assertEquals(LocalDate.now().plusDays(2), updated.getScheduledDate());
    }

    @Test
    @DisplayName("Mark revision as AGAIN resets repetition counter and interval to 1 day")
    void testReviewRevision_Again() {
        RevisionSession session = new RevisionSession(sampleTopic, LocalDate.now(), 3, 7, 2.5, RevisionStatus.PENDING);
        session.setId(103L);

        when(revisionSessionRepository.findById(103L)).thenReturn(Optional.of(session));
        when(revisionSessionRepository.save(any(RevisionSession.class))).thenAnswer(i -> i.getArgument(0));

        RevisionReviewRequest req = new RevisionReviewRequest(RecallRating.AGAIN, "Total blackout on concepts");
        RevisionSessionDto updated = revisionService.reviewRevision(103L, req);

        assertNotNull(updated);
        assertEquals(0, updated.getRepetitionNumber()); // Counter reset!
        assertEquals(1, updated.getIntervalDays());     // Back to 1 day!
        assertEquals(2.30, updated.getEasinessFactor()); // 2.5 - 0.20 = 2.30
        assertEquals(LocalDate.now().plusDays(1), updated.getScheduledDate());
    }

    @Test
    @DisplayName("Mark revision as EASY accelerates interval and increases EF")
    void testReviewRevision_Easy() {
        RevisionSession session = new RevisionSession(sampleTopic, LocalDate.now(), 1, 1, 2.5, RevisionStatus.PENDING);
        session.setId(104L);

        when(revisionSessionRepository.findById(104L)).thenReturn(Optional.of(session));
        when(revisionSessionRepository.save(any(RevisionSession.class))).thenAnswer(i -> i.getArgument(0));

        RevisionReviewRequest req = new RevisionReviewRequest(RecallRating.EASY, "Instant recall");
        RevisionSessionDto updated = revisionService.reviewRevision(104L, req);

        assertNotNull(updated);
        assertEquals(2, updated.getRepetitionNumber());
        assertEquals(4, updated.getIntervalDays()); // EASY 2nd repetition -> 4 days
        assertEquals(2.65, updated.getEasinessFactor()); // 2.5 + 0.15 = 2.65
        assertEquals(LocalDate.now().plusDays(4), updated.getScheduledDate());
    }

    @Test
    @DisplayName("Get today revisions marks past dates as OVERDUE")
    void testGetTodayRevisions_IdentifiesOverdue() {
        RevisionSession overdue = new RevisionSession(sampleTopic, LocalDate.now().minusDays(3), 1, 1, 2.5, RevisionStatus.PENDING);
        overdue.setId(201L);

        RevisionSession dueToday = new RevisionSession(sampleTopic, LocalDate.now(), 1, 1, 2.5, RevisionStatus.PENDING);
        dueToday.setId(202L);

        when(revisionSessionRepository.findByScheduledDateLessThanEqualAndStatusInOrderByScheduledDateAsc(any(), any()))
                .thenReturn(List.of(overdue, dueToday));

        List<RevisionSessionDto> result = revisionService.getTodayRevisions();

        assertEquals(2, result.size());
        assertTrue(result.get(0).isOverdue());
        assertEquals(3, result.get(0).getDaysOverdue());
        assertFalse(result.get(1).isOverdue());
    }
}

