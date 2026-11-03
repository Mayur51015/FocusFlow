export interface OptionStudent {
  id: number;
  optionText: string;
  optionOrder: number;
}

export interface QuestionStudent {
  id: number;
  topicId?: number;
  topicName?: string;
  questionText: string;
  marks: number;
  options: OptionStudent[];
}

export interface QuizSummary {
  id: number;
  title: string;
  description?: string;
  subjectId: number;
  subjectName: string;
  topicId?: number;
  topicName?: string;
  durationMinutes: number;
  totalMarks: number;
  passingMarks: number;
  questionCount: number;
  attemptCount: number;
  bestScorePercentage?: number;
  createdAt: string;
}

export interface QuizDetail {
  id: number;
  title: string;
  description?: string;
  subjectId: number;
  subjectName: string;
  topicId?: number;
  topicName?: string;
  durationMinutes: number;
  totalMarks: number;
  passingMarks: number;
  createdAt: string;
  questions: QuestionStudent[];
}

export interface AnswerSubmission {
  questionId: number;
  selectedOptionId?: number;
}

export interface QuizAttemptSubmitRequest {
  timeSpentSeconds: number;
  answers: AnswerSubmission[];
}

export interface AnswerResult {
  questionId: number;
  questionText: string;
  topicId?: number;
  topicName?: string;
  marks: number;
  selectedOptionId?: number;
  selectedOptionText: string;
  correctOptionId?: number;
  correctOptionText: string;
  isCorrect: boolean;
  marksAwarded: number;
  explanation?: string;
}

export interface QuizAttemptResult {
  attemptId: number;
  quizId: number;
  quizTitle: string;
  subjectName: string;
  topicName?: string;
  score: number;
  totalMarks: number;
  percentage: number;
  passed: boolean;
  passingMarks: number;
  startedAt: string;
  completedAt: string;
  timeSpentSeconds: number;
  questionResults: AnswerResult[];
}

export interface QuizAttemptHistory {
  attemptId: number;
  quizId: number;
  quizTitle: string;
  subjectName: string;
  topicName?: string;
  score: number;
  totalMarks: number;
  percentage: number;
  passed: boolean;
  completedAt: string;
  timeSpentSeconds: number;
}

export interface TopicQuizPerformance {
  topicId: number;
  topicName: string;
  subjectName: string;
  totalQuestionsAttempted: number;
  correctAnswersCount: number;
  accuracyPercentage: number;
  attemptCount: number;
  lastAttemptDate?: string;
}

export interface OptionCreate {
  optionText: string;
  isCorrect: boolean;
  optionOrder: number;
}

export interface QuestionCreate {
  questionText: string;
  topicId?: number;
  marks: number;
  explanation?: string;
  options: OptionCreate[];
}

export interface QuizCreateRequest {
  title: string;
  description?: string;
  subjectId: number;
  topicId?: number;
  durationMinutes: number;
  passingMarks: number;
  questions: QuestionCreate[];
}

