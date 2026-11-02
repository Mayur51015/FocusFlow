package com.focusflow.service;

import com.focusflow.dto.*;
import com.focusflow.entity.*;
import com.focusflow.exception.BadRequestException;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuizServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuizOptionRepository quizOptionRepository;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private QuizAnswerRepository quizAnswerRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private TopicRepository topicRepository;

    @InjectMocks
    private QuizService quizService;

    private Subject dsaSubject;
    private Topic bstTopic;
    private Quiz sampleQuiz;
    private Question q1;
    private QuizOption opt1;
    private QuizOption opt2;

    @BeforeEach
    void setUp() {
        dsaSubject = new Subject("Data Structures & Algorithms", LocalDate.now().plusDays(10), Importance.HIGH);
        dsaSubject.setId(1L);

        bstTopic = new Topic("Binary Search Trees", dsaSubject, Difficulty.HARD, 90, false);
        bstTopic.setId(10L);

        sampleQuiz = new Quiz("BST Mastery", "Test BST knowledge", dsaSubject, bstTopic, 15, 20, 10);
        sampleQuiz.setId(100L);

        q1 = new Question(sampleQuiz, bstTopic, "What is worst case search in unbalanced BST?", 10, "Degenerates to list");
        q1.setId(50L);

        opt1 = new QuizOption(q1, "O(log n)", false, 1);
        opt1.setId(501L);

        opt2 = new QuizOption(q1, "O(n)", true, 2);
        opt2.setId(502L);

        q1.setOptions(List.of(opt1, opt2));
    }

    @Test
    void testCreateQuiz_Success() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(dsaSubject));
        when(topicRepository.findById(10L)).thenReturn(Optional.of(bstTopic));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> {
            Quiz q = invocation.getArgument(0);
            q.setId(100L);
            return q;
        });
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
            Question q = invocation.getArgument(0);
            q.setId(50L);
            return q;
        });
        when(quizOptionRepository.save(any(QuizOption.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuizCreateRequest req = new QuizCreateRequest();
        req.setTitle("BST Mastery");
        req.setSubjectId(1L);
        req.setTopicId(10L);
        req.setDurationMinutes(15);
        req.setPassingMarks(10);

        QuizCreateRequest.QuestionCreateDto qDto = new QuizCreateRequest.QuestionCreateDto();
        qDto.setQuestionText("What is worst case search in unbalanced BST?");
        qDto.setMarks(10);
        qDto.setExplanation("Degenerates to list");

        QuizCreateRequest.OptionCreateDto o1 = new QuizCreateRequest.OptionCreateDto();
        o1.setOptionText("O(log n)");
        o1.setCorrect(false);

        QuizCreateRequest.OptionCreateDto o2 = new QuizCreateRequest.OptionCreateDto();
        o2.setOptionText("O(n)");
        o2.setCorrect(true);

        qDto.setOptions(List.of(o1, o2));
        req.setQuestions(List.of(qDto));

        QuizSummaryDto summary = quizService.createQuiz(req);

        assertNotNull(summary);
        assertEquals("BST Mastery", summary.getTitle());
        assertEquals(10L, summary.getTotalMarks());
        verify(quizRepository, times(1)).save(any(Quiz.class));
        verify(questionRepository, times(1)).save(any(Question.class));
        verify(quizOptionRepository, times(2)).save(any(QuizOption.class));
    }

    @Test
    void testCreateQuiz_ThrowsWhenNoCorrectOption() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(dsaSubject));

        QuizCreateRequest req = new QuizCreateRequest();
        req.setTitle("Invalid Quiz");
        req.setSubjectId(1L);

        QuizCreateRequest.QuestionCreateDto qDto = new QuizCreateRequest.QuestionCreateDto();
        qDto.setQuestionText("Sample question?");
        qDto.setMarks(5);

        QuizCreateRequest.OptionCreateDto o1 = new QuizCreateRequest.OptionCreateDto();
        o1.setOptionText("Option 1");
        o1.setCorrect(false);

        QuizCreateRequest.OptionCreateDto o2 = new QuizCreateRequest.OptionCreateDto();
        o2.setOptionText("Option 2");
        o2.setCorrect(false); // No correct option

        qDto.setOptions(List.of(o1, o2));
        req.setQuestions(List.of(qDto));

        assertThrows(BadRequestException.class, () -> quizService.createQuiz(req));
    }

    @Test
    void testGetQuizForAttempt_MasksCorrectAnswers() {
        when(quizRepository.findById(100L)).thenReturn(Optional.of(sampleQuiz));
        when(questionRepository.findByQuizIdOrderByIdAsc(100L)).thenReturn(List.of(q1));
        when(quizOptionRepository.findByQuestionIdOrderByOptionOrderAsc(50L)).thenReturn(List.of(opt1, opt2));

        QuizDetailDto detail = quizService.getQuizForAttempt(100L);

        assertNotNull(detail);
        assertEquals(100L, detail.getId());
        assertEquals(1, detail.getQuestions().size());

        QuestionStudentDto studentQuestion = detail.getQuestions().get(0);
        assertEquals("What is worst case search in unbalanced BST?", studentQuestion.getQuestionText());
        assertEquals(2, studentQuestion.getOptions().size());

        // Verify that QuestionStudentDto does not expose isCorrect
        assertEquals("O(log n)", studentQuestion.getOptions().get(0).getOptionText());
        assertEquals("O(n)", studentQuestion.getOptions().get(1).getOptionText());
    }

    @Test
    void testSubmitQuizAttempt_ScoresCorrectly() {
        when(quizRepository.findById(100L)).thenReturn(Optional.of(sampleQuiz));
        when(questionRepository.findByQuizIdOrderByIdAsc(100L)).thenReturn(List.of(q1));
        when(quizOptionRepository.findByQuestionIdOrderByOptionOrderAsc(50L)).thenReturn(List.of(opt1, opt2));
        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenAnswer(invocation -> {
            QuizAttempt a = invocation.getArgument(0);
            a.setId(999L);
            return a;
        });

        // Student submits correct answer (opt2 = 502L)
        QuizAttemptSubmitRequest req = new QuizAttemptSubmitRequest();
        req.setTimeSpentSeconds(45);
        req.setAnswers(List.of(new QuizAttemptSubmitRequest.AnswerSubmissionDto(50L, 502L)));

        QuizAttemptResultDto result = quizService.submitQuizAttempt(100L, req);

        assertNotNull(result);
        assertEquals(999L, result.getAttemptId());
        assertEquals(10, result.getScore());
        assertEquals(100.0, result.getPercentage());
        assertTrue(result.isPassed());
        assertEquals(1, result.getQuestionResults().size());
        assertTrue(result.getQuestionResults().get(0).isCorrect());
        assertEquals(10, result.getQuestionResults().get(0).getMarksAwarded());

        verify(quizAttemptRepository, times(1)).save(any(QuizAttempt.class));
        verify(quizAnswerRepository, times(1)).save(any(QuizAnswer.class));
    }

    @Test
    void testSubmitQuizAttempt_ScoresZeroWhenIncorrect() {
        when(quizRepository.findById(100L)).thenReturn(Optional.of(sampleQuiz));
        when(questionRepository.findByQuizIdOrderByIdAsc(100L)).thenReturn(List.of(q1));
        when(quizOptionRepository.findByQuestionIdOrderByOptionOrderAsc(50L)).thenReturn(List.of(opt1, opt2));
        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenAnswer(invocation -> {
            QuizAttempt a = invocation.getArgument(0);
            a.setId(999L);
            return a;
        });

        // Student submits incorrect answer (opt1 = 501L)
        QuizAttemptSubmitRequest req = new QuizAttemptSubmitRequest();
        req.setTimeSpentSeconds(60);
        req.setAnswers(List.of(new QuizAttemptSubmitRequest.AnswerSubmissionDto(50L, 501L)));

        QuizAttemptResultDto result = quizService.submitQuizAttempt(100L, req);

        assertNotNull(result);
        assertEquals(0, result.getScore());
        assertEquals(0.0, result.getPercentage());
        assertFalse(result.isPassed());
        assertFalse(result.getQuestionResults().get(0).isCorrect());
        assertEquals(0, result.getQuestionResults().get(0).getMarksAwarded());
    }

    @Test
    void testGetTopicQuizPerformance() {
        when(topicRepository.findById(10L)).thenReturn(Optional.of(bstTopic));

        QuizAnswer ans1 = new QuizAnswer();
        ans1.setCorrect(true);
        QuizAttempt att1 = new QuizAttempt();
        att1.setId(1L);
        att1.setCompletedAt(LocalDateTime.now());
        ans1.setAttempt(att1);

        QuizAnswer ans2 = new QuizAnswer();
        ans2.setCorrect(false);
        ans2.setAttempt(att1);

        when(quizAnswerRepository.findByTopicId(10L)).thenReturn(List.of(ans1, ans2));

        TopicQuizPerformanceDto perf = quizService.getTopicQuizPerformance(10L);

        assertNotNull(perf);
        assertEquals(10L, perf.getTopicId());
        assertEquals("Binary Search Trees", perf.getTopicName());
        assertEquals(2, perf.getTotalQuestionsAttempted());
        assertEquals(1, perf.getCorrectAnswersCount());
        assertEquals(50.0, perf.getAccuracyPercentage());
    }
}

