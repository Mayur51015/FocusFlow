export type PerformanceLevel = 'STRONG' | 'MODERATE' | 'WEAK';

export interface TopicPerformance {
  id: number;
  topicId: number;
  topicName: string;
  subjectId: number;
  subjectName: string;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  performanceLevel: PerformanceLevel;
  averageQuizScore: number;
  attemptCount: number;
  accuracyPercentage: number;
  totalStudyMinutes: number;
  predictionSource: string;
  confidenceScore: number;
  recommendation: string;
  lastEvaluatedAt: string;
}

