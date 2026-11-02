import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProgressService } from '../../services/progress.service';
import { StudySessionService } from '../../services/study-session.service';
import { DashboardStats } from '../../models/progress.model';
import { StudySession } from '../../models/study-session.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  stats: DashboardStats = {
    plannedMinutesToday: 0,
    totalSessionsToday: 0,
    completedSessionsToday: 0,
    pendingSessionsToday: 0,
    overallProgressPercentage: 0
  };

  todaySessions: StudySession[] = [];
  isLoading: boolean = true;
  isGenerating: boolean = false;
  isRecalculating: boolean = false;
  errorMessage: string = '';
  successMessage: string = '';

  todayDateStr: string = '';
  availableHours: number = 2.5;

  constructor(
    private progressService: ProgressService,
    private studySessionService: StudySessionService
  ) {}

  ngOnInit(): void {
    const today = new Date();
    this.todayDateStr = today.toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.isLoading = true;
    this.errorMessage = '';

    // 1. Load KPI statistics
    this.progressService.getDashboardStats().subscribe({
      next: (data) => {
        this.stats = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load dashboard stats', err);
        this.errorMessage = 'Could not load summary statistics from backend.';
        this.isLoading = false;
      }
    });

    // 2. Load today's scheduled sessions
    this.studySessionService.getSessionsByDate().subscribe({
      next: (sessions) => {
        this.todaySessions = sessions;
      },
      error: (err) => {
        console.error('Failed to load today sessions', err);
      }
    });
  }

  generateSchedule(): void {
    if (this.availableHours <= 0) {
      this.errorMessage = 'Please provide a valid study duration greater than 0 hours.';
      return;
    }

    const todayIso = new Date().toISOString().split('T')[0];

    this.isGenerating = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.studySessionService.generateSchedule({
      date: todayIso,
      availableHours: this.availableHours
    }).subscribe({
      next: (sessions) => {
        this.isGenerating = false;
        this.todaySessions = sessions;
        this.loadDashboardData(); // Refresh summary numbers
        this.successMessage = `Smart schedule generated for today! (${sessions.length} study sessions planned)`;
      },
      error: (err) => {
        this.isGenerating = false;
        this.errorMessage = err.error?.message || 'Failed to generate study schedule.';
      }
    });
  }

  recalculateSchedule(): void {
    const todayIso = new Date().toISOString().split('T')[0];
    this.isRecalculating = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.studySessionService.recalculateSchedule({
      date: todayIso,
      availableHours: this.availableHours > 0 ? this.availableHours : undefined
    }).subscribe({
      next: (summary) => {
        this.isRecalculating = false;
        this.todaySessions = summary.sessions;
        this.loadDashboardData();
        this.successMessage = summary.message;
      },
      error: (err) => {
        this.isRecalculating = false;
        this.errorMessage = err.error?.message || 'Failed to recalculate adaptive schedule.';
      }
    });
  }

  markSessionCompleted(session: StudySession): void {
    if (!session.id) return;
    const actual = session.actualMinutes > 0 ? session.actualMinutes : session.plannedMinutes;
    this.studySessionService.updateSessionStatus(session.id, {
      status: 'COMPLETED',
      actualMinutes: actual
    }).subscribe({
      next: () => {
        this.loadDashboardData();
      },
      error: (err) => {
        this.errorMessage = 'Failed to update session status.';
      }
    });
  }

  markSessionIncomplete(session: StudySession): void {
    if (!session.id) return;
    this.studySessionService.updateSessionStatus(session.id, {
      status: 'PLANNED',
      actualMinutes: 0
    }).subscribe({
      next: () => {
        this.loadDashboardData();
      },
      error: (err) => {
        this.errorMessage = 'Failed to update session status.';
      }
    });
  }

  getRemainingCapacityMinutes(): number {
    const totalMinutes = Math.round(this.availableHours * 60);
    const committed = this.todaySessions
      .filter(s => s.status === 'COMPLETED' || s.status === 'PARTIALLY_COMPLETED' || s.status === 'IN_PROGRESS' || s.locked)
      .reduce((acc, s) => acc + (s.actualMinutes > 0 ? s.actualMinutes : s.plannedMinutes), 0);
    return Math.max(0, totalMinutes - committed);
  }

  getRescheduledCount(): number {
    return this.todaySessions.filter(s => s.isRescheduled || s.rescheduled).length;
  }

  getLockedCount(): number {
    return this.todaySessions.filter(s => s.locked).length;
  }

  getMissedCount(): number {
    return this.todaySessions.filter(s => s.status === 'MISSED').length;
  }
}
