package com.focusflow.service;

import com.focusflow.dto.*;
import com.focusflow.entity.*;
import com.focusflow.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for consolidating learning data into academic performance analytics,
 * subject mastery metrics, spaced repetition retention rates, and recommended study focus.
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);

    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final TopicPerformanceRepository topicPerformanceRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final RevisionSessionRepository revisionSessionRepository;
    private final RevisionHistoryRepository revisionHistoryRepository;
    private final FlashcardRepository flashcardRepository;
    private final ExamReadinessService examReadinessService;

    public AnalyticsService(SubjectRepository subjectRepository,
                            TopicRepository topicRepository,
                            TopicPerformanceRepository topicPerformanceRepository,
                            QuizRepository quizRepository,
                            QuizAttemptRepository quizAttemptRepository,
                            RevisionSessionRepository revisionSessionRepository,
                            RevisionHistoryRepository revisionHistoryRepository,
                            FlashcardRepository flashcardRepository,
                            ExamReadinessService examReadinessService) {
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.topicPerformanceRepository = topicPerformanceRepository;
        this.quizRepository = quizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.revisionSessionRepository = revisionSessionRepository;
        this.revisionHistoryRepository = revisionHistoryRepository;
        this.flashcardRepository = flashcardRepository;
        this.examReadinessService = examReadinessService;
    }

    /**
     * Aggregates a comprehensive learning summary for the student.
     */
    public LearningSummaryDto getLearningSummary() {
        LearningSummaryDto summary = new LearningSummaryDto();

        List<Topic> allTopics = topicRepository.findAll();
        int totalTopics = allTopics.size();
        summary.setTotalTopics(totalTopics);

        // Topic mastery classification counts
        List<TopicPerformance> performances = topicPerformanceRepository.findAll();
        int strong = 0;
        int moderate = 0;
        int weak = 0;

        for (TopicPerformance tp : performances) {
            if (tp.getPerformanceLevel() == PerformanceLevel.STRONG) strong++;
            else if (tp.getPerformanceLevel() == PerformanceLevel.MODERATE) moderate++;
            else if (tp.getPerformanceLevel() == PerformanceLevel.WEAK) weak++;
        }

        // Topics without explicit performance record default to MODERATE or unassessed
        int unassessed = Math.max(0, totalTopics - (strong + moderate + weak));
        moderate += unassessed;

        summary.setStrongTopicsCount(strong);
        summary.setModerateTopicsCount(moderate);
        summary.setWeakTopicsCount(weak);

        double masteryScore = totalTopics > 0
                ? ((strong * 100.0) + (moderate * 60.0) + (weak * 25.0)) / totalTopics
                : 0.0;
        masteryScore = round(masteryScore, 1);
        summary.setOverallMasteryScore(masteryScore);

        if (masteryScore >= 75.0) summary.setOverallMasteryLevel("STRONG");
        else if (masteryScore >= 50.0) summary.setOverallMasteryLevel("MODERATE");
        else summary.setOverallMasteryLevel("WEAK");

        // Spaced repetition retention rate
        List<RevisionHistory> histories = revisionHistoryRepository.findAll();
        int totalReviews = histories.size();
        summary.setTotalRevisionReviews(totalReviews);

        long successfulRecallCount = histories.stream()
                .filter(h -> h.getRating() == RecallRating.GOOD || h.getRating() == RecallRating.EASY)
                .count();

        double retentionRate = totalReviews > 0
                ? ((double) successfulRecallCount / totalReviews) * 100.0
                : 100.0;
        summary.setRetentionRatePercentage(round(retentionRate, 1));
        summary.setTotalFlashcards((int) flashcardRepository.count());

        // Quizzes metrics
        List<QuizAttempt> attempts = quizAttemptRepository.findAll();
        summary.setTotalQuizAttempts(attempts.size());
        double quizAvg = attempts.isEmpty()
                ? 0.0
                : attempts.stream().mapToDouble(QuizAttempt::getPercentage).average().orElse(0.0);
        summary.setOverallQuizAverage(round(quizAvg, 1));

        // Global Exam Readiness Overview
        try {
            ExamReadinessOverviewDto readinessOverview = examReadinessService.getReadinessOverview();
            summary.setOverallReadinessScore(readinessOverview.getOverallReadinessScore());
            summary.setOverallReadinessLevel(readinessOverview.getOverallReadinessLevel().name());
        } catch (Exception e) {
            logger.warn("Could not retrieve exam readiness for learning summary: {}", e.getMessage());
            summary.setOverallReadinessScore(0.0);
            summary.setOverallReadinessLevel("LOW");
        }

        // Recommended Study Focus
        summary.setRecommendedStudyFocus(computeRecommendedStudyFocus(allTopics, performances));

        return summary;
    }

    /**
     * Calculates mastery and exam readiness breakdowns per subject.
     */
    public List<SubjectMasteryDto> getSubjectMastery() {
        List<Subject> subjects = subjectRepository.findAll();
        List<SubjectMasteryDto> results = new ArrayList<>();

        for (Subject s : subjects) {
            SubjectMasteryDto dto = new SubjectMasteryDto();
            dto.setSubjectId(s.getId());
            dto.setSubjectName(s.getName());
            dto.setImportance(s.getImportance() != null ? s.getImportance().name() : "MEDIUM");
            dto.setExamDate(s.getExamDate());
            int daysLeft = (int) ChronoUnit.DAYS.between(LocalDate.now(), s.getExamDate());
            dto.setDaysUntilExam(daysLeft);

            List<Topic> topics = topicRepository.findBySubjectId(s.getId());
            int total = topics.size();
            long completed = topics.stream().filter(Topic::isCompleted).count();
            dto.setTotalTopics(total);
            dto.setCompletedTopics((int) completed);
            dto.setTopicCompletionRate(total > 0 ? round((double) completed / total, 2) : 0.0);

            // Performance classification counts for this subject
            int sCount = 0;
            int mCount = 0;
            int wCount = 0;
            for (Topic t : topics) {
                Optional<TopicPerformance> pOpt = topicPerformanceRepository.findByTopicId(t.getId());
                if (pOpt.isPresent()) {
                    PerformanceLevel level = pOpt.get().getPerformanceLevel();
                    if (level == PerformanceLevel.STRONG) sCount++;
                    else if (level == PerformanceLevel.WEAK) wCount++;
                    else mCount++;
                } else {
                    mCount++;
                }
            }
            dto.setStrongTopicsCount(sCount);
            dto.setModerateTopicsCount(mCount);
            dto.setWeakTopicsCount(wCount);

            // Quizzes for subject
            List<Quiz> quizzes = quizRepository.findBySubjectIdOrderByIdDesc(s.getId());
            List<QuizAttempt> sAttempts = new ArrayList<>();
            for (Quiz q : quizzes) {
                sAttempts.addAll(quizAttemptRepository.findByQuizIdOrderByCompletedAtDesc(q.getId()));
            }
            dto.setQuizAttemptsCount(sAttempts.size());
            double qAvg = sAttempts.isEmpty() ? 0.0 : sAttempts.stream().mapToDouble(QuizAttempt::getPercentage).average().orElse(0.0);
            dto.setAverageQuizScore(round(qAvg, 1));

            // Exam readiness for this subject
            try {
                ExamReadinessDto er = examReadinessService.getSubjectReadiness(s.getId());
                dto.setReadinessScore(er.getReadinessScore());
                dto.setReadinessLevel(er.getReadinessLevel().name());
            } catch (Exception e) {
                dto.setReadinessScore(0.0);
                dto.setReadinessLevel("LOW");
            }

            // Overall mastery tier
            if (dto.getReadinessScore() >= 75.0 || (total > 0 && sCount > wCount && sCount > mCount)) {
                dto.setOverallMasteryLevel("STRONG");
            } else if (dto.getReadinessScore() < 50.0 || wCount > sCount) {
                dto.setOverallMasteryLevel("WEAK");
            } else {
                dto.setOverallMasteryLevel("MODERATE");
            }

            results.add(dto);
        }

        return results;
    }

    /**
     * Aggregates spaced repetition retention trends and historical assessment scores.
     */
    public RetentionTrendsDto getRetentionTrends() {
        RetentionTrendsDto dto = new RetentionTrendsDto();

        List<RevisionHistory> histories = revisionHistoryRepository.findAllByOrderByReviewedAtDesc();
        dto.setTotalReviews(histories.size());

        int again = 0;
        int hard = 0;
        int good = 0;
        int easy = 0;

        for (RevisionHistory h : histories) {
            if (h.getRating() == RecallRating.AGAIN) again++;
            else if (h.getRating() == RecallRating.HARD) hard++;
            else if (h.getRating() == RecallRating.GOOD) good++;
            else if (h.getRating() == RecallRating.EASY) easy++;
        }

        dto.setAgainCount(again);
        dto.setHardCount(hard);
        dto.setGoodCount(good);
        dto.setEasyCount(easy);

        double retentionRate = histories.isEmpty()
                ? 100.0
                : ((double) (good + easy) / histories.size()) * 100.0;
        dto.setRetentionRatePercentage(round(retentionRate, 1));

        // Average EF across active revision sessions
        List<RevisionSession> sessions = revisionSessionRepository.findAll();
        double avgEf = sessions.isEmpty()
                ? 2.5
                : sessions.stream().mapToDouble(RevisionSession::getEasinessFactor).average().orElse(2.5);
        dto.setAverageEasinessFactor(round(avgEf, 2));

        // Recent 10 revision history entries
        List<RevisionHistoryDto> recentRevDtos = histories.stream()
                .limit(10)
                .map(h -> new RevisionHistoryDto(
                        h.getId(),
                        h.getRevisionSession() != null ? h.getRevisionSession().getId() : null,
                        h.getTopic().getId(),
                        h.getTopic().getName(),
                        h.getTopic().getSubject().getName(),
                        h.getRating(),
                        h.getReviewedAt(),
                        h.getPreviousIntervalDays(),
                        h.getNewIntervalDays(),
                        h.getPreviousEf(),
                        h.getNewEf(),
                        h.getNotes()
                ))
                .collect(Collectors.toList());
        dto.setRecentReviews(recentRevDtos);

        // Recent 15 quiz attempt scores
        List<QuizAttempt> attempts = quizAttemptRepository.findAllByOrderByCompletedAtDesc();
        List<QuizScoreTrendDto> quizTrends = attempts.stream()
                .limit(15)
                .map(qa -> new QuizScoreTrendDto(
                        qa.getId(),
                        qa.getQuiz().getTitle(),
                        qa.getQuiz().getSubject().getName(),
                        qa.getPercentage(),
                        qa.isPassed(),
                        qa.getCompletedAt() != null ? qa.getCompletedAt() : qa.getStartedAt()
                ))
                .collect(Collectors.toList());
        dto.setHistoricalQuizScores(quizTrends);

        return dto;
    }

    private List<DailyStudyFocusDto> computeRecommendedStudyFocus(
            List<Topic> allTopics, List<TopicPerformance> performances
    ) {
        List<DailyStudyFocusDto> recommendations = new ArrayList<>();
        Map<Long, TopicPerformance> perfMap = performances.stream()
                .collect(Collectors.toMap(p -> p.getTopic().getId(), p -> p, (a, b) -> a));

        // 1. Weak topics
        for (Topic t : allTopics) {
            TopicPerformance tp = perfMap.get(t.getId());
            if (tp != null && tp.getPerformanceLevel() == PerformanceLevel.WEAK) {
                recommendations.add(new DailyStudyFocusDto(
                        t.getId(),
                        t.getName(),
                        t.getSubject().getId(),
                        t.getSubject().getName(),
                        t.getDifficulty().name(),
                        "Identified as WEAK (" + round(tp.getAccuracyPercentage(), 1) + "% quiz score). Requires immediate review.",
                        95,
                        "QUIZ"
                ));
            }
        }

        // 2. Due / Overdue revisions
        List<RevisionSession> dueRevs = revisionSessionRepository
                .findByScheduledDateLessThanEqualAndStatusInOrderByScheduledDateAsc(
                        LocalDate.now(), List.of(RevisionStatus.PENDING, RevisionStatus.OVERDUE)
                );
        for (RevisionSession rev : dueRevs) {
            boolean alreadyAdded = recommendations.stream().anyMatch(r -> r.getTopicId().equals(rev.getTopic().getId()));
            if (!alreadyAdded) {
                long daysOverdue = ChronoUnit.DAYS.between(rev.getScheduledDate(), LocalDate.now());
                String reason = daysOverdue > 0
                        ? "Spaced repetition is " + daysOverdue + " day(s) overdue. Reinforce active recall."
                        : "Scheduled for SM-2 spaced repetition today (Repetition #" + rev.getRepetitionNumber() + ").";
                recommendations.add(new DailyStudyFocusDto(
                        rev.getTopic().getId(),
                        rev.getTopic().getName(),
                        rev.getTopic().getSubject().getId(),
                        rev.getTopic().getSubject().getName(),
                        rev.getTopic().getDifficulty().name(),
                        reason,
                        daysOverdue > 0 ? 90 : 80,
                        "REVISION"
                ));
            }
        }

        // 3. Urgent Exam upcoming with incomplete topics
        for (Topic t : allTopics) {
            if (!t.isCompleted()) {
                int daysUntilExam = (int) ChronoUnit.DAYS.between(LocalDate.now(), t.getSubject().getExamDate());
                if (daysUntilExam <= 3) {
                    boolean alreadyAdded = recommendations.stream().anyMatch(r -> r.getTopicId().equals(t.getId()));
                    if (!alreadyAdded) {
                        recommendations.add(new DailyStudyFocusDto(
                                t.getId(),
                                t.getName(),
                                t.getSubject().getId(),
                                t.getSubject().getName(),
                                t.getDifficulty().name(),
                                "Exam in " + daysUntilExam + " day(s)! Uncompleted syllabus topic.",
                                85,
                                "STUDY"
                        ));
                    }
                }
            }
        }

        // Sort by priorityScore descending and return top 5
        recommendations.sort((a, b) -> Integer.compare(b.getPriorityScore(), a.getPriorityScore()));
        return recommendations.stream().limit(5).collect(Collectors.toList());
    }

    private double round(double val, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(val * factor) / factor;
    }
}

