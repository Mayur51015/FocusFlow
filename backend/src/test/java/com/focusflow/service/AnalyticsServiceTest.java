package com.focusflow.service;

import com.focusflow.dto.*;
import com.focusflow.entity.*;
import com.focusflow.repository.*;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnalyticsServiceTest {

    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private TopicRepository topicRepository;
    @Mock
    private TopicPerformanceRepository topicPerformanceRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private QuizAttemptRepository quizAttemptRepository;
    @Mock
    private RevisionSessionRepository revisionSessionRepository;
    @Mock
    private RevisionHistoryRepository revisionHistoryRepository;
    @Mock
    private FlashcardRepository flashcardRepository;
    @Mock
    private ExamReadinessService examReadinessService;

    private AnalyticsService analyticsService;

    private Subject dsaSubject;
    private Topic topic1;
    private Topic topic2;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(
                subjectRepository, topicRepository, topicPerformanceRepository,
                quizRepository, quizAttemptRepository, revisionSessionRepository,
                revisionHistoryRepository, flashcardRepository, examReadinessService
        );

        dsaSubject = new Subject("Data Structures & Algorithms", LocalDate.now().plusDays(5), Importance.HIGH);
        dsaSubject.setId(1L);

        topic1 = new Topic("Binary Search Trees", dsaSubject, Difficulty.HARD, 90, true);
        topic1.setId(10L);

        topic2 = new Topic("Graphs", dsaSubject, Difficulty.HARD, 120, false);
        topic2.setId(11L);
    }

    @Test
    void testGetLearningSummary_AggregatesCorrectly() {
        when(topicRepository.findAll()).thenReturn(List.of(topic1, topic2));

        TopicPerformance perf1 = new TopicPerformance(topic1, PerformanceLevel.STRONG, 90.0, 3, 90.0, 120, "TRAINED_ML", 0.95, "Great work", LocalDateTime.now());
        TopicPerformance perf2 = new TopicPerformance(topic2, PerformanceLevel.WEAK, 40.0, 2, 40.0, 45, "TRAINED_ML", 0.88, "Revise basics", LocalDateTime.now());
        when(topicPerformanceRepository.findAll()).thenReturn(List.of(perf1, perf2));

        RevisionHistory rev1 = new RevisionHistory(null, topic1, RecallRating.GOOD, LocalDateTime.now(), 1, 3, 2.5, 2.5, "Good");
        RevisionHistory rev2 = new RevisionHistory(null, topic1, RecallRating.EASY, LocalDateTime.now(), 3, 7, 2.5, 2.65, "Easy");
        when(revisionHistoryRepository.findAll()).thenReturn(List.of(rev1, rev2));
        when(flashcardRepository.count()).thenReturn(5L);

        ExamReadinessOverviewDto overview = new ExamReadinessOverviewDto();
        overview.setOverallReadinessScore(78.5);
        overview.setOverallReadinessLevel(ReadinessLevel.HIGH);
        when(examReadinessService.getReadinessOverview()).thenReturn(overview);

        LearningSummaryDto summary = analyticsService.getLearningSummary();

        assertNotNull(summary);
        assertEquals(2, summary.getTotalTopics());
        assertEquals(1, summary.getStrongTopicsCount());
        assertEquals(1, summary.getWeakTopicsCount());
        assertEquals(100.0, summary.getRetentionRatePercentage());
        assertEquals(5, summary.getTotalFlashcards());
        assertEquals(78.5, summary.getOverallReadinessScore());
        assertEquals("HIGH", summary.getOverallReadinessLevel());
        assertFalse(summary.getRecommendedStudyFocus().isEmpty());
    }

    @Test
    void testGetSubjectMastery_CalculatesPerSubjectMetrics() {
        when(subjectRepository.findAll()).thenReturn(List.of(dsaSubject));
        when(topicRepository.findBySubjectId(1L)).thenReturn(List.of(topic1, topic2));

        ExamReadinessDto erDto = new ExamReadinessDto();
        erDto.setReadinessScore(80.0);
        erDto.setReadinessLevel(ReadinessLevel.HIGH);
        when(examReadinessService.getSubjectReadiness(1L)).thenReturn(erDto);

        List<SubjectMasteryDto> result = analyticsService.getSubjectMastery();

        assertNotNull(result);
        assertEquals(1, result.size());
        SubjectMasteryDto sm = result.get(0);
        assertEquals("Data Structures & Algorithms", sm.getSubjectName());
        assertEquals(2, sm.getTotalTopics());
        assertEquals(1, sm.getCompletedTopics());
        assertEquals(0.5, sm.getTopicCompletionRate());
        assertEquals(80.0, sm.getReadinessScore());
    }

    @Test
    void testGetRetentionTrends_ComputesRecallDistribution() {
        RevisionHistory rAgain = new RevisionHistory(null, topic1, RecallRating.AGAIN, LocalDateTime.now(), 3, 1, 2.5, 2.3, "Again");
        RevisionHistory rGood = new RevisionHistory(null, topic1, RecallRating.GOOD, LocalDateTime.now(), 1, 3, 2.5, 2.5, "Good");
        RevisionHistory rEasy = new RevisionHistory(null, topic1, RecallRating.EASY, LocalDateTime.now(), 3, 7, 2.5, 2.65, "Easy");
        when(revisionHistoryRepository.findAllByOrderByReviewedAtDesc()).thenReturn(List.of(rAgain, rGood, rEasy));

        RevisionSession sess = new RevisionSession(topic1, LocalDate.now(), 2, 3, 2.6, RevisionStatus.PENDING);
        when(revisionSessionRepository.findAll()).thenReturn(List.of(sess));

        RetentionTrendsDto trends = analyticsService.getRetentionTrends();

        assertNotNull(trends);
        assertEquals(3, trends.getTotalReviews());
        assertEquals(1, trends.getAgainCount());
        assertEquals(1, trends.getGoodCount());
        assertEquals(1, trends.getEasyCount());
        assertEquals(66.7, trends.getRetentionRatePercentage());
        assertEquals(2.6, trends.getAverageEasinessFactor());
    }
}
