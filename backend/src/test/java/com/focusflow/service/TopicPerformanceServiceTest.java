package com.focusflow.service;

import com.focusflow.dto.MlWeakTopicResponse;
import com.focusflow.dto.TopicPerformanceDto;
import com.focusflow.entity.*;
import com.focusflow.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class TopicPerformanceServiceTest {

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private TopicPerformanceRepository topicPerformanceRepository;

    @Mock
    private QuizAnswerRepository quizAnswerRepository;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private StudySessionRepository studySessionRepository;

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Mock
    private RestTemplate restTemplate;

    private TopicPerformanceService service;

    private Topic sampleTopic;
    private Subject sampleSubject;

    @BeforeEach
    void setUp() {
        when(restTemplateBuilder.setConnectTimeout(any(Duration.class))).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.setReadTimeout(any(Duration.class))).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        service = new TopicPerformanceService(
                topicRepository,
                topicPerformanceRepository,
                quizAnswerRepository,
                quizAttemptRepository,
                studySessionRepository,
                restTemplateBuilder,
                2000,
                3000
        );

        sampleSubject = new Subject("Data Structures", LocalDate.now().plusDays(10), Importance.HIGH);
        sampleSubject.setId(1L);

        sampleTopic = new Topic("Binary Search Trees", sampleSubject, Difficulty.HARD, 90, false);
        sampleTopic.setId(10L);
    }

    @Test
    @DisplayName("Evaluate topic with ML service returning STRONG")
    void testEvaluateTopic_WithMl_Strong() {
        when(topicRepository.findById(10L)).thenReturn(Optional.of(sampleTopic));
        when(quizAnswerRepository.countByTopicId(10L)).thenReturn(10L);
        when(quizAnswerRepository.countCorrectByTopicId(10L)).thenReturn(9L);

        QuizAttempt attempt = new QuizAttempt(null, 90, 90.0, true, LocalDateTime.now(), LocalDateTime.now(), 120);
        when(quizAttemptRepository.findByTopicIdOrderByCompletedAtDesc(10L)).thenReturn(List.of(attempt));
        when(studySessionRepository.findByTopicId(10L)).thenReturn(Collections.emptyList());

        MlWeakTopicResponse mlResponse = new MlWeakTopicResponse(
                10L, "STRONG", 0.98, "RANDOM_FOREST",
                Map.of("STRONG", 0.98, "MODERATE", 0.02),
                "Strong mastery on Binary Search Trees"
        );
        when(restTemplate.postForEntity(anyString(), any(), eq(MlWeakTopicResponse.class)))
                .thenReturn(new ResponseEntity<>(mlResponse, HttpStatus.OK));

        when(topicPerformanceRepository.findByTopicId(10L)).thenReturn(Optional.empty());
        when(topicPerformanceRepository.save(any(TopicPerformance.class))).thenAnswer(i -> {
            TopicPerformance p = i.getArgument(0);
            p.setId(100L);
            return p;
        });

        TopicPerformanceDto dto = service.evaluateTopic(10L);

        assertNotNull(dto);
        assertEquals(PerformanceLevel.STRONG, dto.getPerformanceLevel());
        assertEquals("RANDOM_FOREST", dto.getPredictionSource());
        assertEquals(0.98, dto.getConfidenceScore());
    }

    @Test
    @DisplayName("Evaluate topic with ML service returning WEAK for struggling student")
    void testEvaluateTopic_WithMl_Weak() {
        when(topicRepository.findById(10L)).thenReturn(Optional.of(sampleTopic));
        when(quizAnswerRepository.countByTopicId(10L)).thenReturn(10L);
        when(quizAnswerRepository.countCorrectByTopicId(10L)).thenReturn(3L);

        QuizAttempt attempt = new QuizAttempt(null, 30, 30.0, false, LocalDateTime.now(), LocalDateTime.now(), 120);
        when(quizAttemptRepository.findByTopicIdOrderByCompletedAtDesc(10L)).thenReturn(List.of(attempt));
        when(studySessionRepository.findByTopicId(10L)).thenReturn(Collections.emptyList());

        MlWeakTopicResponse mlResponse = new MlWeakTopicResponse(
                10L, "WEAK", 0.95, "RANDOM_FOREST",
                Map.of("WEAK", 0.95, "MODERATE", 0.05),
                "Significant knowledge gaps identified"
        );
        when(restTemplate.postForEntity(anyString(), any(), eq(MlWeakTopicResponse.class)))
                .thenReturn(new ResponseEntity<>(mlResponse, HttpStatus.OK));

        when(topicPerformanceRepository.findByTopicId(10L)).thenReturn(Optional.empty());
        when(topicPerformanceRepository.save(any(TopicPerformance.class))).thenAnswer(i -> i.getArgument(0));

        TopicPerformanceDto dto = service.evaluateTopic(10L);

        assertEquals(PerformanceLevel.WEAK, dto.getPerformanceLevel());
        assertEquals("RANDOM_FOREST", dto.getPredictionSource());
    }

    @Test
    @DisplayName("Evaluate topic with ML service unavailable - Rule-Based Fallback classifies WEAK")
    void testEvaluateTopic_MlOffline_RuleBasedFallback_Weak() {
        when(topicRepository.findById(10L)).thenReturn(Optional.of(sampleTopic));
        when(quizAnswerRepository.countByTopicId(10L)).thenReturn(6L);
        when(quizAnswerRepository.countCorrectByTopicId(10L)).thenReturn(2L); // 33.3%

        QuizAttempt attempt = new QuizAttempt(null, 33, 33.3, false, LocalDateTime.now(), LocalDateTime.now(), 60);
        when(quizAttemptRepository.findByTopicIdOrderByCompletedAtDesc(10L)).thenReturn(List.of(attempt));
        when(studySessionRepository.findByTopicId(10L)).thenReturn(Collections.emptyList());

        // Simulate microservice timeout/error
        when(restTemplate.postForEntity(anyString(), any(), eq(MlWeakTopicResponse.class)))
                .thenThrow(new RestClientException("Connection refused"));

        when(topicPerformanceRepository.findByTopicId(10L)).thenReturn(Optional.empty());
        when(topicPerformanceRepository.save(any(TopicPerformance.class))).thenAnswer(i -> i.getArgument(0));

        TopicPerformanceDto dto = service.evaluateTopic(10L);

        assertNotNull(dto);
        assertEquals(PerformanceLevel.WEAK, dto.getPerformanceLevel());
        assertEquals("RULE_BASED", dto.getPredictionSource());
        assertTrue(dto.getRecommendation().contains("Rule-Based"));
    }

    @Test
    @DisplayName("Evaluate unassessed topic with sufficient study time -> MODERATE")
    void testEvaluateTopic_Unassessed_WithStudyTime() {
        when(topicRepository.findById(10L)).thenReturn(Optional.of(sampleTopic));
        when(quizAnswerRepository.countByTopicId(10L)).thenReturn(0L);
        when(quizAnswerRepository.countCorrectByTopicId(10L)).thenReturn(0L);
        when(quizAttemptRepository.findByTopicIdOrderByCompletedAtDesc(10L)).thenReturn(Collections.emptyList());

        StudySession session = new StudySession(sampleTopic, LocalDate.now(), 60, 75, SessionStatus.COMPLETED);
        when(studySessionRepository.findByTopicId(10L)).thenReturn(List.of(session));

        when(restTemplate.postForEntity(anyString(), any(), eq(MlWeakTopicResponse.class)))
                .thenThrow(new RestClientException("Offline"));

        when(topicPerformanceRepository.findByTopicId(10L)).thenReturn(Optional.empty());
        when(topicPerformanceRepository.save(any(TopicPerformance.class))).thenAnswer(i -> i.getArgument(0));

        TopicPerformanceDto dto = service.evaluateTopic(10L);

        assertEquals(PerformanceLevel.MODERATE, dto.getPerformanceLevel());
        assertEquals("RULE_BASED", dto.getPredictionSource());
        assertTrue(dto.getTotalStudyMinutes() >= 60);
    }

    @Test
    @DisplayName("Evaluate unassessed topic with no study time -> WEAK")
    void testEvaluateTopic_Unassessed_NoStudyTime() {
        when(topicRepository.findById(10L)).thenReturn(Optional.of(sampleTopic));
        when(quizAnswerRepository.countByTopicId(10L)).thenReturn(0L);
        when(quizAnswerRepository.countCorrectByTopicId(10L)).thenReturn(0L);
        when(quizAttemptRepository.findByTopicIdOrderByCompletedAtDesc(10L)).thenReturn(Collections.emptyList());
        when(studySessionRepository.findByTopicId(10L)).thenReturn(Collections.emptyList());

        when(restTemplate.postForEntity(anyString(), any(), eq(MlWeakTopicResponse.class)))
                .thenThrow(new RestClientException("Offline"));

        when(topicPerformanceRepository.findByTopicId(10L)).thenReturn(Optional.empty());
        when(topicPerformanceRepository.save(any(TopicPerformance.class))).thenAnswer(i -> i.getArgument(0));

        TopicPerformanceDto dto = service.evaluateTopic(10L);

        assertEquals(PerformanceLevel.WEAK, dto.getPerformanceLevel());
        assertEquals("RULE_BASED", dto.getPredictionSource());
    }

    @Test
    @DisplayName("Get weak topics returns only WEAK classifications")
    void testGetWeakTopics() {
        TopicPerformance weak1 = new TopicPerformance(sampleTopic, PerformanceLevel.WEAK, 40.0, 2, 40.0, 45, "RANDOM_FOREST", 0.92, "Focus here", LocalDateTime.now());
        when(topicPerformanceRepository.findByPerformanceLevel(PerformanceLevel.WEAK)).thenReturn(List.of(weak1));

        List<TopicPerformanceDto> weakTopics = service.getWeakTopics();

        assertEquals(1, weakTopics.size());
        assertEquals(sampleTopic.getName(), weakTopics.get(0).getTopicName());
        assertEquals(PerformanceLevel.WEAK, weakTopics.get(0).getPerformanceLevel());
    }
}
