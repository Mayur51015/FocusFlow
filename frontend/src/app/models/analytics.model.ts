import { RevisionHistory } from './revision.model';

export interface DailyStudyFocus {
  topicId: number;
  topicName: string;
  subjectId: number;
  subjectName: string;
  difficulty: string;
  reason: string;
  priorityScore: number;
  actionType: 'QUIZ' | 'REVISION' | 'STUDY';
}

export interface LearningSummary {
  overallMasteryScore: number;
  overallMasteryLevel: 'STRONG' | 'MODERATE' | 'WEAK';
  totalTopics: number;
  strongTopicsCount: number;
  moderateTopicsCount: number;
  weakTopicsCount: number;
  retentionRatePercentage: number;
  totalRevisionReviews: number;
  totalFlashcards: number;
  totalQuizAttempts: number;
  overallQuizAverage: number;
  overallReadinessScore: number;
  overallReadinessLevel: string;
  recommendedStudyFocus: DailyStudyFocus[];
}

export interface SubjectMastery {
  subjectId: number;
  subjectName: string;
  importance: string;
  examDate: string;
  daysUntilExam: number;
  totalTopics: number;
  completedTopics: number;
  topicCompletionRate: number;
  strongTopicsCount: number;
  moderateTopicsCount: number;
  weakTopicsCount: number;
  averageQuizScore: number;
  quizAttemptsCount: number;
  readinessScore: number;
  readinessLevel: string;
  overallMasteryLevel: 'STRONG' | 'MODERATE' | 'WEAK';
}

export interface QuizScoreTrend {
  attemptId: number;
  quizTitle: string;
  subjectName: string;
  scorePercentage: number;
  passed: boolean;
  attemptedAt: string;
}

export interface RetentionTrends {
  totalReviews: number;
  againCount: number;
  hardCount: number;
  goodCount: number;
  easyCount: number;
  retentionRatePercentage: number;
  averageEasinessFactor: number;
  recentReviews: RevisionHistory[];
  historicalQuizScores: QuizScoreTrend[];
}

