package com.focusflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focusflow.dto.ExamReadinessDto;
import com.focusflow.dto.ExamReadinessOverviewDto;
import com.focusflow.dto.MlReadinessResponse;
import com.focusflow.entity.*;
import com.focusflow.exception.ResourceNotFoundException;
import com.focusflow.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExamReadinessServiceTest {

    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private TopicRepository topicRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private QuizAttemptRepository quizAttemptRepository;
    @Mock
    private TopicPerformanceRepository topicPerformanceRepository;
    @Mock
    private RevisionSessionRepository revisionSessionRepository;
    @Mock
    private RevisionHistoryRepository revisionHistoryRepository;
    @Mock
    private ExamReadinessRepository examReadinessRepository;
    @Mock
    private RestTemplateBuilder restTemplateBuilder;
    @Mock
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();
    private ExamReadinessService examReadinessService;

    private Subject dsaSubject;
    private Topic topic1;
    private Topic topic2;

    @BeforeEach
    void setUp() {
        when(restTemplateBuilder.setConnectTimeout(any())).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.setReadTimeout(any())).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        examReadinessService = new ExamReadinessService(
                subjectRepository, topicRepository, quizRepository, quizAttemptRepository,
                topicPerformanceRepository, revisionSessionRepository, revisionHistoryRepository,
                examReadinessRepository, restTemplateBuilder, objectMapper
        );
        ReflectionTestUtils.setField(examReadinessService, "mlServiceBaseUrl", "http://localhost:8000");

        dsaSubject = new Subject("Data Structures & Algorithms", LocalDate.now().plusDays(10), Importance.HIGH);
        dsaSubject.setId(1L);

        topic1 = new Topic("Binary Search Trees", dsaSubject, Difficulty.HARD, 90, true);
        topic1.setId(10L);

        topic2 = new Topic("Graphs", dsaSubject, Difficulty.HARD, 120, false);
        topic2.setId(11L);

        when(examReadinessRepository.save(any(ExamReadiness.class))).thenAnswer(invocation -> {
            ExamReadiness er = invocation.getArgument(0);
            er.setId(99L);
            return er;
        });
    }

    @Test
    void testCalculateSubjectReadiness_WithSuccessfulMlResponse() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(dsaSubject));
        when(topicRepository.findBySubjectId(1L)).thenReturn(List.of(topic1, topic2));
        when(quizRepository.findBySubjectIdOrderByIdDesc(anyLong())).thenReturn(Collections.emptyList());

        MlReadinessResponse mlResp = new MlReadinessResponse();
        mlResp.setSubjectId(1L);
        mlResp.setReadinessScore(82.5);
        mlResp.setReadinessLevel("HIGH");
        mlResp.setPredictionSource("TRAINED_ML");
        mlResp.setRecommendations(List.of("Continue spaced repetition"));

        when(restTemplate.postForEntity(contains("/ml/predict-exam-readiness"), any(), eq(MlReadinessResponse.class)))
                .thenReturn(ResponseEntity.ok(mlResp));

        ExamReadinessDto dto = examReadinessService.calculateSubjectReadiness(1L);

        assertNotNull(dto);
        assertEquals(82.5, dto.getReadinessScore());
        assertEquals(ReadinessLevel.HIGH, dto.getReadinessLevel());
        assertEquals("TRAINED_ML", dto.getPredictionSource());
        assertTrue(dto.getRecommendations().contains("Continue spaced repetition"));
    }

    @Test
    void testCalculateSubjectReadiness_MlOffline_AppliesRuleBasedFallback() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(dsaSubject));
        when(topicRepository.findBySubjectId(1L)).thenReturn(List.of(topic1)); // 100% completion
        when(quizRepository.findBySubjectIdOrderByIdDesc(anyLong())).thenReturn(Collections.emptyList());

        // Simulate ML connection error
        when(restTemplate.postForEntity(anyString(), any(), eq(MlReadinessResponse.class)))
                .thenThrow(new ResourceAccessException("Connection timed out to ML microservice"));

        ExamReadinessDto dto = examReadinessService.calculateSubjectReadiness(1L);

        assertNotNull(dto);
        assertEquals("RULE_BASED_FALLBACK", dto.getPredictionSource());
        assertNotNull(dto.getReadinessLevel());
        assertFalse(dto.getRecommendations().isEmpty());
    }

    @Test
    void testCalculateSubjectReadiness_UrgencyPenaltyApplied() {
        // Subject with exam in 2 days and low completion (0%)
        Subject urgentSubject = new Subject("Urgent Subject", LocalDate.now().plusDays(2), Importance.HIGH);
        urgentSubject.setId(2L);

        when(subjectRepository.findById(2L)).thenReturn(Optional.of(urgentSubject));
        when(topicRepository.findBySubjectId(2L)).thenReturn(List.of(topic2)); // topic2 is incomplete (0% completion)
        when(quizRepository.findBySubjectIdOrderByIdDesc(2L)).thenReturn(Collections.emptyList());

        // Simulate ML offline so rule-based penalty is exercised
        when(restTemplate.postForEntity(anyString(), any(), eq(MlReadinessResponse.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        ExamReadinessDto dto = examReadinessService.calculateSubjectReadiness(2L);

        assertEquals(ReadinessLevel.LOW, dto.getReadinessLevel());
        assertTrue(dto.getReadinessScore() < 50.0);
        assertTrue(dto.getRecommendations().stream().anyMatch(r -> r.contains("Urgent")));
    }

    @Test
    void testGetSubjectReadiness_ReturnsExistingOrCalculates() {
        ExamReadiness existing = new ExamReadiness(
                dsaSubject, 78.0, ReadinessLevel.HIGH, 0.8, 80.0, 0, 0.0, 0.7, 10,
                "TRAINED_ML", "[\"Review BST flashcards\"]", LocalDateTime.now()
        );
        when(examReadinessRepository.findTopBySubjectIdOrderByEvaluatedAtDesc(1L))
                .thenReturn(Optional.of(existing));

        ExamReadinessDto dto = examReadinessService.getSubjectReadiness(1L);

        assertNotNull(dto);
        assertEquals(78.0, dto.getReadinessScore());
        assertEquals(ReadinessLevel.HIGH, dto.getReadinessLevel());
        verify(examReadinessRepository, never()).save(any());
    }

    @Test
    void testGetReadinessOverview_AggregatesCorrectly() {
        when(subjectRepository.findAll()).thenReturn(List.of(dsaSubject));
        ExamReadiness evaluation = new ExamReadiness(
                dsaSubject, 85.0, ReadinessLevel.HIGH, 0.9, 85.0, 0, 0.0, 0.8, 10,
                "TRAINED_ML", "[]", LocalDateTime.now()
        );
        when(examReadinessRepository.findTopBySubjectIdOrderByEvaluatedAtDesc(1L))
                .thenReturn(Optional.of(evaluation));

        ExamReadinessOverviewDto overview = examReadinessService.getReadinessOverview();

        assertNotNull(overview);
        assertEquals(1, overview.getTotalSubjects());
        assertEquals(85.0, overview.getOverallReadinessScore());
        assertEquals(ReadinessLevel.HIGH, overview.getOverallReadinessLevel());
        assertEquals(1, overview.getHighReadinessCount());
        assertEquals(0, overview.getLowReadinessCount());
    }

    @Test
    void testSubjectNotFound_ThrowsException() {
        when(subjectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> examReadinessService.calculateSubjectReadiness(999L));
    }
}
