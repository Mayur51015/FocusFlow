import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ExamReadiness, ExamReadinessOverview } from '../models/exam-readiness.model';

@Injectable({
  providedIn: 'root'
})
export class ExamReadinessService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api';

  getSubjectReadiness(subjectId: number): Observable<ExamReadiness> {
    return this.http.get<ExamReadiness>(`${this.apiUrl}/subjects/${subjectId}/exam-readiness`);
  }

  calculateSubjectReadiness(subjectId: number): Observable<ExamReadiness> {
    return this.http.post<ExamReadiness>(`${this.apiUrl}/subjects/${subjectId}/calculate-readiness`, {});
  }

  getReadinessOverview(): Observable<ExamReadinessOverview> {
    return this.http.get<ExamReadinessOverview>(`${this.apiUrl}/exam-readiness/overview`);
  }

  calculateAllReadiness(): Observable<ExamReadiness[]> {
    return this.http.post<ExamReadiness[]>(`${this.apiUrl}/exam-readiness/calculate-all`, {});
  }
}

