import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  QuizSummary,
  QuizDetail,
  QuizAttemptSubmitRequest,
  QuizAttemptResult,
  QuizAttemptHistory,
  TopicQuizPerformance,
  QuizCreateRequest
} from '../models/quiz.model';

@Injectable({
  providedIn: 'root'
})
export class QuizService {

  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  getQuizzes(subjectId?: number, topicId?: number): Observable<QuizSummary[]> {
    let params = new HttpParams();
    if (subjectId) {
      params = params.set('subjectId', subjectId.toString());
    }
    if (topicId) {
      params = params.set('topicId', topicId.toString());
    }
    return this.http.get<QuizSummary[]>(`${this.apiUrl}/quizzes`, { params });
  }

  getQuizForAttempt(quizId: number): Observable<QuizDetail> {
    return this.http.get<QuizDetail>(`${this.apiUrl}/quizzes/${quizId}`);
  }

  submitQuiz(quizId: number, payload: QuizAttemptSubmitRequest): Observable<QuizAttemptResult> {
    return this.http.post<QuizAttemptResult>(`${this.apiUrl}/quizzes/${quizId}/submit`, payload);
  }

  getAttemptHistory(quizId?: number): Observable<QuizAttemptHistory[]> {
    let params = new HttpParams();
    if (quizId) {
      params = params.set('quizId', quizId.toString());
    }
    return this.http.get<QuizAttemptHistory[]>(`${this.apiUrl}/quiz-attempts`, { params });
  }

  getTopicQuizPerformance(topicId: number): Observable<TopicQuizPerformance> {
    return this.http.get<TopicQuizPerformance>(`${this.apiUrl}/topics/${topicId}/quiz-performance`);
  }

  createQuiz(payload: QuizCreateRequest): Observable<QuizSummary> {
    return this.http.post<QuizSummary>(`${this.apiUrl}/quizzes`, payload);
  }
}

