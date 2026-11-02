import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ActualDurationUpdateRequest,
  AdaptiveScheduleSummaryDto,
  MlHealthStatus,
  RecalculateScheduleRequest,
  ScheduleRequest,
  SessionStatusUpdateRequest,
  StudyHistoryDto,
  StudySession,
  TopicPredictionResponse
} from '../models/study-session.model';

@Injectable({
  providedIn: 'root'
})
export class StudySessionService {
  private readonly apiUrl = 'http://localhost:8080/api/schedule';
  private readonly historyUrl = 'http://localhost:8080/api/study-history';

  constructor(private http: HttpClient) {}

  generateSchedule(request: ScheduleRequest): Observable<StudySession[]> {
    return this.http.post<StudySession[]>(`${this.apiUrl}/generate`, request);
  }

  recalculateSchedule(request?: RecalculateScheduleRequest): Observable<AdaptiveScheduleSummaryDto> {
    return this.http.post<AdaptiveScheduleSummaryDto>(`${this.apiUrl}/recalculate`, request || {});
  }

  getSessionsByDate(date?: string): Observable<StudySession[]> {
    let params = new HttpParams();
    if (date) {
      params = params.set('date', date);
    }
    return this.http.get<StudySession[]>(this.apiUrl, { params });
  }

  updateSessionStatus(sessionId: number, request: SessionStatusUpdateRequest): Observable<StudySession> {
    return this.http.patch<StudySession>(`${this.apiUrl}/sessions/${sessionId}/status`, request);
  }

  updateActualDuration(sessionId: number, actualMinutes: number): Observable<StudySession> {
    const payload: ActualDurationUpdateRequest = { actualMinutes };
    return this.http.patch<StudySession>(`${this.apiUrl}/sessions/${sessionId}/actual-duration`, payload);
  }

  toggleLock(sessionId: number): Observable<StudySession> {
    return this.http.patch<StudySession>(`${this.apiUrl}/sessions/${sessionId}/lock`, {});
  }

  deleteSession(sessionId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/sessions/${sessionId}`);
  }

  getStudyHistory(completedOnly: boolean = false): Observable<StudyHistoryDto[]> {
    let params = new HttpParams().set('completedOnly', completedOnly ? 'true' : 'false');
    return this.http.get<StudyHistoryDto[]>(this.historyUrl, { params });
  }

  predictDuration(topicId: number): Observable<TopicPredictionResponse> {
    return this.http.post<TopicPredictionResponse>(`${this.apiUrl}/predict-duration`, { topicId });
  }

  getMlHealth(): Observable<MlHealthStatus> {
    return this.http.get<MlHealthStatus>(`${this.apiUrl}/ml-health`);
  }
}
