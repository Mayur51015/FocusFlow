import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { QuizService } from '../../services/quiz.service';
import { SubjectService } from '../../services/subject.service';
import { TopicService } from '../../services/topic.service';
import { Subject } from '../../models/subject.model';
import { Topic } from '../../models/topic.model';
import {
  QuizSummary,
  QuizDetail,
  QuizAttemptResult,
  QuizAttemptHistory,
  QuizCreateRequest,
  QuestionCreate,
  OptionCreate
} from '../../models/quiz.model';

export type QuizViewMode = 'LIST' | 'CREATE' | 'ATTEMPT' | 'RESULT' | 'HISTORY';

@Component({
  selector: 'app-quizzes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './quizzes.component.html',
  styleUrls: ['./quizzes.component.css']
})
export class QuizzesComponent implements OnInit, OnDestroy {

  viewMode: QuizViewMode = 'LIST';

  // Data collections
  quizzes: QuizSummary[] = [];
  subjects: Subject[] = [];
  topics: Topic[] = [];
  topicsForNewQuiz: Topic[] = [];
  attemptHistory: QuizAttemptHistory[] = [];

  // Filter state
  selectedSubjectFilter: number | null = null;
  selectedTopicFilter: number | null = null;

  // Active Quiz Attempt state
  activeQuiz: QuizDetail | null = null;
  currentQuestionIndex: number = 0;
  studentAnswers: { [questionId: number]: number } = {};
  timerRemainingSeconds: number = 0;
  timerTotalSeconds: number = 0;
  private timerInterval: any = null;
  isSubmitting: boolean = false;
  showConfirmSubmitModal: boolean = false;

  // Active Result state
  lastResult: QuizAttemptResult | null = null;

  // Create Quiz Form Model
  newQuiz: QuizCreateRequest = this.initNewQuizModel();

  // Loading & Alert feedback
  isLoading: boolean = false;
  errorMessage: string = '';
  successMessage: string = '';

  constructor(
    private quizService: QuizService,
    private subjectService: SubjectService,
    private topicService: TopicService
  ) {}

  ngOnInit(): void {
    this.loadQuizzes();
    this.loadSubjects();
    this.loadTopics();
  }

  ngOnDestroy(): void {
    this.clearTimer();
  }

  // --- Data Loading ---
  loadQuizzes(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.quizService.getQuizzes(
      this.selectedSubjectFilter || undefined,
      this.selectedTopicFilter || undefined
    ).subscribe({
      next: (data) => {
        this.quizzes = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load quizzes', err);
        this.errorMessage = 'Could not load quizzes from backend.';
        this.isLoading = false;
      }
    });
  }

  loadSubjects(): void {
    this.subjectService.getAllSubjects().subscribe({
      next: (subs) => {
        this.subjects = subs;
      },
      error: (err) => console.error('Failed to load subjects', err)
    });
  }

  loadTopics(): void {
    this.topicService.getAllTopics().subscribe({
      next: (tops) => {
        this.topics = tops;
      },
      error: (err) => console.error('Failed to load topics', err)
    });
  }

  onFilterSubjectChange(): void {
    this.selectedTopicFilter = null;
    this.loadQuizzes();
  }

  // --- Active Quiz Attempt Flow ---
  startQuiz(quizId: number): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.quizService.getQuizForAttempt(quizId).subscribe({
      next: (quizDetail) => {
        this.activeQuiz = quizDetail;
        this.currentQuestionIndex = 0;
        this.studentAnswers = {};
        this.timerTotalSeconds = (quizDetail.durationMinutes || 15) * 60;
        this.timerRemainingSeconds = this.timerTotalSeconds;
        this.isSubmitting = false;
        this.showConfirmSubmitModal = false;
        this.isLoading = false;
        this.viewMode = 'ATTEMPT';
        this.startTimer();
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Failed to start quiz attempt.';
      }
    });
  }

  startTimer(): void {
    this.clearTimer();
    this.timerInterval = setInterval(() => {
      if (this.timerRemainingSeconds > 0) {
        this.timerRemainingSeconds--;
      } else {
        this.clearTimer();
        // Time expired: automatically submit quiz
        this.submitAttempt(true);
      }
    }, 1000);
  }

  clearTimer(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

  get formattedRemainingTime(): string {
    const mins = Math.floor(this.timerRemainingSeconds / 60);
    const secs = this.timerRemainingSeconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }

  get isTimeLow(): boolean {
    return this.timerRemainingSeconds <= 120; // 2 minutes or less
  }

  selectOption(questionId: number, optionId: number): void {
    this.studentAnswers[questionId] = optionId;
  }

  isQuestionAnswered(questionId: number): boolean {
    return this.studentAnswers[questionId] !== undefined;
  }

  get answeredCount(): number {
    return Object.keys(this.studentAnswers).length;
  }

  get totalQuestionsCount(): number {
    return this.activeQuiz?.questions?.length || 0;
  }

  jumpToQuestion(index: number): void {
    if (this.activeQuiz && index >= 0 && index < this.activeQuiz.questions.length) {
      this.currentQuestionIndex = index;
    }
  }

  nextQuestion(): void {
    if (this.activeQuiz && this.currentQuestionIndex < this.activeQuiz.questions.length - 1) {
      this.currentQuestionIndex++;
    }
  }

  prevQuestion(): void {
    if (this.currentQuestionIndex > 0) {
      this.currentQuestionIndex--;
    }
  }

  confirmSubmit(): void {
    if (this.answeredCount < this.totalQuestionsCount) {
      this.showConfirmSubmitModal = true;
    } else {
      this.submitAttempt(false);
    }
  }

  submitAttempt(force: boolean = false): void {
    if (!this.activeQuiz) return;
    this.clearTimer();
    this.isSubmitting = true;
    this.showConfirmSubmitModal = false;

    const timeSpent = this.timerTotalSeconds - this.timerRemainingSeconds;
    const answerSubmissions = Object.keys(this.studentAnswers).map(qId => ({
      questionId: Number(qId),
      selectedOptionId: this.studentAnswers[Number(qId)]
    }));

    this.quizService.submitQuiz(this.activeQuiz.id, {
      timeSpentSeconds: Math.max(1, timeSpent),
      answers: answerSubmissions
    }).subscribe({
      next: (result) => {
        this.lastResult = result;
        this.isSubmitting = false;
        this.viewMode = 'RESULT';
        this.successMessage = `Quiz finished! You scored ${result.score} / ${result.totalMarks} (${result.percentage}%).`;
      },
      error: (err) => {
        this.isSubmitting = false;
        this.errorMessage = err.error?.message || 'Failed to submit quiz attempt.';
      }
    });
  }

  retakeCurrentQuiz(): void {
    if (this.lastResult) {
      this.startQuiz(this.lastResult.quizId);
    }
  }

  // --- Attempt History View ---
  openHistory(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.viewMode = 'HISTORY';
    this.quizService.getAttemptHistory().subscribe({
      next: (history) => {
        this.attemptHistory = history;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = 'Could not load quiz attempt history.';
      }
    });
  }

  // --- Create Quiz Flow ---
  openCreateForm(): void {
    this.newQuiz = this.initNewQuizModel();
    if (this.subjects.length > 0) {
      this.newQuiz.subjectId = this.subjects[0].id!;
      this.onNewQuizSubjectChange();
    }
    this.viewMode = 'CREATE';
  }

  onNewQuizSubjectChange(): void {
    if (!this.newQuiz.subjectId) {
      this.topicsForNewQuiz = [];
      this.newQuiz.topicId = undefined;
      return;
    }
    this.topicService.getAllTopics(this.newQuiz.subjectId).subscribe({
      next: (topics) => {
        this.topicsForNewQuiz = topics;
        this.newQuiz.topicId = topics.length > 0 ? topics[0].id : undefined;
      }
    });
  }

  addQuestion(): void {
    this.newQuiz.questions.push({
      questionText: '',
      topicId: this.newQuiz.topicId,
      marks: 10,
      explanation: '',
      options: [
        { optionText: '', isCorrect: true, optionOrder: 1 },
        { optionText: '', isCorrect: false, optionOrder: 2 },
        { optionText: '', isCorrect: false, optionOrder: 3 },
        { optionText: '', isCorrect: false, optionOrder: 4 }
      ]
    });
  }

  removeQuestion(index: number): void {
    if (this.newQuiz.questions.length > 1) {
      this.newQuiz.questions.splice(index, 1);
    }
  }

  addOption(qIndex: number): void {
    const q = this.newQuiz.questions[qIndex];
    if (q.options.length < 6) {
      q.options.push({
        optionText: '',
        isCorrect: false,
        optionOrder: q.options.length + 1
      });
    }
  }

  removeOption(qIndex: number, optIndex: number): void {
    const q = this.newQuiz.questions[qIndex];
    if (q.options.length > 2) {
      q.options.splice(optIndex, 1);
      // Re-assign option orders
      q.options.forEach((o, idx) => o.optionOrder = idx + 1);
      // If removed option was correct, set first as correct
      if (!q.options.some(o => o.isCorrect)) {
        q.options[0].isCorrect = true;
      }
    }
  }

  setCorrectOption(qIndex: number, optIndex: number): void {
    const q = this.newQuiz.questions[qIndex];
    q.options.forEach((opt, idx) => {
      opt.isCorrect = (idx === optIndex);
    });
  }

  submitCreateQuiz(): void {
    this.errorMessage = '';
    // Validation
    if (!this.newQuiz.title.trim()) {
      this.errorMessage = 'Please provide a quiz title.';
      return;
    }
    if (!this.newQuiz.subjectId) {
      this.errorMessage = 'Please choose a subject for this quiz.';
      return;
    }

    for (let i = 0; i < this.newQuiz.questions.length; i++) {
      const q = this.newQuiz.questions[i];
      if (!q.questionText.trim()) {
        this.errorMessage = `Question #${i + 1} text cannot be empty.`;
        return;
      }
      if (q.options.some(o => !o.optionText.trim())) {
        this.errorMessage = `All options in Question #${i + 1} must have non-empty text.`;
        return;
      }
      if (!q.options.some(o => o.isCorrect)) {
        this.errorMessage = `Question #${i + 1} must have one option marked as correct.`;
        return;
      }
    }

    this.isLoading = true;
    this.quizService.createQuiz(this.newQuiz).subscribe({
      next: (created) => {
        this.isLoading = false;
        this.successMessage = `Quiz "${created.title}" successfully created with ${created.questionCount} questions!`;
        this.viewMode = 'LIST';
        this.loadQuizzes();
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Failed to create quiz.';
      }
    });
  }

  backToList(): void {
    this.clearTimer();
    this.activeQuiz = null;
    this.lastResult = null;
    this.showConfirmSubmitModal = false;
    this.viewMode = 'LIST';
    this.loadQuizzes();
  }

  private initNewQuizModel(): QuizCreateRequest {
    return {
      title: '',
      description: '',
      subjectId: 0,
      topicId: undefined,
      durationMinutes: 15,
      passingMarks: 20,
      questions: [
        {
          questionText: '',
          marks: 10,
          explanation: '',
          options: [
            { optionText: '', isCorrect: true, optionOrder: 1 },
            { optionText: '', isCorrect: false, optionOrder: 2 },
            { optionText: '', isCorrect: false, optionOrder: 3 },
            { optionText: '', isCorrect: false, optionOrder: 4 }
          ]
        }
      ]
    };
  }
}

