package com.focusflow.service;

import com.focusflow.dto.MlWeakTopicRequest;
import com.focusflow.dto.MlWeakTopicResponse;
import com.focusflow.dto.TopicPerformanceDto;
import com.focusflow.entity.*;
import com.focusflow.exception.ResourceNotFoundException;
import com.focusflow.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TopicPerformanceService analyzes student understanding and classifies topics into
 * STRONG, MODERATE, or WEAK. Integrates with Python FastAPI ML service with a transparent
 * rule-based fallback if ML is unavailable.
 */
@Service
public class TopicPerformanceService {

    private static final Logger logger = LoggerFactory.getLogger(TopicPerformanceService.class);

    private final TopicRepository topicRepository;
    private final TopicPerformanceRepository topicPerformanceRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final StudySessionRepository studySessionRepository;
    private final RestTemplate restTemplate;

    @Value("${focusflow.ml.service.url:http://localhost:8000}")
    private String mlServiceUrl = "http://localhost:8000";

    @Value("${focusflow.ml.service.enabled:true}")
    private boolean mlEnabled = true;

    public void setMlServiceUrl(String mlServiceUrl) {
        this.mlServiceUrl = mlServiceUrl;
    }

    public void setMlEnabled(boolean mlEnabled) {
        this.mlEnabled = mlEnabled;
    }

    public TopicPerformanceService(TopicRepository topicRepository,
                                   TopicPerformanceRepository topicPerformanceRepository,
                                   QuizAnswerRepository quizAnswerRepository,
                                   QuizAttemptRepository quizAttemptRepository,
                                   StudySessionRepository studySessionRepository,
                                   RestTemplateBuilder restTemplateBuilder,
                                   @Value("${focusflow.ml.service.connect-timeout-ms:2000}") int connectTimeoutMs,
                                   @Value("${focusflow.ml.service.read-timeout-ms:3000}") int readTimeoutMs) {
        this.topicRepository = topicRepository;
        this.topicPerformanceRepository = topicPerformanceRepository;
        this.quizAnswerRepository = quizAnswerRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.studySessionRepository = studySessionRepository;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }

    /**
     * Evaluates and updates the performance classification for a single topic.
     */
    @Transactional
    public TopicPerformanceDto evaluateTopic(Long topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + topicId));

        // 1. Aggregate Quiz Accuracy
        long totalQuestions = quizAnswerRepository.countByTopicId(topicId);
        long correctQuestions = quizAnswerRepository.countCorrectByTopicId(topicId);
        double accuracyPercentage = totalQuestions > 0
                ? Math.round((correctQuestions * 1000.0) / totalQuestions) / 10.0
                : 0.0;

        // 2. Aggregate Quiz Attempts
        List<QuizAttempt> attempts = quizAttemptRepository.findByTopicIdOrderByCompletedAtDesc(topicId);
        int attemptCount = attempts.size();
        double avgScore = 0.0;
        Double lastScore = null;

        if (attemptCount > 0) {
            double totalScore = attempts.stream().mapToDouble(QuizAttempt::getPercentage).sum();
            avgScore = Math.round((totalScore / attemptCount) * 10.0) / 10.0;
            lastScore = attempts.get(0).getPercentage();
        } else if (totalQuestions > 0) {
            avgScore = accuracyPercentage;
            attemptCount = (int) Math.ceil(totalQuestions / 3.0);
        }

        // 3. Aggregate Study Duration
        List<StudySession> sessions = studySessionRepository.findByTopicId(topicId);
        int totalStudyMinutes = sessions.stream()
                .mapToInt(s -> s.getActualMinutes() > 0 ? s.getActualMinutes() : s.getPlannedMinutes())
                .sum();

        // 4. ML or Rule-Based Classification
        PerformanceLevel level;
        double confidence;
        String predictionSource;
        String recommendation;

        MlWeakTopicResponse mlResponse = callMlMicroservice(topic, avgScore, attemptCount, totalStudyMinutes, lastScore);
        if (mlResponse != null) {
            level = parsePerformanceLevel(mlResponse.getPerformanceLevel());
            confidence = mlResponse.getConfidence();
            predictionSource = mlResponse.getPredictionSource();
            recommendation = mlResponse.getExplanation();
        } else {
            // Transparent Rule-Based Fallback
            ClassificationResult ruleResult = applyRuleBasedFallback(topic, avgScore, attemptCount, accuracyPercentage, totalStudyMinutes);
            level = ruleResult.level;
            confidence = ruleResult.confidence;
            predictionSource = "RULE_BASED";
            recommendation = ruleResult.recommendation;
        }

        // 5. Persist or Update TopicPerformance
        TopicPerformance perf = topicPerformanceRepository.findByTopicId(topicId)
                .orElse(new TopicPerformance());

        perf.setTopic(topic);
        perf.setPerformanceLevel(level);
        perf.setAverageQuizScore(avgScore);
        perf.setAttemptCount(attemptCount);
        perf.setAccuracyPercentage(accuracyPercentage);
        perf.setTotalStudyMinutes(totalStudyMinutes);
        perf.setPredictionSource(predictionSource);
        perf.setConfidenceScore(confidence);
        perf.setRecommendation(recommendation);
        perf.setLastEvaluatedAt(LocalDateTime.now());

        TopicPerformance saved = topicPerformanceRepository.save(perf);
        return mapToDto(saved);
    }

    /**
     * Evaluates performance across all active topics.
     */
    @Transactional
    public List<TopicPerformanceDto> evaluateAllTopics() {
        List<Topic> topics = topicRepository.findAll();
        List<TopicPerformanceDto> results = new ArrayList<>();
        for (Topic t : topics) {
            results.add(evaluateTopic(t.getId()));
        }
        return results;
    }

    /**
     * Retrieves all topic performance records, evaluating if uninitialized.
     */
    @Transactional
    public List<TopicPerformanceDto> getAllPerformances() {
        List<TopicPerformance> list = topicPerformanceRepository.findAll();
        if (list.isEmpty()) {
            return evaluateAllTopics();
        }
        return list.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    /**
     * Retrieves all topics currently classified as WEAK.
     */
    public List<TopicPerformanceDto> getWeakTopics() {
        return topicPerformanceRepository.findByPerformanceLevel(PerformanceLevel.WEAK)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    /**
     * Retrieves performance for a specific topic.
     */
    public TopicPerformanceDto getPerformanceByTopicId(Long topicId) {
        return topicPerformanceRepository.findByTopicId(topicId)
                .map(this::mapToDto)
                .orElseGet(() -> evaluateTopic(topicId));
    }

    private MlWeakTopicResponse callMlMicroservice(Topic topic, double avgScore, int attempts, int studyMin, Double lastScore) {
        if (!mlEnabled) {
            return null;
        }

        String url = mlServiceUrl + "/ml/predict-weak-topics";
        MlWeakTopicRequest req = new MlWeakTopicRequest(
                topic.getId(),
                topic.getDifficulty().name(),
                avgScore,
                attempts,
                0, // revisions
                studyMin,
                lastScore
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<MlWeakTopicRequest> entity = new HttpEntity<>(req, headers);

        try {
            ResponseEntity<MlWeakTopicResponse> response = restTemplate.postForEntity(url, entity, MlWeakTopicResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (RestClientException ex) {
            logger.warn("ML microservice unavailable at {}. Gracefully applying rule-based fallback for topic '{}': {}",
                    url, topic.getName(), ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error contacting ML microservice for topic '{}': {}", topic.getName(), ex.getMessage());
        }
        return null;
    }

    private ClassificationResult applyRuleBasedFallback(Topic topic, double avgScore, int attempts, double accuracy, int studyMin) {
        // Scenario A: No quiz assessment data yet
        if (attempts == 0 && accuracy == 0.0) {
            if (studyMin >= 60) {
                return new ClassificationResult(
                        PerformanceLevel.MODERATE,
                        0.70,
                        "Rule-Based: Topic has " + studyMin + "m of study but no quiz attempts. Complete an assessment to verify retention."
                );
            } else {
                return new ClassificationResult(
                        PerformanceLevel.WEAK,
                        0.75,
                        "Rule-Based: Topic is unassessed with only " + studyMin + "m study time. Prioritize in your study schedule."
                );
            }
        }

        double effectiveScore = attempts > 0 ? avgScore : accuracy;

        // Scenario B: High Mastery
        if (effectiveScore >= 75.0) {
            return new ClassificationResult(
                    PerformanceLevel.STRONG,
                    0.90,
                    "Rule-Based: Solid performance (" + effectiveScore + "% score). Maintain retention through spaced revision."
            );
        }

        // Scenario C: Below Passing Threshold
        if (effectiveScore < 50.0) {
            return new ClassificationResult(
                    PerformanceLevel.WEAK,
                    0.88,
                    "Rule-Based: Quiz score (" + effectiveScore + "%) is below passing threshold. Needs focused review and remedial practice."
            );
        }

        // Scenario D: Borderline on HARD difficulty
        if (topic.getDifficulty() == Difficulty.HARD && effectiveScore < 60.0 && attempts >= 2) {
            return new ClassificationResult(
                    PerformanceLevel.WEAK,
                    0.80,
                    "Rule-Based: Score (" + effectiveScore + "%) on HARD difficulty indicates persistent conceptual struggle."
            );
        }

        // Scenario E: Moderate Mastery
        return new ClassificationResult(
                PerformanceLevel.MODERATE,
                0.75,
                "Rule-Based: Moderate mastery (" + effectiveScore + "% score). Practice missed concepts to advance to STRONG."
        );
    }

    private PerformanceLevel parsePerformanceLevel(String levelStr) {
        if (levelStr != null) {
            try {
                return PerformanceLevel.valueOf(levelStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Unknown performance level '{}' from ML service, defaulting to MODERATE", levelStr);
            }
        }
        return PerformanceLevel.MODERATE;
    }

    private TopicPerformanceDto mapToDto(TopicPerformance p) {
        Topic t = p.getTopic();
        return new TopicPerformanceDto(
                p.getId(),
                t.getId(),
                t.getName(),
                t.getSubject() != null ? t.getSubject().getId() : null,
                t.getSubject() != null ? t.getSubject().getName() : "General",
                t.getDifficulty(),
                p.getPerformanceLevel(),
                p.getAverageQuizScore(),
                p.getAttemptCount(),
                p.getAccuracyPercentage(),
                p.getTotalStudyMinutes(),
                p.getPredictionSource(),
                p.getConfidenceScore(),
                p.getRecommendation(),
                p.getLastEvaluatedAt()
        );
    }

    private static class ClassificationResult {
        final PerformanceLevel level;
        final double confidence;
        final String recommendation;

        ClassificationResult(PerformanceLevel level, double confidence, String recommendation) {
            this.level = level;
            this.confidence = confidence;
            this.recommendation = recommendation;
        }
    }
}
