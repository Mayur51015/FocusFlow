import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SubjectService } from '../../services/subject.service';
import { ExamReadinessService } from '../../services/exam-readiness.service';
import { Subject, Importance } from '../../models/subject.model';
import { ExamReadiness, ExamReadinessOverview } from '../../models/exam-readiness.model';

@Component({
  selector: 'app-subjects',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './subjects.component.html',
  styleUrls: ['./subjects.component.css']
})
export class SubjectsComponent implements OnInit {
  subjects: Subject[] = [];
  readinessMap: { [subjectId: number]: ExamReadiness } = {};
  selectedReadiness: ExamReadiness | null = null;
  readinessOverview: ExamReadinessOverview | null = null;
  
  // Form model
  currentSubject: Subject = {
    name: '',
    examDate: '',
    importance: 'MEDIUM'
  };

  isEditing: boolean = false;
  editingId: number | null = null;
  isLoading: boolean = false;
  isCalculatingReadiness: boolean = false;
  successMessage: string = '';
  errorMessage: string = '';

  constructor(
    private subjectService: SubjectService,
    private examReadinessService: ExamReadinessService
  ) {}

  ngOnInit(): void {
    this.loadSubjects();
  }

  loadSubjects(): void {
    this.isLoading = true;
    this.subjectService.getAllSubjects().subscribe({
      next: (data) => {
        this.subjects = data;
        this.isLoading = false;
        this.loadReadinessOverview();
      },
      error: (err) => {
        console.error('Failed to load subjects', err);
        this.errorMessage = 'Could not load subjects from backend.';
        this.isLoading = false;
      }
    });
  }

  loadReadinessOverview(): void {
    this.examReadinessService.getReadinessOverview().subscribe({
      next: (overview) => {
        this.readinessOverview = overview;
        if (overview.subjectEvaluations) {
          for (const ev of overview.subjectEvaluations) {
            this.readinessMap[ev.subjectId] = ev;
          }
        }
      },
      error: (err) => {
        console.warn('Could not load readiness overview:', err);
      }
    });
  }

  calculateSubjectReadiness(subjectId?: number): void {
    if (!subjectId) return;
    this.isCalculatingReadiness = true;
    this.examReadinessService.calculateSubjectReadiness(subjectId).subscribe({
      next: (res) => {
        this.readinessMap[subjectId] = res;
        this.selectedReadiness = res;
        this.isCalculatingReadiness = false;
        this.successMessage = `Readiness updated for ${res.subjectName}: ${res.readinessScore}% (${res.readinessLevel})`;
        this.loadReadinessOverview();
      },
      error: (err) => {
        this.errorMessage = 'Failed to calculate readiness score.';
        this.isCalculatingReadiness = false;
      }
    });
  }

  calculateAllReadiness(): void {
    this.isCalculatingReadiness = true;
    this.examReadinessService.calculateAllReadiness().subscribe({
      next: (list) => {
        for (const item of list) {
          this.readinessMap[item.subjectId] = item;
        }
        this.isCalculatingReadiness = false;
        this.successMessage = 'Evaluated exam readiness across all enrolled subjects!';
        this.loadReadinessOverview();
      },
      error: (err) => {
        this.errorMessage = 'Failed to batch evaluate exam readiness.';
        this.isCalculatingReadiness = false;
      }
    });
  }

  viewReadiness(subjectId?: number): void {
    if (!subjectId) return;
    if (this.readinessMap[subjectId]) {
      this.selectedReadiness = this.readinessMap[subjectId];
    } else {
      this.calculateSubjectReadiness(subjectId);
    }
  }

  closeReadinessModal(): void {
    this.selectedReadiness = null;
  }

  saveSubject(): void {
    if (!this.currentSubject.name.trim()) {
      this.errorMessage = 'Please enter a subject name.';
      return;
    }
    if (!this.currentSubject.examDate) {
      this.errorMessage = 'Please select an exam date.';
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    if (this.isEditing && this.editingId) {
      this.subjectService.updateSubject(this.editingId, this.currentSubject).subscribe({
        next: (updated) => {
          this.successMessage = `Subject "${updated.name}" updated successfully!`;
          this.resetForm();
          this.loadSubjects();
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Failed to update subject.';
        }
      });
    } else {
      this.subjectService.createSubject(this.currentSubject).subscribe({
        next: (created) => {
          this.successMessage = `Subject "${created.name}" created successfully!`;
          this.resetForm();
          this.loadSubjects();
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Failed to create subject.';
        }
      });
    }
  }

  startEdit(subject: Subject): void {
    this.isEditing = true;
    this.editingId = subject.id || null;
    this.currentSubject = {
      name: subject.name,
      examDate: subject.examDate,
      importance: subject.importance
    };
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  cancelEdit(): void {
    this.resetForm();
  }

  deleteSubject(id?: number): void {
    if (!id) return;
    if (confirm('Are you sure you want to delete this subject? All associated topics will also be removed.')) {
      this.subjectService.deleteSubject(id).subscribe({
        next: () => {
          this.successMessage = 'Subject deleted successfully.';
          this.loadSubjects();
        },
        error: (err) => {
          this.errorMessage = 'Failed to delete subject.';
        }
      });
    }
  }

  resetForm(): void {
    this.isEditing = false;
    this.editingId = null;
    this.currentSubject = {
      name: '',
      examDate: '',
      importance: 'MEDIUM'
    };
  }

  getDaysUntilExam(examDateStr: string): number {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const exam = new Date(examDateStr);
    exam.setHours(0, 0, 0, 0);
    const diffTime = exam.getTime() - today.getTime();
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  }
}
