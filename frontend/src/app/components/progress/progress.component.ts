import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProgressService } from '../../services/progress.service';
import { StudySessionService } from '../../services/study-session.service';
import { AnalyticsService } from '../../services/analytics.service';
import { WeeklyProgress } from '../../models/progress.model';
import { StudyHistoryDto } from '../../models/study-session.model';
import {
  LearningSummary,
  SubjectMastery,
  RetentionTrends
} from '../../models/analytics.model';

export interface PredictionAccuracyItem {
  id: number;
  topicName: string;
  subjectName: string;
  difficulty: string;
  scheduledDate: string;
  plannedMinutes: number;
  predictedMinutes: number;
  actualMinutes: number;
  predictionSource: string;
  varianceMinutes: number;
  absErrorMinutes: number;
  accuracyLabel: string;
  accuracyClass: string;
}

export interface PredictionAccuracyMetrics {
  totalAnalyzed: number;
  meanAbsoluteError: number;
  accuracyRatePercentage: number;
  totalPredictedMinutes: number;
  totalActualMinutes: number;
}

@Component({
  selector: 'app-progress',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './progress.component.html',
  styleUrls: ['./progress.component.css']
})
export class ProgressComponent implements OnInit {
  private progressService = inject(ProgressService);
  private studySessionService = inject(StudySessionService);
  private analyticsService = inject(AnalyticsService);

  activeAnalyticsTab: 'MASTERY' | 'SUBJECTS' | 'RETENTION' | 'STUDY_TIME' = 'MASTERY';

  // Academic Learning Analytics (Phase 5)
  learningSummary: LearningSummary | null = null;
  subjectMasteryList: SubjectMastery[] = [];
  retentionTrends: RetentionTrends | null = null;
  isLoadingAnalytics: boolean = false;

  // Study Time & ML Duration Prediction Metrics (MVP 1)
  weeklyData: WeeklyProgress = {
    totalCompletedMinutes: 0,
    totalCompletedSessions: 0,
    progressPercentage: 0,
    subjectSummaries: []
  };

  accuracyItems: PredictionAccuracyItem[] = [];
  accuracyMetrics: PredictionAccuracyMetrics = {
    totalAnalyzed: 0,
    meanAbsoluteError: 0,
    accuracyRatePercentage: 0,
    totalPredictedMinutes: 0,
    totalActualMinutes: 0
  };

  isLoading: boolean = false;
  isLoadingHistory: boolean = false;
  errorMessage: string = '';

  ngOnInit(): void {
    this.loadLearningAnalytics();
    this.loadWeeklyProgress();
    this.loadPredictionAccuracy();
  }

  setAnalyticsTab(tab: 'MASTERY' | 'SUBJECTS' | 'RETENTION' | 'STUDY_TIME'): void {
    this.activeAnalyticsTab = tab;
    if (tab === 'MASTERY') this.loadLearningAnalytics();
    else if (tab === 'SUBJECTS') this.loadSubjectMastery();
    else if (tab === 'RETENTION') this.loadRetentionTrends();
  }

  loadLearningAnalytics(): void {
    this.isLoadingAnalytics = true;
    this.analyticsService.getLearningSummary().subscribe({
      next: (summary) => {
        this.learningSummary = summary;
        this.isLoadingAnalytics = false;
      },
      error: (err) => {
        console.warn('Could not load learning summary:', err);
        this.isLoadingAnalytics = false;
      }
    });
  }

  loadSubjectMastery(): void {
    this.isLoadingAnalytics = true;
    this.analyticsService.getSubjectMastery().subscribe({
      next: (list) => {
        this.subjectMasteryList = list;
        this.isLoadingAnalytics = false;
      },
      error: (err) => {
        console.warn('Could not load subject mastery breakdown:', err);
        this.isLoadingAnalytics = false;
      }
    });
  }

  loadRetentionTrends(): void {
    this.isLoadingAnalytics = true;
    this.analyticsService.getRetentionTrends().subscribe({
      next: (trends) => {
        this.retentionTrends = trends;
        this.isLoadingAnalytics = false;
      },
      error: (err) => {
        console.warn('Could not load retention trends:', err);
        this.isLoadingAnalytics = false;
      }
    });
  }

  loadWeeklyProgress(): void {
    this.isLoading = true;
    this.progressService.getWeeklyProgress().subscribe({
      next: (data) => {
        this.weeklyData = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load weekly progress', err);
        this.errorMessage = 'Could not load weekly analytics from backend.';
        this.isLoading = false;
      }
    });
  }

  loadPredictionAccuracy(): void {
    this.isLoadingHistory = true;
    this.studySessionService.getStudyHistory(true).subscribe({
      next: (history: StudyHistoryDto[]) => {
        this.processAccuracyData(history);
        this.isLoadingHistory = false;
      },
      error: (err) => {
        console.error('Failed to load study history for accuracy metrics', err);
        this.isLoadingHistory = false;
      }
    });
  }

  private processAccuracyData(history: StudyHistoryDto[]): void {
    const valid = history.filter(h => h.actualMinutes > 0);
    if (valid.length === 0) {
      this.accuracyItems = [];
      this.accuracyMetrics = {
        totalAnalyzed: 0,
        meanAbsoluteError: 0,
        accuracyRatePercentage: 0,
        totalPredictedMinutes: 0,
        totalActualMinutes: 0
      };
      return;
    }

    let totalAbsError = 0;
    let within15Count = 0;
    let totalPred = 0;
    let totalAct = 0;

    this.accuracyItems = valid.map(h => {
      const predicted = h.predictedMinutes && h.predictedMinutes > 0 ? h.predictedMinutes : h.plannedMinutes;
      const variance = h.actualMinutes - predicted;
      const absError = Math.abs(variance);

      totalAbsError += absError;
      totalPred += predicted;
      totalAct += h.actualMinutes;

      if (absError <= 15) {
        within15Count++;
      }

      let label = '🎯 High (±5m)';
      let cssClass = 'badge-success';

      if (absError <= 5) {
        label = '🎯 High (±5m)';
        cssClass = 'badge-success';
      } else if (absError <= 15) {
        label = '✓ Good (±15m)';
        cssClass = 'badge-info';
      } else if (variance > 15) {
        label = `⚠️ Under (+${variance}m)`;
        cssClass = 'badge-warning';
      } else {
        label = `⏱️ Over (-${absError}m)`;
        cssClass = 'badge-neutral';
      }

      return {
        id: h.id,
        topicName: h.topicName,
        subjectName: h.subjectName,
        difficulty: h.difficulty,
        scheduledDate: h.scheduledDate,
        plannedMinutes: h.plannedMinutes,
        predictedMinutes: predicted,
        actualMinutes: h.actualMinutes,
        predictionSource: h.predictionSource || 'HEURISTIC_FALLBACK',
        varianceMinutes: variance,
        absErrorMinutes: absError,
        accuracyLabel: label,
        accuracyClass: cssClass
      };
    });

    this.accuracyMetrics = {
      totalAnalyzed: valid.length,
      meanAbsoluteError: Math.round((totalAbsError / valid.length) * 10) / 10,
      accuracyRatePercentage: Math.round((within15Count / valid.length) * 100),
      totalPredictedMinutes: totalPred,
      totalActualMinutes: totalAct
    };
  }

  getMaxSubjectMinutes(): number {
    if (!this.weeklyData.subjectSummaries || this.weeklyData.subjectSummaries.length === 0) {
      return 1;
    }
    return Math.max(...this.weeklyData.subjectSummaries.map(s => s.completedMinutes), 1);
  }

  getSubjectBarPercentage(minutes: number): number {
    const max = this.getMaxSubjectMinutes();
    return Math.round((minutes / max) * 100);
  }
}
