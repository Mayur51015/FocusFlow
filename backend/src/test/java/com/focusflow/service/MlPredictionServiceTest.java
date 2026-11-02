package com.focusflow.service;

import com.focusflow.dto.FastApiPredictionResponse;
import com.focusflow.dto.TopicPredictionResponse;
import com.focusflow.entity.Difficulty;
import com.focusflow.entity.Importance;
import com.focusflow.entity.Subject;
import com.focusflow.entity.Topic;
import com.focusflow.exception.ResourceNotFoundException;
import com.focusflow.repository.StudySessionRepository;
import com.focusflow.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MlPredictionServiceTest {

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private StudySessionRepository studySessionRepository;

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Mock
    private RestTemplate restTemplate;

    private MlPredictionService mlPredictionService;
    private Topic topicDsa;

    @BeforeEach
    void setUp() throws Exception {
        when(restTemplateBuilder.setConnectTimeout(any())).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.setReadTimeout(any())).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        mlPredictionService = new MlPredictionService(
                topicRepository,
                studySessionRepository,
                restTemplateBuilder,
                2000,
                3000
        );

        // Set private fields via reflection
        Field urlField = MlPredictionService.class.getDeclaredField("mlServiceUrl");
        urlField.setAccessible(true);
        urlField.set(mlPredictionService, "http://localhost:8000");

        Field enabledField = MlPredictionService.class.getDeclaredField("mlEnabled");
        enabledField.setAccessible(true);
        enabledField.set(mlPredictionService, true);

        Subject subject = new Subject("Data Structures", LocalDate.now().plusDays(5), Importance.HIGH);
        subject.setId(1L);

        topicDsa = new Topic("Dynamic Programming", subject, Difficulty.HARD, 60, false);
        topicDsa.setId(10L);
    }

    @Test
    @DisplayName("predictForTopic: Successfully uses ML microservice response when online")
    void testPredictForTopic_Success() {
        FastApiPredictionResponse fastApiResponse = new FastApiPredictionResponse(
                85, 0.92, 8, "1.0", "EXPERIMENTAL_ML", null
        );

        when(restTemplate.postForEntity(anyString(), any(), eq(FastApiPredictionResponse.class)))
                .thenReturn(new ResponseEntity<>(fastApiResponse, HttpStatus.OK));

        when(studySessionRepository.findByTopicId(10L)).thenReturn(Collections.emptyList());

        TopicPredictionResponse response = mlPredictionService.predictForTopic(topicDsa);

        assertNotNull(response);
        assertEquals(10L, response.getTopicId());
        assertEquals(85, response.getPredictedMinutes());
        assertEquals("EXPERIMENTAL_ML", response.getPredictionSource());
        assertEquals("1.0", response.getModelVersion());
        assertEquals(0.92, response.getConfidenceScore());
    }

    @Test
    @DisplayName("predictForTopic: Gracefully falls back when ML service throws connection exception")
    void testPredictForTopic_ConnectionTimeoutFallback() {
        when(restTemplate.postForEntity(anyString(), any(), eq(FastApiPredictionResponse.class)))
                .thenThrow(new ResourceAccessException("Connection timed out to http://localhost:8000"));

        when(studySessionRepository.findByTopicId(10L)).thenReturn(Collections.emptyList());

        TopicPredictionResponse response = mlPredictionService.predictForTopic(topicDsa);

        assertNotNull(response);
        assertEquals(10L, response.getTopicId());
        // For HARD topic with 60 mins baseline, fallback is 60 * 1.30 = 78 mins
        assertEquals(78, response.getPredictedMinutes());
        assertEquals("HEURISTIC_FALLBACK", response.getPredictionSource());
        assertEquals("fallback", response.getModelVersion());
        assertTrue(response.getMessage().contains("fallback"));
    }

    @Test
    @DisplayName("predictForTopicId: Throws ResourceNotFoundException when topic does not exist")
    void testPredictForTopicId_NotFound() {
        when(topicRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> mlPredictionService.predictForTopicId(999L));
    }

    @Test
    @DisplayName("clampDuration: Enforces upper and lower safety bounds")
    void testClampDuration() {
        // Baseline 60: lower bound = max(15, 60 * 0.4 = 24) -> 24; upper bound = min(360, 60 * 2.5 = 150) -> 150
        assertEquals(24, mlPredictionService.clampDuration(5, 60)); // Under minimum -> clamped to 24
        assertEquals(150, mlPredictionService.clampDuration(500, 60)); // Over maximum -> clamped to 150
        assertEquals(75, mlPredictionService.clampDuration(75, 60)); // Normal -> untouched
    }
}

