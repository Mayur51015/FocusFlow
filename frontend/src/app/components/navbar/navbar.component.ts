import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="navbar">
      <div class="nav-container">
        <div class="brand">
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
        </div>

        <nav class="nav-links">
          <a routerLink="/dashboard" routerLinkActive="active" class="nav-item">Dashboard</a>
          <a routerLink="/schedule" routerLinkActive="active" class="nav-item">Daily Schedule</a>
          <a routerLink="/subjects" routerLinkActive="active" class="nav-item">Subjects</a>
          <a routerLink="/topics" routerLinkActive="active" class="nav-item">Topics</a>
          <a routerLink="/quizzes" routerLinkActive="active" class="nav-item">Quizzes</a>
          <a routerLink="/revisions" routerLinkActive="active" class="nav-item">Revisions</a>
          <a routerLink="/pomodoro" routerLinkActive="active" class="nav-item">Pomodoro</a>
          <a routerLink="/progress" routerLinkActive="active" class="nav-item">Analytics</a>
        </nav>

        <div class="user-badge">
          <span class="user-avatar">🎓</span>
          <span class="user-name">Student Mode</span>
        </div>
      </div>
    </header>
  `,
  styles: [`
    .navbar {
      background: #ffffff;
      border-bottom: 1px solid var(--border);
      position: sticky;
      top: 0;
      z-index: 100;
      box-shadow: 0 1px 4px rgba(0, 0, 0, 0.02);
    }
    .nav-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 20px;
      height: 70px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 20px;
    }
    .brand {
      display: flex;
      align-items: center;
      gap: 12px;
      text-decoration: none;
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
      color: var(--text-main);
      letter-spacing: -0.02em;
    }
    .brand-subtitle {
      font-size: 0.72rem;
      color: var(--text-muted);
      font-weight: 500;
    }
    .nav-links {
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .nav-item {
      padding: 8px 14px;
      font-size: 0.88rem;
      font-weight: 600;
      color: var(--text-muted);
      text-decoration: none;
      border-radius: var(--radius-sm);
      transition: all 0.15s ease;
    }
    .nav-item:hover {
      color: var(--primary);
      background: var(--primary-light);
    }
    .nav-item.active {
      color: #ffffff;
      background: var(--primary);
      box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
    }
    .user-badge {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 6px 14px;
      background: #f8fafc;
      border: 1px solid var(--border);
      border-radius: 9999px;
    }
    .user-avatar {
      font-size: 1rem;
    }
    .user-name {
      font-size: 0.825rem;
      font-weight: 600;
      color: var(--text-main);
    }
    @media (max-width: 850px) {
      .nav-container {
        height: auto;
        padding: 12px 16px;
        flex-direction: column;
        gap: 12px;
      }
      .nav-links {
        flex-wrap: wrap;
        justify-content: center;
      }
      .user-badge {
        display: none;
      }
    }
  `]
})
export class NavbarComponent {}
