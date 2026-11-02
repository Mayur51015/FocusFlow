import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TopicPerformance } from '../models/topic-performance.model';

@Injectable({
  providedIn: 'root'
})
export class TopicPerformanceService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/topics';

  getAllPerformances(): Observable<TopicPerformance[]> {
    return this.http.get<TopicPerformance[]>(`${this.apiUrl}/performance`);
  }

  getWeakTopics(): Observable<TopicPerformance[]> {
    return this.http.get<TopicPerformance[]>(`${this.apiUrl}/weak`);
  }

  getTopicPerformance(topicId: number): Observable<TopicPerformance> {
    return this.http.get<TopicPerformance>(`${this.apiUrl}/${topicId}/performance`);
  }

  evaluateTopic(topicId: number): Observable<TopicPerformance> {
    return this.http.post<TopicPerformance>(`${this.apiUrl}/${topicId}/evaluate-performance`, {});
  }

  evaluateAllTopics(): Observable<TopicPerformance[]> {
    return this.http.post<TopicPerformance[]>(`${this.apiUrl}/evaluate-all`, {});
  }
}

