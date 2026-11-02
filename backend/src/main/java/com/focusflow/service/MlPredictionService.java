package com.focusflow.service;

import com.focusflow.dto.FastApiPredictionRequest;
import com.focusflow.dto.FastApiPredictionResponse;
import com.focusflow.dto.TopicPredictionResponse;
import com.focusflow.entity.Difficulty;
import com.focusflow.entity.StudySession;
import com.focusflow.entity.Topic;
import com.focusflow.exception.ResourceNotFoundException;
import com.focusflow.repository.StudySessionRepository;
import com.focusflow.repository.TopicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.OptionalDouble;

/**
 * MlPredictionService handles communication with the Python FastAPI ML microservice.
 * Provides resilient study duration prediction, input/output validation, reasonableness checks,
 * and automatic heuristic fallback when the ML service is offline or times out.
 */
@Service
public class MlPredictionService {

    private static final Logger logger = LoggerFactory.getLogger(MlPredictionService.class);

    private final TopicRepository topicRepository;
    private final StudySessionRepository studySessionRepository;
    private final RestTemplate restTemplate;

    @Value("${focusflow.ml.service.url:http://localhost:8000}")
    private String mlServiceUrl;

    @Value("${focusflow.ml.service.enabled:true}")
    private boolean mlEnabled;

    public MlPredictionService(TopicRepository topicRepository,
                               StudySessionRepository studySessionRepository,
                               RestTemplateBuilder restTemplateBuilder,
                               @Value("${focusflow.ml.service.connect-timeout-ms:2000}") int connectTimeoutMs,
                               @Value("${focusflow.ml.service.read-timeout-ms:3000}") int readTimeoutMs) {
        this.topicRepository = topicRepository;
        this.studySessionRepository = studySessionRepository;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }

    /**
     * Predicts realistic study duration for a topic by its ID.
     */
    public TopicPredictionResponse predictForTopicId(Long topicId) {
        if (topicId == null) {
            throw new IllegalArgumentException("Topic ID cannot be null");
        }
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + topicId));
        return predictForTopic(topic);
    }

    /**
     * Predicts realistic study duration for a given Topic entity.
     * Uses empirical Random Forest ML predictions when available, falling back safely to heuristic rules.
     */
    public TopicPredictionResponse predictForTopic(Topic topic) {
        if (topic == null) {
            return new TopicPredictionResponse(
                    null, "Unknown", 60, 60, 0.5, 10, "fallback", "HEURISTIC_FALLBACK", "Null topic provided"
            );
        }

        int baselineEstimate = Math.max(15, topic.getEstimatedMinutes());

        if (!mlEnabled) {
            logger.info("ML prediction is disabled via configuration. Using heuristic fallback for '{}'", topic.getName());
            return executeHeuristicFallback(topic, baselineEstimate, "ML service disabled via configuration");
        }

        // 1. Gather historical metrics for topic
        List<StudySession> historicalSessions = studySessionRepository.findByTopicId(topic.getId());
        List<StudySession> completedSessionsWithActuals = historicalSessions.stream()
                .filter(s -> s.getActualMinutes() > 0)
                .toList();

        Double historicalAverage = null;
        if (!completedSessionsWithActuals.isEmpty()) {
            OptionalDouble avg = completedSessionsWithActuals.stream()
                    .mapToInt(StudySession::getActualMinutes)
                    .average();
            if (avg.isPresent()) {
                historicalAverage = Math.round(avg.getAsDouble() * 10.0) / 10.0;
            }
        }

        int previousSessionsCount = completedSessionsWithActuals.size();
        String subjectCategory = resolveSubjectCategory(topic);
        String difficultyStr = topic.getDifficulty() != null ? topic.getDifficulty().name() : "MEDIUM";

        // 2. Prepare request payload for FastAPI
        FastApiPredictionRequest requestPayload = new FastApiPredictionRequest(
                difficultyStr,
                baselineEstimate,
                historicalAverage,
                previousSessionsCount,
                subjectCategory
        );

        String endpointUrl = mlServiceUrl + "/ml/predict-duration";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<FastApiPredictionRequest> entity = new HttpEntity<>(requestPayload, headers);

            logger.debug("Dispatching ML prediction request to {} for topic '{}'", endpointUrl, topic.getName());
            ResponseEntity<FastApiPredictionResponse> response = restTemplate.postForEntity(
                    endpointUrl,
                    entity,
                    FastApiPredictionResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                FastApiPredictionResponse mlResult = response.getBody();

                // 3. Reasonableness clamping: prevent extreme outliers
                int clampedMinutes = clampDuration(mlResult.getPredictedMinutes(), baselineEstimate);

                logger.info("ML Prediction received for '{}': {}m (original: {}m, source: {})",
                        topic.getName(), clampedMinutes, baselineEstimate, mlResult.getPredictionSource());

                return new TopicPredictionResponse(
                        topic.getId(),
                        topic.getName(),
                        baselineEstimate,
                        clampedMinutes,
                        mlResult.getConfidenceScore(),
                        mlResult.getEstimatedErrorRangeMinutes(),
                        mlResult.getModelVersion(),
                        mlResult.getPredictionSource() != null ? mlResult.getPredictionSource() : "EXPERIMENTAL_ML",
                        "Predicted via FocusFlow ML Random Forest Regressor"
                );
            }
        } catch (RestClientException ex) {
            logger.warn("ML microservice connection or timeout failure: {}. Gracefully applying heuristic fallback for topic '{}'",
                    ex.getMessage(), topic.getName());
        } catch (Exception ex) {
            logger.error("Unexpected error during ML duration prediction: {}. Falling back to heuristic rule.", ex.getMessage(), ex);
        }

        // 4. Graceful Fallback if service is down or unreachable
        return executeHeuristicFallback(topic, baselineEstimate, "ML service unreachable, heuristic fallback applied");
    }

    /**
     * Executes rule-based heuristic prediction when ML microservice is offline.
     */
    public TopicPredictionResponse executeHeuristicFallback(Topic topic, int baselineEstimate, String reason) {
        Difficulty difficulty = topic.getDifficulty() != null ? topic.getDifficulty() : Difficulty.MEDIUM;

        double factor;
        switch (difficulty) {
            case EASY -> factor = 0.90;
            case HARD -> factor = 1.30;
            case MEDIUM -> factor = 1.05;
            default -> factor = 1.0;
        }

        double calculated = baselineEstimate * factor;

        // Check if student has historical sessions
        List<StudySession> historicalSessions = studySessionRepository.findByTopicId(topic.getId());
        List<StudySession> pastSessions = historicalSessions.stream()
                .filter(s -> s.getActualMinutes() > 0)
                .toList();

        if (!pastSessions.isEmpty()) {
            double histAvg = pastSessions.stream().mapToInt(StudySession::getActualMinutes).average().orElse(calculated);
            calculated = (calculated * 0.5) + (histAvg * 0.5);
        }

        int finalMinutes = clampDuration((int) Math.round(calculated), baselineEstimate);

        return new TopicPredictionResponse(
                topic.getId(),
                topic.getName(),
                baselineEstimate,
                finalMinutes,
                0.70,
                Math.max(10, (int) Math.round(baselineEstimate * 0.20)),
                "fallback",
                "HEURISTIC_FALLBACK",
                reason
        );
    }

    /**
     * Clamps predicted duration to realistic boundaries:
     * - Minimum: max(15 mins, 40% of baseline estimate)
     * - Maximum: min(360 mins, 250% of baseline estimate)
     */
    public int clampDuration(int rawDuration, int baselineEstimate) {
        int minAllowed = Math.max(15, (int) Math.round(baselineEstimate * 0.40));
        int maxAllowed = Math.min(360, (int) Math.round(baselineEstimate * 2.50));
        return Math.min(Math.max(rawDuration, minAllowed), maxAllowed);
    }

    /**
     * Infers coarse subject category from subject metadata.
     */
    private String resolveSubjectCategory(Topic topic) {
        if (topic.getSubject() != null && topic.getSubject().getName() != null) {
            String nameUpper = topic.getSubject().getName().toUpperCase();
            if (nameUpper.contains("ALGORITHM") || nameUpper.contains("DATA") || nameUpper.contains("OPERATING")
                    || nameUpper.contains("SYSTEM") || nameUpper.contains("DATABASE") || nameUpper.contains("MATH")
                    || nameUpper.contains("PHYSIC") || nameUpper.contains("CODE")) {
                return "STEM";
            }
            if (nameUpper.contains("HISTORY") || nameUpper.contains("LITERATURE") || nameUpper.contains("LANGUAGE")) {
                return "HUMANITIES";
            }
            if (nameUpper.contains("ACCOUNT") || nameUpper.contains("ECONOMY") || nameUpper.contains("FINANCE")) {
                return "COMMERCE";
            }
        }
        return "GENERAL";
    }

    /**
     * Checks if the ML microservice is currently responsive.
     */
    public boolean isMlServiceAvailable() {
        if (!mlEnabled) return false;
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(mlServiceUrl + "/health", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}

