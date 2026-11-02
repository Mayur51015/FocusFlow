import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { StudySessionService } from '../../services/study-session.service';
import {
  StudySession,
  SessionStatus,
  StudyHistoryDto,
  AdaptiveScheduleSummaryDto,
  TopicPredictionResponse,
  MlHealthStatus
} from '../../models/study-session.model';

@Component({
  selector: 'app-schedule',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './schedule.component.html',
  styleUrls: ['./schedule.component.css']
})
export class ScheduleComponent implements OnInit {
  selectedDate: string = '';
  availableHours: number = 3.0;

  sessions: StudySession[] = [];
  isLoading: boolean = false;
  isGenerating: boolean = false;
  isRecalculating: boolean = false;
  errorMessage: string = '';
  successMessage: string = '';

  // ML Health & Preview
  mlHealth: MlHealthStatus | null = null;
  previewTopicId: number | null = null;
  previewPrediction: TopicPredictionResponse | null = null;
  isLoadingPreview: boolean = false;

  // Adaptive Schedule State
  adaptiveSummary: AdaptiveScheduleSummaryDto | null = null;

  // Study History Modal/Panel
  showHistory: boolean = false;
  historyList: StudyHistoryDto[] = [];
  isLoadingHistory: boolean = false;

  constructor(private studySessionService: StudySessionService) {}

  ngOnInit(): void {
    const today = new Date();
    this.selectedDate = today.toISOString().split('T')[0];
    this.loadSessions();
    this.checkMlHealth();
  }

  checkMlHealth(): void {
    this.studySessionService.getMlHealth().subscribe({
      next: (health) => {
        this.mlHealth = health;
      },
      error: () => {
        this.mlHealth = { mlAvailable: false, service: 'FocusFlow ML Predictor', status: 'OFFLINE' };
      }
    });
  }

  loadSessions(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.adaptiveSummary = null;

    this.studySessionService.getSessionsByDate(this.selectedDate).subscribe({
      next: (data) => {
        this.sessions = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load sessions', err);
        this.errorMessage = 'Could not load sessions from backend.';
        this.isLoading = false;
      }
    });
  }

  generateSchedule(): void {
    if (this.availableHours <= 0) {
      this.errorMessage = 'Please enter a valid study duration greater than 0 hours.';
      return;
    }
    if (!this.selectedDate) {
      this.errorMessage = 'Please select a date for scheduling.';
      return;
    }

    this.isGenerating = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.adaptiveSummary = null;

    this.studySessionService.generateSchedule({
      date: this.selectedDate,
      availableHours: this.availableHours
    }).subscribe({
      next: (generated) => {
        this.isGenerating = false;
        this.sessions = generated;
        if (generated.length === 0) {
          this.errorMessage = 'No topics could be scheduled for this date. Check that topics are incomplete and exam dates have not passed.';
        } else {
          const totalPlannedMinutes = generated.reduce((acc, s) => acc + s.plannedMinutes, 0);
          this.successMessage = `Schedule generated! ${generated.length} sessions totaling ${totalPlannedMinutes} mins. Existing completed/locked sessions preserved.`;
        }
      },
      error: (err) => {
        this.isGenerating = false;
        this.errorMessage = err.error?.message || 'Failed to generate schedule.';
      }
    });
  }

  recalculateSchedule(): void {
    if (!this.selectedDate) {
      this.errorMessage = 'Please select a date for adaptive recalculation.';
      return;
    }

    this.isRecalculating = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.studySessionService.recalculateSchedule({
      date: this.selectedDate,
      availableHours: this.availableHours > 0 ? this.availableHours : undefined
    }).subscribe({
      next: (summary) => {
        this.isRecalculating = false;
        this.adaptiveSummary = summary;
        this.sessions = summary.sessions;
        this.successMessage = summary.message;
      },
      error: (err) => {
        this.isRecalculating = false;
        this.errorMessage = err.error?.message || 'Failed to recalculate adaptive schedule.';
      }
    });
  }

  startStudy(session: StudySession): void {
    if (!session.id) return;
    this.studySessionService.updateSessionStatus(session.id, {
      status: 'IN_PROGRESS',
      actualMinutes: session.actualMinutes
    }).subscribe({
      next: (updated) => {
        this.applySessionUpdate(session, updated);
        this.successMessage = `Started study block for "${session.topic.name}"! Stay focused.`;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to start study session.';
      }
    });
  }

  completeSession(session: StudySession): void {
    if (!session.id) return;
    const finalMinutes = session.actualMinutes > 0 ? session.actualMinutes : session.plannedMinutes;

    this.studySessionService.updateSessionStatus(session.id, {
      status: 'COMPLETED',
      actualMinutes: finalMinutes
    }).subscribe({
      next: (updated) => {
        this.applySessionUpdate(session, updated);
        this.successMessage = `🎉 Awesome! Completed "${session.topic.name}" (${updated.actualMinutes} mins logged).`;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to complete session.';
      }
    });
  }

  markPartiallyCompleted(session: StudySession): void {
    if (!session.id) return;
    const input = prompt(`Enter minutes actually studied for "${session.topic.name}":`, String(session.actualMinutes || Math.round(session.plannedMinutes / 2)));
    if (input === null) return;

    const minutes = parseInt(input, 10);
    if (isNaN(minutes) || minutes < 0) {
      this.errorMessage = 'Invalid duration entered. Must be a non-negative number.';
      return;
    }

    this.studySessionService.updateSessionStatus(session.id, {
      status: 'PARTIALLY_COMPLETED',
      actualMinutes: minutes
    }).subscribe({
      next: (updated) => {
        this.applySessionUpdate(session, updated);
        this.successMessage = `Marked "${session.topic.name}" as partially completed (${updated.actualMinutes} mins).`;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to update session.';
      }
    });
  }

  markMissed(session: StudySession): void {
    if (!session.id) return;
    this.studySessionService.updateSessionStatus(session.id, {
      status: 'MISSED',
      actualMinutes: 0
    }).subscribe({
      next: (updated) => {
        this.applySessionUpdate(session, updated);
        this.successMessage = `Session for "${session.topic.name}" marked as missed. Ready for rescheduling.`;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to update session status.';
      }
    });
  }

  resetToPlanned(session: StudySession): void {
    if (!session.id) return;
    this.studySessionService.updateSessionStatus(session.id, {
      status: 'PLANNED',
      actualMinutes: 0
    }).subscribe({
      next: (updated) => {
        this.applySessionUpdate(session, updated);
        this.successMessage = `Reset "${session.topic.name}" back to planned status.`;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to reset session.';
      }
    });
  }

  saveActualMinutes(session: StudySession, newMinutes: number): void {
    if (!session.id) return;
    if (newMinutes < 0) {
      this.errorMessage = 'Actual study duration cannot be negative.';
      return;
    }

    this.studySessionService.updateActualDuration(session.id, Number(newMinutes)).subscribe({
      next: (updated) => {
        session.actualMinutes = updated.actualMinutes;
        this.successMessage = `Recorded actual study duration: ${updated.actualMinutes} mins for "${session.topic.name}".`;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to update actual minutes.';
      }
    });
  }

  toggleLock(session: StudySession): void {
    if (!session.id) return;
    this.studySessionService.toggleLock(session.id).subscribe({
      next: (updated) => {
        session.locked = updated.locked;
        this.successMessage = updated.locked
          ? `🔒 Session locked. It will not be moved or replaced.`
          : `🔓 Session unlocked.`;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to toggle session lock.';
      }
    });
  }

  deleteSession(sessionId?: number): void {
    if (!sessionId) return;
    if (confirm('Delete this study session?')) {
      this.studySessionService.deleteSession(sessionId).subscribe({
        next: () => {
          this.sessions = this.sessions.filter(s => s.id !== sessionId);
          this.successMessage = 'Study session removed.';
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Failed to delete session.';
        }
      });
    }
  }

  toggleHistory(): void {
    this.showHistory = !this.showHistory;
    if (this.showHistory) {
      this.loadHistory();
    }
  }

  loadHistory(): void {
    this.isLoadingHistory = true;
    this.studySessionService.getStudyHistory(false).subscribe({
      next: (history) => {
        this.historyList = history;
        this.isLoadingHistory = false;
      },
      error: (err) => {
        console.error('Failed to load study history', err);
        this.errorMessage = 'Could not load study history from backend.';
        this.isLoadingHistory = false;
      }
    });
  }

  private applySessionUpdate(session: StudySession, updated: StudySession): void {
    session.status = updated.status;
    session.actualMinutes = updated.actualMinutes;
    session.startTime = updated.startTime;
    session.endTime = updated.endTime;
    session.completedAt = updated.completedAt;
    session.locked = updated.locked;
    session.isRescheduled = updated.isRescheduled ?? updated.rescheduled;
    session.rescheduled = updated.rescheduled ?? updated.isRescheduled;
  }

  getTotalPlannedMinutes(): number {
    return this.sessions.reduce((acc, s) => acc + s.plannedMinutes, 0);
  }

  getTotalActualMinutes(): number {
    return this.sessions.reduce((acc, s) => acc + s.actualMinutes, 0);
  }

  getCompletedSessionsCount(): number {
    return this.sessions.filter(s => s.status === 'COMPLETED').length;
  }

  getMissedSessionsCount(): number {
    return this.sessions.filter(s => s.status === 'MISSED').length;
  }

  getPartiallyCompletedCount(): number {
    return this.sessions.filter(s => s.status === 'PARTIALLY_COMPLETED').length;
  }

  getRescheduledSessionsCount(): number {
    return this.sessions.filter(s => s.isRescheduled || s.rescheduled).length;
  }

  getLockedSessionsCount(): number {
    return this.sessions.filter(s => s.locked).length;
  }

  getCommittedMinutes(): number {
    return this.sessions
      .filter(s => s.status === 'COMPLETED' || s.status === 'PARTIALLY_COMPLETED' || s.status === 'IN_PROGRESS' || s.locked)
      .reduce((acc, s) => acc + (s.actualMinutes > 0 ? s.actualMinutes : s.plannedMinutes), 0);
  }

  getRemainingDailyCapacityMinutes(): number {
    if (this.adaptiveSummary) {
      return this.adaptiveSummary.remainingCapacityMinutes;
    }
    const total = Math.round(this.availableHours * 60);
    return Math.max(0, total - this.getCommittedMinutes());
  }

  isMlPrediction(session: StudySession): boolean {
    return session.predictionSource === 'EXPERIMENTAL_ML';
  }

  isFallbackPrediction(session: StudySession): boolean {
    return session.predictionSource === 'HEURISTIC_FALLBACK';
  }

  previewPredictionForTopic(topicId?: number): void {
    if (!topicId) return;
    this.previewTopicId = topicId;
    this.isLoadingPreview = true;
    this.previewPrediction = null;

    this.studySessionService.predictDuration(topicId).subscribe({
      next: (res) => {
        this.previewPrediction = res;
        this.isLoadingPreview = false;
      },
      error: (err) => {
        this.isLoadingPreview = false;
        this.errorMessage = err.error?.message || 'Failed to generate ML prediction preview.';
      }
    });
  }

  closePreview(): void {
    this.previewTopicId = null;
    this.previewPrediction = null;
  }
}
