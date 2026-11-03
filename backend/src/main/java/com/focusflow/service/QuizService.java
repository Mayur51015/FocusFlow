package com.focusflow.service;

import com.focusflow.dto.*;
import com.focusflow.entity.*;
import com.focusflow.exception.BadRequestException;
import com.focusflow.exception.ResourceNotFoundException;
import com.focusflow.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * QuizService handles the creation, listing, attempt execution, objective scoring,
 * and historical audit of academic quizzes.
 */
@Service
@Transactional
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizOptionRepository quizOptionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;

    public QuizService(QuizRepository quizRepository,
                       QuestionRepository questionRepository,
                       QuizOptionRepository quizOptionRepository,
                       QuizAttemptRepository quizAttemptRepository,
                       QuizAnswerRepository quizAnswerRepository,
                       SubjectRepository subjectRepository,
                       TopicRepository topicRepository) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.quizOptionRepository = quizOptionRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizAnswerRepository = quizAnswerRepository;
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
    }

    /**
     * Creates a new quiz with associated questions and multiple-choice options.
     */
    public QuizSummaryDto createQuiz(QuizCreateRequest request) {
        if (request == null) {
            throw new BadRequestException("Quiz request cannot be null");
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new BadRequestException("Quiz title is required");
        }
        if (request.getSubjectId() == null) {
            throw new BadRequestException("Subject ID is required");
        }

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + request.getSubjectId()));

        Topic topic = null;
        if (request.getTopicId() != null) {
            topic = topicRepository.findById(request.getTopicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Topic not found with ID: " + request.getTopicId()));
        }

        if (request.getQuestions() == null || request.getQuestions().isEmpty()) {
            throw new BadRequestException("Quiz must contain at least one question");
        }

        int calculatedTotalMarks = 0;
        for (int i = 0; i < request.getQuestions().size(); i++) {
            QuizCreateRequest.QuestionCreateDto qDto = request.getQuestions().get(i);
            if (qDto.getQuestionText() == null || qDto.getQuestionText().trim().isEmpty()) {
                throw new BadRequestException("Question #" + (i + 1) + " must have non-empty text");
            }
            if (qDto.getOptions() == null || qDto.getOptions().size() < 2) {
                throw new BadRequestException("Question #" + (i + 1) + " must have at least two answer options");
            }
            boolean hasCorrect = qDto.getOptions().stream().anyMatch(QuizCreateRequest.OptionCreateDto::isCorrect);
            if (!hasCorrect) {
                throw new BadRequestException("Question #" + (i + 1) + " must have at least one option marked as correct");
            }
            calculatedTotalMarks += Math.max(1, qDto.getMarks());
        }

        int duration = request.getDurationMinutes() > 0 ? request.getDurationMinutes() : 15;
        int passingMarks = request.getPassingMarks() > 0 ? request.getPassingMarks() : (int) Math.round(calculatedTotalMarks * 0.5);

        Quiz quiz = new Quiz(
                request.getTitle().trim(),
                request.getDescription(),
                subject,
                topic,
                duration,
                calculatedTotalMarks,
                passingMarks
        );

        Quiz savedQuiz = quizRepository.save(quiz);

        for (int i = 0; i < request.getQuestions().size(); i++) {
            QuizCreateRequest.QuestionCreateDto qDto = request.getQuestions().get(i);
            Topic questionTopic = topic;
            if (qDto.getTopicId() != null) {
                questionTopic = topicRepository.findById(qDto.getTopicId()).orElse(topic);
            }

            Question question = new Question(
                    savedQuiz,
                    questionTopic,
                    qDto.getQuestionText().trim(),
                    Math.max(1, qDto.getMarks()),
                    qDto.getExplanation()
            );
            Question savedQuestion = questionRepository.save(question);

            for (int j = 0; j < qDto.getOptions().size(); j++) {
                QuizCreateRequest.OptionCreateDto optDto = qDto.getOptions().get(j);
                QuizOption option = new QuizOption(
                        savedQuestion,
                        optDto.getOptionText().trim(),
                        optDto.isCorrect(),
                        optDto.getOptionOrder() > 0 ? optDto.getOptionOrder() : (j + 1)
                );
                quizOptionRepository.save(option);
            }
        }

        return mapToSummaryDto(savedQuiz);
    }

    /**
     * Lists quizzes optionally filtered by subject or topic.
     */
    @Transactional(readOnly = true)
    public List<QuizSummaryDto> getAllQuizzes(Long subjectId, Long topicId) {
        List<Quiz> quizzes;
        if (subjectId != null && topicId != null) {
            quizzes = quizRepository.findBySubjectIdAndTopicIdOrderByIdDesc(subjectId, topicId);
        } else if (subjectId != null) {
            quizzes = quizRepository.findBySubjectIdOrderByIdDesc(subjectId);
        } else if (topicId != null) {
            quizzes = quizRepository.findByTopicIdOrderByIdDesc(topicId);
        } else {
            quizzes = quizRepository.findAllByOrderByIdDesc();
        }

        return quizzes.stream()
                .map(this::mapToSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves quiz metadata and sanitized student questions for an active attempt.
     * Crucial: Correct answers and explanations are omitted to prevent cheating.
     */
    @Transactional(readOnly = true)
    public QuizDetailDto getQuizForAttempt(Long quizId) {
        if (quizId == null) {
            throw new BadRequestException("Quiz ID cannot be null");
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with ID: " + quizId));

        List<Question> questions = questionRepository.findByQuizIdOrderByIdAsc(quizId);
        List<QuestionStudentDto> studentQuestions = questions.stream().map(q -> {
            List<QuizOption> options = quizOptionRepository.findByQuestionIdOrderByOptionOrderAsc(q.getId());
            List<QuestionStudentDto.OptionStudentDto> studentOptions = options.stream()
                    .map(o -> new QuestionStudentDto.OptionStudentDto(o.getId(), o.getOptionText(), o.getOptionOrder()))
                    .collect(Collectors.toList());

            return new QuestionStudentDto(
                    q.getId(),
                    q.getTopic() != null ? q.getTopic().getId() : null,
                    q.getTopic() != null ? q.getTopic().getName() : null,
                    q.getQuestionText(),
                    q.getMarks(),
                    studentOptions
            );
        }).collect(Collectors.toList());

        return new QuizDetailDto(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getSubject().getId(),
                quiz.getSubject().getName(),
                quiz.getTopic() != null ? quiz.getTopic().getId() : null,
                quiz.getTopic() != null ? quiz.getTopic().getName() : null,
                quiz.getDurationMinutes(),
                quiz.getTotalMarks(),
                quiz.getPassingMarks(),
                quiz.getCreatedAt(),
                studentQuestions
        );
    }

    /**
     * Grades a student's quiz attempt server-side, logs individual answers,
     * calculates score, percentage, and pass/fail result.
     */
    public QuizAttemptResultDto submitQuizAttempt(Long quizId, QuizAttemptSubmitRequest request) {
        if (quizId == null) {
            throw new BadRequestException("Quiz ID cannot be null");
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with ID: " + quizId));

        List<Question> questions = questionRepository.findByQuizIdOrderByIdAsc(quizId);
        if (questions.isEmpty()) {
            throw new BadRequestException("Cannot submit attempt for a quiz with no questions");
        }

        // Map student answers by questionId
        Map<Long, Long> answersMap = new HashMap<>();
        if (request != null && request.getAnswers() != null) {
            for (QuizAttemptSubmitRequest.AnswerSubmissionDto sub : request.getAnswers()) {
                if (sub.getQuestionId() != null) {
                    answersMap.put(sub.getQuestionId(), sub.getSelectedOptionId());
                }
            }
        }

        int totalEarned = 0;
        int totalPossible = 0;
        LocalDateTime now = LocalDateTime.now();
        int timeSpent = (request != null && request.getTimeSpentSeconds() > 0)
                ? request.getTimeSpentSeconds()
                : 60;

        List<QuizAttemptResultDto.AnswerResultDto> resultItems = new ArrayList<>();
        List<QuizAnswer> answersToSave = new ArrayList<>();

        for (Question question : questions) {
            totalPossible += question.getMarks();
            Long selectedOptionId = answersMap.get(question.getId());

            List<QuizOption> options = quizOptionRepository.findByQuestionIdOrderByOptionOrderAsc(question.getId());
            QuizOption correctOption = options.stream().filter(QuizOption::isCorrect).findFirst().orElse(null);
            QuizOption selectedOption = null;

            if (selectedOptionId != null) {
                selectedOption = options.stream()
                        .filter(o -> o.getId().equals(selectedOptionId))
                        .findFirst()
                        .orElse(null);
            }

            boolean isCorrect = false;
            int marksAwarded = 0;

            if (selectedOption != null && selectedOption.isCorrect()) {
                isCorrect = true;
                marksAwarded = question.getMarks();
                totalEarned += marksAwarded;
            }

            QuizAnswer answer = new QuizAnswer();
            answer.setQuestion(question);
            answer.setSelectedOption(selectedOption);
            answer.setCorrect(isCorrect);
            answer.setMarksAwarded(marksAwarded);
            answersToSave.add(answer);

            resultItems.add(new QuizAttemptResultDto.AnswerResultDto(
                    question.getId(),
                    question.getQuestionText(),
                    question.getTopic() != null ? question.getTopic().getId() : null,
                    question.getTopic() != null ? question.getTopic().getName() : null,
                    question.getMarks(),
                    selectedOption != null ? selectedOption.getId() : null,
                    selectedOption != null ? selectedOption.getOptionText() : "Not Answered",
                    correctOption != null ? correctOption.getId() : null,
                    correctOption != null ? correctOption.getOptionText() : "None",
                    isCorrect,
                    marksAwarded,
                    question.getExplanation()
            ));
        }

        double percentage = totalPossible > 0
                ? Math.round(((double) totalEarned / totalPossible) * 1000.0) / 10.0
                : 0.0;
        boolean passed = totalEarned >= quiz.getPassingMarks();

        QuizAttempt attempt = new QuizAttempt(
                quiz,
                totalEarned,
                percentage,
                passed,
                now.minusSeconds(timeSpent),
                now,
                timeSpent
        );

        QuizAttempt savedAttempt = quizAttemptRepository.save(attempt);

        for (QuizAnswer ans : answersToSave) {
            ans.setAttempt(savedAttempt);
            quizAnswerRepository.save(ans);
        }

        return new QuizAttemptResultDto(
                savedAttempt.getId(),
                quiz.getId(),
                quiz.getTitle(),
                quiz.getSubject().getName(),
                quiz.getTopic() != null ? quiz.getTopic().getName() : null,
                totalEarned,
                totalPossible,
                percentage,
                passed,
                quiz.getPassingMarks(),
                savedAttempt.getStartedAt(),
                savedAttempt.getCompletedAt(),
                timeSpent,
                resultItems
        );
    }

    /**
     * Retrieves historical quiz attempts.
     */
    @Transactional(readOnly = true)
    public List<QuizAttemptHistoryDto> getAttemptHistory(Long quizId) {
        List<QuizAttempt> attempts;
        if (quizId != null) {
            attempts = quizAttemptRepository.findByQuizIdOrderByCompletedAtDesc(quizId);
        } else {
            attempts = quizAttemptRepository.findAllByOrderByCompletedAtDesc();
        }

        return attempts.stream().map(a -> new QuizAttemptHistoryDto(
                a.getId(),
                a.getQuiz().getId(),
                a.getQuiz().getTitle(),
                a.getQuiz().getSubject().getName(),
                a.getQuiz().getTopic() != null ? a.getQuiz().getTopic().getName() : null,
                a.getScore(),
                a.getQuiz().getTotalMarks(),
                a.getPercentage(),
                a.isPassed(),
                a.getCompletedAt(),
                a.getTimeSpentSeconds()
        )).collect(Collectors.toList());
    }

    /**
     * Retrieves quiz performance metrics for a specific topic.
     */
    @Transactional(readOnly = true)
    public TopicQuizPerformanceDto getTopicQuizPerformance(Long topicId) {
        if (topicId == null) {
            throw new BadRequestException("Topic ID cannot be null");
        }

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with ID: " + topicId));

        List<QuizAnswer> topicAnswers = quizAnswerRepository.findByTopicId(topicId);
        long totalAttempted = topicAnswers.size();
        long correctCount = topicAnswers.stream().filter(QuizAnswer::isCorrect).count();
        double accuracy = totalAttempted > 0
                ? Math.round(((double) correctCount / totalAttempted) * 1000.0) / 10.0
                : 0.0;

        LocalDateTime lastDate = null;
        if (!topicAnswers.isEmpty()) {
            lastDate = topicAnswers.stream()
                    .map(a -> a.getAttempt().getCompletedAt())
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
        }

        // Count unique attempts for this topic
        long uniqueAttempts = topicAnswers.stream()
                .map(a -> a.getAttempt().getId())
                .distinct()
                .count();

        return new TopicQuizPerformanceDto(
                topic.getId(),
                topic.getName(),
                topic.getSubject().getName(),
                totalAttempted,
                correctCount,
                accuracy,
                uniqueAttempts,
                lastDate
        );
    }

    private QuizSummaryDto mapToSummaryDto(Quiz quiz) {
        long questionCount = questionRepository.countByQuizId(quiz.getId());
        long attemptCount = quizAttemptRepository.countByQuizId(quiz.getId());
        Optional<QuizAttempt> bestAttempt = quizAttemptRepository.findTopByQuizIdOrderByScoreDesc(quiz.getId());
        Double bestPercentage = bestAttempt.map(QuizAttempt::getPercentage).orElse(null);

        return new QuizSummaryDto(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getSubject().getId(),
                quiz.getSubject().getName(),
                quiz.getTopic() != null ? quiz.getTopic().getId() : null,
                quiz.getTopic() != null ? quiz.getTopic().getName() : null,
                quiz.getDurationMinutes(),
                quiz.getTotalMarks(),
                quiz.getPassingMarks(),
                (int) questionCount,
                attemptCount,
                bestPercentage,
                quiz.getCreatedAt()
        );
    }
}

