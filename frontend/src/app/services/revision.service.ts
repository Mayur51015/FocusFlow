import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RevisionSession,
  RevisionHistory,
  RevisionReviewRequest,
  Flashcard,
  FlashcardCreateRequest,
  FlashcardReviewRequest
} from '../models/revision.model';

@Injectable({
  providedIn: 'root'
})
export class RevisionService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api';

  getTodayRevisions(): Observable<RevisionSession[]> {
    return this.http.get<RevisionSession[]>(`${this.apiUrl}/revisions/today`);
  }

  getUpcomingRevisions(): Observable<RevisionSession[]> {
    return this.http.get<RevisionSession[]>(`${this.apiUrl}/revisions/upcoming`);
  }

  reviewRevision(id: number, request: RevisionReviewRequest): Observable<RevisionSession> {
    return this.http.post<RevisionSession>(`${this.apiUrl}/revisions/${id}/review`, request);
  }

  getRevisionHistory(): Observable<RevisionHistory[]> {
    return this.http.get<RevisionHistory[]>(`${this.apiUrl}/revisions/history`);
  }

  getAllFlashcards(): Observable<Flashcard[]> {
    return this.http.get<Flashcard[]>(`${this.apiUrl}/flashcards`);
  }

  getDueFlashcards(): Observable<Flashcard[]> {
    return this.http.get<Flashcard[]>(`${this.apiUrl}/flashcards/due`);
  }

  getFlashcardsByTopic(topicId: number): Observable<Flashcard[]> {
    return this.http.get<Flashcard[]>(`${this.apiUrl}/topics/${topicId}/flashcards`);
  }

  createFlashcard(request: FlashcardCreateRequest): Observable<Flashcard> {
    return this.http.post<Flashcard>(`${this.apiUrl}/flashcards`, request);
  }

  reviewFlashcard(cardId: number, request: FlashcardReviewRequest): Observable<Flashcard> {
    return this.http.post<Flashcard>(`${this.apiUrl}/flashcards/${cardId}/review`, request);
  }
}

