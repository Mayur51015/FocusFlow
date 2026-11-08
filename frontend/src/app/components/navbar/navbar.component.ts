import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <header class="navbar">
      <div class="nav-container">
        <!-- Brand -->
        <a routerLink="/" class="brand">
          <div class="brand-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <polygon points="12 2 2 7 12 12 22 7 12 2"></polygon>
              <polyline points="2 17 12 22 22 17"></polyline>
              <polyline points="2 12 12 17 22 12"></polyline>
            </svg>
          </div>
          <div class="brand-text">
            <span class="brand-title">FocusFlow</span>
            <span class="brand-subtitle">Smart Study Planner</span>
          </div>
        </a>

        <!-- Authenticated Navigation Links -->
        @if (authService.isAuthenticated()) {
          <nav class="nav-links">
            <a routerLink="/dashboard" routerLinkActive="active" class="nav-item">Dashboard</a>
            <a routerLink="/schedule" routerLinkActive="active" class="nav-item">Daily Schedule</a>
            <a routerLink="/subjects" routerLinkActive="active" class="nav-item">Subjects</a>
            <a routerLink="/topics" routerLinkActive="active" class="nav-item">Topics</a>
            <a routerLink="/quizzes" routerLinkActive="active" class="nav-item">Quizzes</a>
            <a routerLink="/revisions" routerLinkActive="active" class="nav-item">Revisions</a>
            <a routerLink="/pomodoro" routerLinkActive="active" class="nav-item">Pomodoro</a>
            <a routerLink="/progress" routerLinkActive="active" class="nav-item">Analytics</a>
            @if (authService.isAdmin()) {
              <a routerLink="/admin" routerLinkActive="active" class="nav-item nav-item-admin">
                <span class="admin-badge-icon">🛡️</span> Admin Console
              </a>
            }
          </nav>

          <!-- User Profile & Logout -->
          <div class="user-section">
            <div class="user-badge" [class.badge-admin]="authService.isAdmin()">
              <span class="user-avatar">{{ authService.isAdmin() ? '🛡️' : '🎓' }}</span>
              <div class="user-info">
                <span class="user-name">{{ authService.currentUser()?.fullName || 'User' }}</span>
                <span class="user-role">{{ authService.isAdmin() ? 'Admin' : 'Student' }}</span>
              </div>
            </div>
            <button (click)="logout()" class="btn-logout" title="Sign out of FocusFlow">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                <polyline points="16 17 21 12 16 7"></polyline>
                <line x1="21" y1="12" x2="9" y2="12"></line>
              </svg>
              <span>Logout</span>
            </button>
          </div>
        } @else {
          <!-- Unauthenticated Action Buttons -->
          <div class="guest-actions">
            <a routerLink="/login" class="btn-login">Login</a>
            <a routerLink="/register" class="btn-register">Get Started</a>
          </div>
        }
      </div>
    </header>
  `,
  styles: [`
    .navbar {
      background: #ffffff;
      border-bottom: 1px solid var(--border, #e2e8f0);
      position: sticky;
      top: 0;
      z-index: 100;
      box-shadow: 0 1px 4px rgba(0, 0, 0, 0.02);
    }
    .nav-container {
      max-width: 1300px;
      margin: 0 auto;
      padding: 0 20px;
      height: 70px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
    }
    .brand {
      display: flex;
      align-items: center;
      gap: 12px;
      text-decoration: none;
      flex-shrink: 0;
    }
    .brand-icon {
      width: 40px;
      height: 40px;
      border-radius: 12px;
      background: linear-gradient(135deg, #2563eb, #7c3aed);
      color: #ffffff;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 4px 10px rgba(37, 99, 235, 0.25);
    }
    .brand-text {
      display: flex;
      flex-direction: column;
    }
    .brand-title {
      font-size: 1.15rem;
      font-weight: 800;
      color: var(--text-main, #0f172a);
      letter-spacing: -0.02em;
    }
    .brand-subtitle {
      font-size: 0.72rem;
      color: var(--text-muted, #64748b);
      font-weight: 500;
    }
    .nav-links {
      display: flex;
      align-items: center;
      gap: 4px;
      flex-wrap: wrap;
    }
    .nav-item {
      padding: 8px 12px;
      font-size: 0.85rem;
      font-weight: 600;
      color: var(--text-muted, #64748b);
      text-decoration: none;
      border-radius: var(--radius-sm, 8px);
      transition: all 0.15s ease;
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .nav-item:hover {
      color: var(--primary, #2563eb);
      background: var(--primary-light, #eff6ff);
    }
    .nav-item.active {
      color: #ffffff;
      background: var(--primary, #2563eb);
      box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
    }
    .nav-item-admin {
      color: #7c3aed;
      border: 1px dashed #c4b5fd;
    }
    .nav-item-admin:hover {
      background: #f5f3ff;
      color: #6d28d9;
    }
    .nav-item-admin.active {
      background: linear-gradient(135deg, #7c3aed, #4f46e5);
      border-color: transparent;
      color: #ffffff;
      box-shadow: 0 2px 8px rgba(124, 58, 237, 0.25);
    }
    .admin-badge-icon {
      font-size: 0.9rem;
    }
    .user-section {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-shrink: 0;
    }
    .user-badge {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 4px 12px;
      background: #f8fafc;
      border: 1px solid var(--border, #e2e8f0);
      border-radius: 9999px;
    }
    .user-badge.badge-admin {
      background: #faf5ff;
      border-color: #e9d5ff;
    }
    .user-avatar {
      font-size: 1rem;
    }
    .user-info {
      display: flex;
      flex-direction: column;
    }
    .user-name {
      font-size: 0.8rem;
      font-weight: 700;
      color: var(--text-main, #0f172a);
      line-height: 1.1;
      max-width: 120px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .user-role {
      font-size: 0.68rem;
      color: #64748b;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }
    .btn-logout {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 7px 12px;
      background: #ffffff;
      border: 1px solid var(--border, #e2e8f0);
      border-radius: 8px;
      font-size: 0.825rem;
      font-weight: 600;
      color: #dc2626;
      cursor: pointer;
      transition: all 0.15s ease;
    }
    .btn-logout:hover {
      background: #fef2f2;
      border-color: #fecaca;
    }
    .guest-actions {
      display: flex;
      align-items: center;
      gap: 10px;
    }
    .btn-login {
      font-size: 0.875rem;
      font-weight: 600;
      color: var(--text-main, #0f172a);
      text-decoration: none;
      padding: 8px 16px;
      border-radius: 8px;
      transition: background 0.15s ease;
    }
    .btn-login:hover {
      background: #f1f5f9;
    }
    .btn-register {
      font-size: 0.875rem;
      font-weight: 700;
      color: #ffffff;
      background: var(--primary, #2563eb);
      text-decoration: none;
      padding: 8px 18px;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
      transition: all 0.15s ease;
    }
    .btn-register:hover {
      background: var(--primary-hover, #1d4ed8);
    }
    @media (max-width: 950px) {
      .nav-container {
        height: auto;
        padding: 12px 16px;
        flex-direction: column;
        gap: 12px;
      }
      .nav-links {
        justify-content: center;
      }
    }
  `]
})
export class NavbarComponent {
  authService = inject(AuthService);

  logout(): void {
    this.authService.logout();
  }
}
