import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TopicService } from '../../services/topic.service';
import { SubjectService } from '../../services/subject.service';
import { TopicPerformanceService } from '../../services/topic-performance.service';
import { Topic, Difficulty } from '../../models/topic.model';
import { Subject } from '../../models/subject.model';
import { TopicPerformance } from '../../models/topic-performance.model';

@Component({
  selector: 'app-topics',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './topics.component.html',
  styleUrls: ['./topics.component.css']
})
export class TopicsComponent implements OnInit {
  topics: Topic[] = [];
  subjects: Subject[] = [];
  performancesMap: Map<number, TopicPerformance> = new Map();
  weakTopics: TopicPerformance[] = [];
  isEvaluating: boolean = false;

  selectedSubjectFilter: string = 'ALL';

  // Form model
  currentTopic: {
    name: string;
    subjectId: number | null;
    difficulty: Difficulty;
    estimatedMinutes: number;
    completed: boolean;
  } = {
    name: '',
    subjectId: null,
    difficulty: 'MEDIUM',
    estimatedMinutes: 60,
    completed: false
  };

  isEditing: boolean = false;
  editingId: number | null = null;
  isLoading: boolean = false;
  successMessage: string = '';
  errorMessage: string = '';

  constructor(
    private topicService: TopicService,
    private subjectService: SubjectService,
    private performanceService: TopicPerformanceService
  ) {}

  ngOnInit(): void {
    this.loadSubjects();
    this.loadTopics();
    this.loadPerformances();
  }

  loadPerformances(): void {
    this.performanceService.getAllPerformances().subscribe({
      next: (data) => {
        const map = new Map<number, TopicPerformance>();
        const weaks: TopicPerformance[] = [];
        for (const p of data) {
          map.set(p.topicId, p);
          if (p.performanceLevel === 'WEAK') {
            weaks.push(p);
          }
        }
        this.performancesMap = map;
        this.weakTopics = weaks;
      },
      error: (err) => console.warn('Could not load topic performances:', err)
    });
  }

  getPerformance(topicId?: number): TopicPerformance | undefined {
    return topicId ? this.performancesMap.get(topicId) : undefined;
  }

  evaluateAllTopics(): void {
    this.isEvaluating = true;
    this.performanceService.evaluateAllTopics().subscribe({
      next: (data) => {
        this.isEvaluating = false;
        this.successMessage = `ML Evaluation completed for ${data.length} topics!`;
        this.loadPerformances();
      },
      error: (err) => {
        this.isEvaluating = false;
        this.errorMessage = 'Failed to evaluate topics with ML microservice.';
      }
    });
  }

  evaluateTopic(topicId?: number): void {
    if (!topicId) return;
    this.performanceService.evaluateTopic(topicId).subscribe({
      next: (perf) => {
        this.performancesMap.set(perf.topicId, perf);
        this.loadPerformances();
        this.successMessage = `Topic "${perf.topicName}" evaluated: ${perf.performanceLevel} (${perf.predictionSource})`;
      },
      error: (err) => {
        this.errorMessage = 'Failed to evaluate topic performance.';
      }
    });
  }

  loadSubjects(): void {
    this.subjectService.getAllSubjects().subscribe({
      next: (data) => {
        this.subjects = data;
        if (this.subjects.length > 0 && !this.currentTopic.subjectId) {
          this.currentTopic.subjectId = this.subjects[0].id || null;
        }
      },
      error: (err) => {
        console.error('Failed to load subjects', err);
      }
    });
  }

  loadTopics(): void {
    this.isLoading = true;
    const filterId = this.selectedSubjectFilter === 'ALL' ? undefined : Number(this.selectedSubjectFilter);

    this.topicService.getAllTopics(filterId).subscribe({
      next: (data) => {
        this.topics = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load topics', err);
        this.errorMessage = 'Could not load topics from backend.';
        this.isLoading = false;
      }
    });
  }

  saveTopic(): void {
    if (!this.currentTopic.name.trim()) {
      this.errorMessage = 'Please enter a topic name.';
      return;
    }
    if (!this.currentTopic.subjectId) {
      this.errorMessage = 'Please select a subject.';
      return;
    }
    if (this.currentTopic.estimatedMinutes <= 0) {
      this.errorMessage = 'Estimated study duration must be greater than 0 minutes.';
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    const payload: Topic = {
      name: this.currentTopic.name,
      difficulty: this.currentTopic.difficulty,
      estimatedMinutes: this.currentTopic.estimatedMinutes,
      completed: this.currentTopic.completed,
      subject: { id: this.currentTopic.subjectId } as Subject
    };

    if (this.isEditing && this.editingId) {
      this.topicService.updateTopic(this.editingId, payload).subscribe({
        next: (updated) => {
          this.successMessage = `Topic "${updated.name}" updated successfully!`;
          this.resetForm();
          this.loadTopics();
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Failed to update topic.';
        }
      });
    } else {
      this.topicService.createTopic(payload).subscribe({
        next: (created) => {
          this.successMessage = `Topic "${created.name}" created successfully!`;
          this.resetForm();
          this.loadTopics();
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Failed to create topic.';
        }
      });
    }
  }

  startEdit(topic: Topic): void {
    this.isEditing = true;
    this.editingId = topic.id || null;
    this.currentTopic = {
      name: topic.name,
      subjectId: topic.subject?.id || null,
      difficulty: topic.difficulty,
      estimatedMinutes: topic.estimatedMinutes,
      completed: topic.completed
    };
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  cancelEdit(): void {
    this.resetForm();
  }

  toggleCompletion(topic: Topic): void {
    if (!topic.id) return;
    this.topicService.toggleCompletion(topic.id).subscribe({
      next: (updated) => {
        topic.completed = updated.completed;
      },
      error: (err) => {
        this.errorMessage = 'Failed to toggle topic completion.';
      }
    });
  }

  deleteTopic(id?: number): void {
    if (!id) return;
    if (confirm('Are you sure you want to delete this study topic?')) {
      this.topicService.deleteTopic(id).subscribe({
        next: () => {
          this.successMessage = 'Topic deleted successfully.';
          this.loadTopics();
        },
        error: (err) => {
          this.errorMessage = 'Failed to delete topic.';
        }
      });
    }
  }

  resetForm(): void {
    this.isEditing = false;
    this.editingId = null;
    this.currentTopic = {
      name: '',
      subjectId: this.subjects.length > 0 ? (this.subjects[0].id || null) : null,
      difficulty: 'MEDIUM',
      estimatedMinutes: 60,
      completed: false
    };
  }
}
