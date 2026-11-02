import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RevisionService } from '../../services/revision.service';
import { TopicService } from '../../services/topic.service';
import {
  RevisionSession,
  RevisionHistory,
  Flashcard,
  RecallRating,
  FlashcardCreateRequest
} from '../../models/revision.model';
import { Topic } from '../../models/topic.model';

@Component({
  selector: 'app-revisions',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './revisions.component.html',
  styleUrls: ['./revisions.component.css']
})
export class RevisionsComponent implements OnInit {
  private revisionService = inject(RevisionService);
  private topicService = inject(TopicService);

  activeTab: 'TODAY' | 'UPCOMING' | 'FLASHCARDS' | 'HISTORY' = 'TODAY';

  todayRevisions: RevisionSession[] = [];
  upcomingRevisions: RevisionSession[] = [];
  revisionHistory: RevisionHistory[] = [];
  flashcards: Flashcard[] = [];
  topics: Topic[] = [];

  isLoading: boolean = false;
  successMessage: string = '';
  errorMessage: string = '';

  // Reviewing modal state for RevisionSession
  reviewingSession: RevisionSession | null = null;
  reviewNotes: string = '';

  // Flashcards state
  activeCardIndex: number = 0;
  isCardFlipped: boolean = false;
  showCardHint: boolean = false;
  showNewCardModal: boolean = false;
  newCard: FlashcardCreateRequest = {
    topicId: 0,
    frontPrompt: '',
    backAnswer: '',
    hint: ''
  };

  ngOnInit(): void {
    this.loadTodayRevisions();
    this.loadTopics();
  }

  setTab(tab: 'TODAY' | 'UPCOMING' | 'FLASHCARDS' | 'HISTORY'): void {
    this.activeTab = tab;
    this.errorMessage = '';
    this.successMessage = '';

    if (tab === 'TODAY') this.loadTodayRevisions();
    else if (tab === 'UPCOMING') this.loadUpcomingRevisions();
    else if (tab === 'FLASHCARDS') this.loadFlashcards();
    else if (tab === 'HISTORY') this.loadHistory();
  }

  loadTopics(): void {
    this.topicService.getAllTopics().subscribe({
      next: (data) => {
        this.topics = data;
        if (data.length > 0) {
          this.newCard.topicId = data[0].id || 0;
        }
      },
      error: (err) => console.warn('Could not load topics:', err)
    });
  }

  loadTodayRevisions(): void {
    this.isLoading = true;
    this.revisionService.getTodayRevisions().subscribe({
      next: (data) => {
        this.todayRevisions = data;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = 'Failed to load today revisions.';
        this.isLoading = false;
      }
    });
  }

  loadUpcomingRevisions(): void {
    this.isLoading = true;
    this.revisionService.getUpcomingRevisions().subscribe({
      next: (data) => {
        this.upcomingRevisions = data;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = 'Failed to load upcoming revisions.';
        this.isLoading = false;
      }
    });
  }

  loadHistory(): void {
    this.isLoading = true;
    this.revisionService.getRevisionHistory().subscribe({
      next: (data) => {
        this.revisionHistory = data;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = 'Failed to load revision history.';
        this.isLoading = false;
      }
    });
  }

  loadFlashcards(): void {
    this.isLoading = true;
    this.revisionService.getAllFlashcards().subscribe({
      next: (data) => {
        this.flashcards = data;
        this.activeCardIndex = 0;
        this.isCardFlipped = false;
        this.showCardHint = false;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = 'Failed to load flashcards.';
        this.isLoading = false;
      }
    });
  }

  openReviewModal(session: RevisionSession): void {
    this.reviewingSession = session;
    this.reviewNotes = '';
  }

  closeReviewModal(): void {
    this.reviewingSession = null;
    this.reviewNotes = '';
  }

  submitSessionReview(rating: RecallRating): void {
    if (!this.reviewingSession) return;
    const sessionId = this.reviewingSession.id;
    const topicName = this.reviewingSession.topicName;

    this.revisionService.reviewRevision(sessionId, { rating, notes: this.reviewNotes }).subscribe({
      next: (updated) => {
        this.successMessage = `Recorded rating "${rating}" for "${topicName}". Next revision scheduled for ${updated.scheduledDate} (${updated.intervalDays}d interval)!`;
        this.closeReviewModal();
        this.loadTodayRevisions();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to submit recall review.';
      }
    });
  }

  // --- Flashcard Review Controls ---

  flipCard(): void {
    this.isCardFlipped = !this.isCardFlipped;
  }

  toggleHint(): void {
    this.showCardHint = !this.showCardHint;
  }

  rateCurrentFlashcard(rating: RecallRating): void {
    if (this.flashcards.length === 0) return;
    const card = this.flashcards[this.activeCardIndex];

    this.revisionService.reviewFlashcard(card.id, { rating }).subscribe({
      next: (updatedCard) => {
        this.flashcards[this.activeCardIndex] = updatedCard;
        this.isCardFlipped = false;
        this.showCardHint = false;

        if (this.activeCardIndex < this.flashcards.length - 1) {
          this.activeCardIndex++;
        } else {
          this.successMessage = '🎉 You completed all available concept flashcards for this deck!';
        }
      },
      error: (err) => {
        this.errorMessage = 'Failed to record flashcard review.';
      }
    });
  }

  prevFlashcard(): void {
    if (this.activeCardIndex > 0) {
      this.activeCardIndex--;
      this.isCardFlipped = false;
      this.showCardHint = false;
    }
  }

  nextFlashcard(): void {
    if (this.activeCardIndex < this.flashcards.length - 1) {
      this.activeCardIndex++;
      this.isCardFlipped = false;
      this.showCardHint = false;
    }
  }

  saveNewFlashcard(): void {
    if (!this.newCard.frontPrompt.trim() || !this.newCard.backAnswer.trim()) {
      this.errorMessage = 'Both question prompt and answer are required.';
      return;
    }
    this.revisionService.createFlashcard(this.newCard).subscribe({
      next: (created) => {
        this.successMessage = 'New concept flashcard created successfully!';
        this.showNewCardModal = false;
        this.newCard = { topicId: this.topics[0]?.id || 0, frontPrompt: '', backAnswer: '', hint: '' };
        this.loadFlashcards();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to create flashcard.';
      }
    });
  }
}
