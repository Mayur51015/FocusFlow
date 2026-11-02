export type ReadinessLevel = 'LOW' | 'MODERATE' | 'HIGH';

export interface ExamReadiness {
  id?: number;
  subjectId: number;
  subjectName: string;
  importance: 'HIGH' | 'MEDIUM' | 'LOW';
  examDate: string;
  daysUntilExam: number;
  readinessScore: number;
  readinessLevel: ReadinessLevel;
  topicCompletionRate: number;
  averageQuizScore: number;
  weakTopicCount: number;
  weakTopicRatio: number;
  revisionCompletionRate: number;
  predictionSource: string;
  recommendations: string[];
  evaluatedAt: string;
}

export interface ExamReadinessOverview {
  overallReadinessScore: number;
  overallReadinessLevel: ReadinessLevel;
  totalSubjects: number;
  highReadinessCount: number;
  moderateReadinessCount: number;
  lowReadinessCount: number;
  urgentExamsCount: number;
  subjectEvaluations: ExamReadiness[];
}

