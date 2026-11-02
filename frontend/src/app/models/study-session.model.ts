import { Topic } from './topic.model';

export type SessionStatus =
  | 'PLANNED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'MISSED'
  | 'PARTIALLY_COMPLETED'
  | 'RESCHEDULED'
  | 'INCOMPLETE';

export interface StudySession {
  id?: number;
  topic: Topic;
  scheduledDate: string; // YYYY-MM-DD
  plannedMinutes: number;
  actualMinutes: number;
  status: SessionStatus;
  startTime?: string;
  endTime?: string;
  completedAt?: string;
  isRescheduled?: boolean;
  rescheduled?: boolean;
  locked?: boolean;
  predictedMinutes?: number;
  predictionSource?: string;
}

export interface ScheduleRequest {
  date: string; // YYYY-MM-DD
  availableHours: number;
}

export interface RecalculateScheduleRequest {
  date?: string; // YYYY-MM-DD
  availableHours?: number;
}

export interface AdaptiveScheduleSummaryDto {
  sessions: StudySession[];
  totalCapacityMinutes: number;
  committedMinutes: number;
  remainingCapacityMinutes: number;
  completedSessionsCount: number;
  partiallyCompletedCount: number;
  missedSessionsCount: number;
  rescheduledSessionsCount: number;
  lockedSessionsCount: number;
  message: string;
}

export interface SessionStatusUpdateRequest {
  status: SessionStatus;
  actualMinutes: number;
  startTime?: string;
  endTime?: string;
  completedAt?: string;
}

export interface ActualDurationUpdateRequest {
  actualMinutes: number;
}

export interface StudyHistoryDto {
  id: number;
  topicId: number;
  topicName: string;
  subjectId: number;
  subjectName: string;
  difficulty: string;
  scheduledDate: string;
  plannedMinutes: number;
  actualMinutes: number;
  status: SessionStatus;
  startTime?: string;
  endTime?: string;
  completedAt?: string;
  isRescheduled: boolean;
  locked: boolean;
  predictedMinutes?: number;
  predictionSource?: string;
}

export interface TopicPredictionResponse {
  topicId: number;
  topicName: string;
  estimatedMinutes: number;
  predictedMinutes: number;
  confidenceScore?: number;
  estimatedErrorRangeMinutes?: number;
  modelVersion?: string;
  predictionSource: string;
  message?: string;
}

export interface MlHealthStatus {
  mlAvailable: boolean;
  service: string;
  status: string;
  modelVersion?: string;
}
