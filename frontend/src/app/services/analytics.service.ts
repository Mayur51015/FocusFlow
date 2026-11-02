import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  LearningSummary,
  SubjectMastery,
  RetentionTrends
} from '../models/analytics.model';

@Injectable({
  providedIn: 'root'
})
export class AnalyticsService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/analytics';

  getLearningSummary(): Observable<LearningSummary> {
    return this.http.get<LearningSummary>(`${this.apiUrl}/learning-summary`);
  }

  getSubjectMastery(): Observable<SubjectMastery[]> {
    return this.http.get<SubjectMastery[]>(`${this.apiUrl}/subject-mastery`);
  }

  getRetentionTrends(): Observable<RetentionTrends> {
    return this.http.get<RetentionTrends>(`${this.apiUrl}/retention-trends`);
  }
}

