export type RecallRating = 'AGAIN' | 'HARD' | 'GOOD' | 'EASY';
export type RevisionStatus = 'PENDING' | 'COMPLETED' | 'OVERDUE';

export interface RevisionSession {
  id: number;
  topicId: number;
  topicName: string;
  subjectId: number;
  subjectName: string;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  scheduledDate: string;
  repetitionNumber: number;
  intervalDays: number;
  easinessFactor: number;
  status: RevisionStatus;
  overdue: boolean;
  daysOverdue: number;
  lastReviewedAt?: string;
  createdAt: string;
}

export interface RevisionHistory {
  id: number;
  revisionSessionId?: number;
  topicId: number;
  topicName: string;
  subjectName: string;
  rating: RecallRating;
  reviewedAt: string;
  previousIntervalDays: number;
  newIntervalDays: number;
  previousEf: number;
  newEf: number;
  notes?: string;
}

export interface Flashcard {
  id: number;
  topicId: number;
  topicName: string;
  subjectName: string;
  frontPrompt: string;
  backAnswer: string;
  hint?: string;
  repetitionNumber: number;
  intervalDays: number;
  easinessFactor: number;
  nextReviewDate: string;
  due: boolean;
}

export interface RevisionReviewRequest {
  rating: RecallRating;
  notes?: string;
}

export interface FlashcardCreateRequest {
  topicId: number;
  frontPrompt: string;
  backAnswer: string;
  hint?: string;
}

export interface FlashcardReviewRequest {
  rating: RecallRating;
  timeSpentSeconds?: number;
}

