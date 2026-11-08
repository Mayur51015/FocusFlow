import { Routes } from '@angular/router';
import { LandingComponent } from './components/landing/landing.component';
import { RegisterComponent } from './components/auth/register/register.component';
import { LoginComponent } from './components/auth/login/login.component';
import { ForgotPasswordComponent } from './components/auth/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './components/auth/reset-password/reset-password.component';
import { AccessDeniedComponent } from './components/auth/access-denied/access-denied.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { SubjectsComponent } from './components/subjects/subjects.component';
import { TopicsComponent } from './components/topics/topics.component';
import { ScheduleComponent } from './components/schedule/schedule.component';
import { PomodoroComponent } from './components/pomodoro/pomodoro.component';
import { ProgressComponent } from './components/progress/progress.component';
import { QuizzesComponent } from './components/quizzes/quizzes.component';
import { RevisionsComponent } from './components/revisions/revisions.component';
import { AdminComponent } from './components/admin/admin.component';
import { authGuard } from './guards/auth.guard';
import { adminGuard } from './guards/admin.guard';

export const routes: Routes = [
  // Public Routes
  { path: '', component: LandingComponent, pathMatch: 'full' },
  { path: 'register', component: RegisterComponent },
  { path: 'login', component: LoginComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password', component: ResetPasswordComponent },
  { path: 'access-denied', component: AccessDeniedComponent },

  // Protected Core Academic Workspaces (Enforced by authGuard)
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'quizzes', component: QuizzesComponent, canActivate: [authGuard] },
  { path: 'revisions', component: RevisionsComponent, canActivate: [authGuard] },
  { path: 'subjects', component: SubjectsComponent, canActivate: [authGuard] },
  { path: 'topics', component: TopicsComponent, canActivate: [authGuard] },
  { path: 'schedule', component: ScheduleComponent, canActivate: [authGuard] },
  { path: 'pomodoro', component: PomodoroComponent, canActivate: [authGuard] },
  { path: 'progress', component: ProgressComponent, canActivate: [authGuard] },

  // Phase 5 Dedicated Admin Dashboard (Enforced by adminGuard)
  { path: 'admin', component: AdminComponent, canActivate: [adminGuard] },

  // Wildcard fallback to landing page
  { path: '**', redirectTo: '' }
];
