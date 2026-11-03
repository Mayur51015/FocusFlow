package com.focusflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.focusflow.dto.*;
import com.focusflow.entity.*;
import com.focusflow.exception.ResourceNotFoundException;
import com.focusflow.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for calculating, persisting, and retrieving Exam Readiness evaluations.
 * Integrates with Python FastAPI ML service with a transparent rule-based fallback.
 */
@Service
public class ExamReadinessService {

    private static final Logger logger = LoggerFactory.getLogger(ExamReadinessService.class);

    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final TopicPerformanceRepository topicPerformanceRepository;
    private final RevisionSessionRepository revisionSessionRepository;
    private final RevisionHistoryRepository revisionHistoryRepository;
    private final ExamReadinessRepository examReadinessRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ml.service.url:http://localhost:8000}")
    private String mlServiceBaseUrl;

    public ExamReadinessService(SubjectRepository subjectRepository,
                                TopicRepository topicRepository,
                                QuizRepository quizRepository,
                                QuizAttemptRepository quizAttemptRepository,
                                TopicPerformanceRepository topicPerformanceRepository,
                                RevisionSessionRepository revisionSessionRepository,
                                RevisionHistoryRepository revisionHistoryRepository,
                                ExamReadinessRepository examReadinessRepository,
                                RestTemplateBuilder restTemplateBuilder,
                                ObjectMapper objectMapper) {
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.quizRepository = quizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.topicPerformanceRepository = topicPerformanceRepository;
        this.revisionSessionRepository = revisionSessionRepository;
        this.revisionHistoryRepository = revisionHistoryRepository;
        this.examReadinessRepository = examReadinessRepository;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(3000))
                .setReadTimeout(Duration.ofMillis(3000))
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Evaluates exam readiness for a single subject and persists the result.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExamReadinessDto calculateSubjectReadiness(Long subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + subjectId));

        List<Topic> topics = topicRepository.findBySubjectId(subjectId);
        int totalTopics = topics.size();
        long completedTopics = topics.stream().filter(Topic::isCompleted).count();
        double topicCompletionRate = totalTopics > 0 ? (double) completedTopics / totalTopics : 0.0;

        // Quiz metrics
        List<Quiz> quizzes = quizRepository.findBySubjectIdOrderByIdDesc(subjectId);
        List<QuizAttempt> allAttempts = new ArrayList<>();
        for (Quiz q : quizzes) {
            allAttempts.addAll(quizAttemptRepository.findByQuizIdOrderByCompletedAtDesc(q.getId()));
        }

        double averageQuizScore = 0.0;
        Double recentTrend = null;
        if (!allAttempts.isEmpty()) {
            averageQuizScore = allAttempts.stream()
                    .mapToDouble(QuizAttempt::getPercentage)
                    .average()
                    .orElse(0.0);

            // Last 3 attempts for trend
            allAttempts.sort((a, b) -> {
                LocalDateTime timeB = b.getCompletedAt() != null ? b.getCompletedAt() : b.getStartedAt();
                LocalDateTime timeA = a.getCompletedAt() != null ? a.getCompletedAt() : a.getStartedAt();
                return timeB.compareTo(timeA);
            });
            recentTrend = allAttempts.stream()
                    .limit(3)
                    .mapToDouble(QuizAttempt::getPercentage)
                    .average()
                    .orElse(averageQuizScore);
        } else {
            recentTrend = 0.0;
        }

        // Weak topics count
        int weakTopicCount = 0;
        for (Topic t : topics) {
            Optional<TopicPerformance> perfOpt = topicPerformanceRepository.findByTopicId(t.getId());
            if (perfOpt.isPresent() && perfOpt.get().getPerformanceLevel() == PerformanceLevel.WEAK) {
                weakTopicCount++;
            }
        }
        double weakTopicRatio = totalTopics > 0 ? (double) weakTopicCount / totalTopics : 0.0;

        // Revision consistency
        double revisionCompletionRate = 0.0;
        if (totalTopics > 0) {
            List<Long> topicIds = topics.stream().map(Topic::getId).toList();
            List<RevisionSession> revisions = revisionSessionRepository.findByTopicIdIn(topicIds);
            long reviewCount = revisionHistoryRepository.findByTopicIdIn(topicIds).size();
            if (!revisions.isEmpty() || reviewCount > 0) {
                long totalSched = Math.max(revisions.size(), reviewCount);
                revisionCompletionRate = Math.min(1.0, (double) reviewCount / Math.max(1, totalSched));
            } else if (completedTopics > 0) {
                revisionCompletionRate = 0.5; // neutral baseline for completed topics without history
            }
        }

        int daysUntilExam = subject.getExamDate() != null ? (int) ChronoUnit.DAYS.between(LocalDate.now(), subject.getExamDate()) : 0;

        // Prepare ML Request
        MlReadinessRequest mlReq = new MlReadinessRequest(
                subject.getId(),
                subject.getName(),
                topicCompletionRate,
                averageQuizScore,
                weakTopicRatio,
                revisionCompletionRate,
                daysUntilExam,
                subject.getImportance() != null ? subject.getImportance().name() : "MEDIUM",
                recentTrend
        );

        double readinessScore;
        ReadinessLevel readinessLevel;
        String predictionSource;
        List<String> recommendations;

        try {
            String url = mlServiceBaseUrl + "/ml/predict-exam-readiness";
            ResponseEntity<MlReadinessResponse> response = restTemplate.postForEntity(url, mlReq, MlReadinessResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                MlReadinessResponse body = response.getBody();
                readinessScore = body.getReadinessScore();
                readinessLevel = ReadinessLevel.valueOf(body.getReadinessLevel());
                predictionSource = body.getPredictionSource();
                recommendations = body.getRecommendations() != null ? body.getRecommendations() : new ArrayList<>();
                logger.info("ML Readiness evaluation received for subject '{}': {}% ({}) from {}",
                        subject.getName(), readinessScore, readinessLevel, predictionSource);
            } else {
                throw new RuntimeException("Unsuccessful status from ML service");
            }
        } catch (Exception ex) {
            logger.warn("ML microservice unavailable at {}. Gracefully applying rule-based fallback for subject '{}': {}",
                    mlServiceBaseUrl, subject.getName(), ex.getMessage());

            RuleBasedCalculation fallback = calculateRuleBased(
                    topicCompletionRate, averageQuizScore, weakTopicRatio,
                    revisionCompletionRate, daysUntilExam,
                    subject.getImportance() != null ? subject.getImportance().name() : "MEDIUM",
                    recentTrend
            );
            readinessScore = fallback.score;
            readinessLevel = fallback.level;
            predictionSource = "RULE_BASED_FALLBACK";
            recommendations = fallback.recommendations;
        }

        String recsJson;
        try {
            recsJson = objectMapper.writeValueAsString(recommendations);
        } catch (Exception e) {
            recsJson = "[]";
        }

        // Persist evaluation
        ExamReadiness evaluation = new ExamReadiness(
                subject,
                readinessScore,
                readinessLevel,
                round(topicCompletionRate, 4),
                round(averageQuizScore, 2),
                weakTopicCount,
                round(weakTopicRatio, 4),
                round(revisionCompletionRate, 4),
                daysUntilExam,
                predictionSource,
                recsJson,
                LocalDateTime.now()
        );

        ExamReadiness saved = examReadinessRepository.save(evaluation);
        return mapToDto(saved, recommendations);
    }

    /**
     * Retrieves the latest exam readiness evaluation for a subject, or computes one if none exists.
     */
    @Transactional
    public ExamReadinessDto getSubjectReadiness(Long subjectId) {
        return examReadinessRepository.findTopBySubjectIdOrderByEvaluatedAtDesc(subjectId)
                .map(this::mapToDto)
                .orElseGet(() -> calculateSubjectReadiness(subjectId));
    }

    /**
     * Calculates exam readiness for all active subjects.
     */
    @Transactional
    public List<ExamReadinessDto> calculateAllReadiness() {
        List<Subject> subjects = subjectRepository.findAll();
        List<ExamReadinessDto> results = new ArrayList<>();
        for (Subject s : subjects) {
            results.add(calculateSubjectReadiness(s.getId()));
        }
        return results;
    }

    /**
     * Aggregates an overall Exam Readiness Overview across all enrolled subjects.
     */
    @Transactional(readOnly = true)
    public ExamReadinessOverviewDto getReadinessOverview() {
        List<Subject> subjects = subjectRepository.findAll();
        List<ExamReadinessDto> evaluations = new ArrayList<>();

        for (Subject s : subjects) {
            evaluations.add(
                    examReadinessRepository.findTopBySubjectIdOrderByEvaluatedAtDesc(s.getId())
                            .map(this::mapToDto)
                            .orElseGet(() -> {
                                int days = s.getExamDate() != null ? (int) ChronoUnit.DAYS.between(LocalDate.now(), s.getExamDate()) : 0;
                                ExamReadinessDto dto = new ExamReadinessDto();
                                dto.setSubjectId(s.getId());
                                dto.setSubjectName(s.getName());
                                dto.setImportance(s.getImportance());
                                dto.setExamDate(s.getExamDate());
                                dto.setDaysUntilExam(days);
                                dto.setReadinessScore(0.0);
                                dto.setReadinessLevel(ReadinessLevel.LOW);
                                dto.setPredictionSource("PENDING_EVALUATION");
                                dto.setRecommendations(List.of("Perform initial readiness evaluation."));
                                dto.setEvaluatedAt(LocalDateTime.now());
                                return dto;
                            })
            );
        }

        int total = evaluations.size();
        double avgScore = total > 0
                ? evaluations.stream().mapToDouble(ExamReadinessDto::getReadinessScore).average().orElse(0.0)
                : 0.0;
        avgScore = round(avgScore, 1);

        ReadinessLevel overallLevel;
        if (avgScore >= 75.0) overallLevel = ReadinessLevel.HIGH;
        else if (avgScore >= 50.0) overallLevel = ReadinessLevel.MODERATE;
        else overallLevel = ReadinessLevel.LOW;

        int high = (int) evaluations.stream().filter(e -> e.getReadinessLevel() == ReadinessLevel.HIGH).count();
        int mod = (int) evaluations.stream().filter(e -> e.getReadinessLevel() == ReadinessLevel.MODERATE).count();
        int low = (int) evaluations.stream().filter(e -> e.getReadinessLevel() == ReadinessLevel.LOW).count();
        int urgent = (int) evaluations.stream().filter(e -> e.getDaysUntilExam() <= 7).count();

        ExamReadinessOverviewDto overview = new ExamReadinessOverviewDto();
        overview.setOverallReadinessScore(avgScore);
        overview.setOverallReadinessLevel(overallLevel);
        overview.setTotalSubjects(total);
        overview.setHighReadinessCount(high);
        overview.setModerateReadinessCount(mod);
        overview.setLowReadinessCount(low);
        overview.setUrgentExamsCount(urgent);
        overview.setSubjectEvaluations(evaluations);

        return overview;
    }

    // --- Transparent Rule-Based Engine & Recommendations Fallback ---
    private RuleBasedCalculation calculateRuleBased(
            double topicComp, double quizAvg, double weakRatio,
            double revComp, int daysLeft, String importance, double trend
    ) {
        double compPts = topicComp * 100.0 * 0.35;
        double quizPts = quizAvg * 0.35;
        double revPts = revComp * 100.0 * 0.20;
        double trendPts = trend * 0.10;
        double raw = compPts + quizPts + revPts + trendPts;

        double weakPenalty = weakRatio > 0 ? Math.min(15.0, weakRatio * 20.0) : 0.0;
        double urgentPenalty = (daysLeft <= 3 && topicComp < 0.50) ? 15.0 : 0.0;
        double impPenalty = ("HIGH".equalsIgnoreCase(importance) && topicComp < 0.70) ? 5.0 : 0.0;

        double finalScore = Math.max(0.0, Math.min(100.0, round(raw - (weakPenalty + urgentPenalty + impPenalty), 1)));

        ReadinessLevel level;
        if (finalScore >= 75.0) level = ReadinessLevel.HIGH;
        else if (finalScore >= 50.0) level = ReadinessLevel.MODERATE;
        else level = ReadinessLevel.LOW;

        List<String> recs = new ArrayList<>();
        if (daysLeft <= 3 && topicComp < 0.60) {
            recs.add("⚠️ Urgent: Exam is in " + daysLeft + " day(s) with only " + Math.round(topicComp * 100) + "% completed. Focus on flashcards and high-yield topics.");
        } else if (daysLeft <= 7) {
            recs.add("⏰ Exam approaching in " + daysLeft + " days. Transition to practice quizzes and spaced recall.");
        }
        if (weakRatio > 0.0) {
            recs.add("🔍 Address your identified weak topics (" + Math.round(weakRatio * 100) + "% of syllabus) to prevent score drops.");
        }
        if (topicComp < 0.70) {
            recs.add("📚 Syllabus completion is at " + Math.round(topicComp * 100) + "%. Schedule remaining topics into daily study sessions.");
        }
        if (quizAvg < 60.0) {
            recs.add("📝 Quiz average is " + round(quizAvg, 1) + "%. Take timed topic assessments to identify knowledge gaps.");
        }
        if (revComp < 0.50) {
            recs.add("🧠 Practice scheduled SM-2 flashcard decks daily to reinforce long-term memory.");
        }
        if ("HIGH".equalsIgnoreCase(importance) && finalScore < 75.0) {
            recs.add("🎯 HIGH priority subject: Allocate at least 60-90 minutes daily in the study planner.");
        }
        if (recs.isEmpty()) {
            recs.add("✅ Strong overall preparation! Keep reviewing flashcards to lock in retention before exam day.");
        }

        return new RuleBasedCalculation(finalScore, level, recs);
    }

    private static class RuleBasedCalculation {
        final double score;
        final ReadinessLevel level;
        final List<String> recommendations;

        RuleBasedCalculation(double score, ReadinessLevel level, List<String> recommendations) {
            this.score = score;
            this.level = level;
            this.recommendations = recommendations;
        }
    }

    private ExamReadinessDto mapToDto(ExamReadiness r) {
        List<String> recs = new ArrayList<>();
        if (r.getRecommendations() != null && !r.getRecommendations().isEmpty()) {
            try {
                recs = objectMapper.readValue(r.getRecommendations(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                recs.add(r.getRecommendations());
            }
        }
        return mapToDto(r, recs);
    }

    private ExamReadinessDto mapToDto(ExamReadiness r, List<String> recs) {
        ExamReadinessDto dto = new ExamReadinessDto();
        dto.setId(r.getId());
        dto.setSubjectId(r.getSubject().getId());
        dto.setSubjectName(r.getSubject().getName());
        dto.setImportance(r.getSubject().getImportance());
        dto.setExamDate(r.getSubject().getExamDate());
        dto.setDaysUntilExam(r.getDaysUntilExam());
        dto.setReadinessScore(r.getReadinessScore());
        dto.setReadinessLevel(r.getReadinessLevel());
        dto.setTopicCompletionRate(r.getTopicCompletionRate());
        dto.setAverageQuizScore(r.getAverageQuizScore());
        dto.setWeakTopicCount(r.getWeakTopicCount());
        dto.setWeakTopicRatio(r.getWeakTopicRatio());
        dto.setRevisionCompletionRate(r.getRevisionCompletionRate());
        dto.setPredictionSource(r.getPredictionSource());
        dto.setRecommendations(recs);
        dto.setEvaluatedAt(r.getEvaluatedAt());
        return dto;
    }

    private double round(double val, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(val * factor) / factor;
    }
}
