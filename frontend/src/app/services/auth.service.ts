import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, throwError, of } from 'rxjs';
import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  ChangePasswordRequest,
  User,
  UserProfile
} from '../models/user.model';

const TOKEN_KEY = 'focusflow_access_token';
const USER_KEY = 'focusflow_user';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private readonly baseUrl = 'http://localhost:8080/api/auth';

  // Signals for reactive, zoneless-friendly state
  readonly accessToken = signal<string | null>(this.getStoredToken());
  readonly currentUser = signal<User | null>(this.getStoredUser());

  readonly isAuthenticated = computed(() => {
    return !!this.accessToken() && !!this.currentUser();
  });

  readonly isAdmin = computed(() => {
    const user = this.currentUser();
    if (!user) return false;
    return user.role === 'ROLE_ADMIN' || (Array.isArray(user.roles) && user.roles.includes('ROLE_ADMIN'));
  });

  constructor() {
    // If a token exists in storage on initialization, attempt to synchronize profile
    if (this.accessToken()) {
      this.fetchCurrentUser().subscribe({
        error: () => {
          // If token is invalid or expired, attempt refresh
          this.refreshToken().subscribe({
            error: () => this.clearSession()
          });
        }
      });
    }
  }

  /**
   * Register a new student account.
   */
  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/register`, request);
  }

  /**
   * Authenticate user with credentials.
   */
  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, request, { withCredentials: true }).pipe(
      tap((res) => {
        if (res.success && res.accessToken) {
          this.setSession(res, request.rememberMe !== false);
        }
      })
    );
  }

  /**
   * Log out current user, invalidate refresh token on backend, and clear local state.
   */
  logout(): void {
    this.http.post(`${this.baseUrl}/logout`, {}, { withCredentials: true }).subscribe({
      next: () => this.finalizeLogout(),
      error: () => this.finalizeLogout()
    });
  }

  /**
   * Silent refresh of access token via HttpOnly refresh cookie.
   */
  refreshToken(): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/refresh`, {}, { withCredentials: true }).pipe(
      tap((res) => {
        if (res.success && res.accessToken) {
          this.accessToken.set(res.accessToken);
          this.storeToken(res.accessToken);
          if (res.userId && res.email) {
            const user: User = {
              id: res.userId,
              email: res.email,
              fullName: res.fullName || '',
              role: res.role || 'ROLE_USER',
              roles: [res.role || 'ROLE_USER']
            };
            this.currentUser.set(user);
            this.storeUser(user);
          }
        }
      }),
      catchError((err) => {
        this.clearSession();
        return throwError(() => err);
      })
    );
  }

  /**
   * Request password reset instructions (anti-enumeration protected).
   */
  forgotPassword(request: ForgotPasswordRequest): Observable<{ success: boolean; message: string }> {
    return this.http.post<{ success: boolean; message: string }>(`${this.baseUrl}/forgot-password`, request);
  }

  /**
   * Reset password with reset token.
   */
  resetPassword(request: ResetPasswordRequest): Observable<{ success: boolean; message: string }> {
    return this.http.post<{ success: boolean; message: string }>(`${this.baseUrl}/reset-password`, request);
  }

  /**
   * Change password for authenticated user.
   */
  changePassword(request: ChangePasswordRequest): Observable<{ success: boolean; message: string }> {
    return this.http.post<{ success: boolean; message: string }>(`${this.baseUrl}/change-password`, request);
  }

  /**
   * Fetch currently authenticated user profile from GET /api/auth/me.
   */
  fetchCurrentUser(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.baseUrl}/me`).pipe(
      tap((profile) => {
        const user: User = {
          id: profile.id,
          email: profile.email,
          fullName: profile.fullName,
          role: profile.role,
          roles: Array.isArray(profile.roles) ? profile.roles : [profile.role],
          emailVerified: profile.emailVerified,
          accountNonLocked: profile.accountNonLocked,
          createdAt: profile.createdAt
        };
        this.currentUser.set(user);
        this.storeUser(user);
      })
    );
  }

  // --- Session Storage Management ---

  private setSession(res: AuthResponse, rememberMe: boolean): void {
    const token = res.accessToken || null;
    const user: User = {
      id: res.userId!,
      email: res.email!,
      fullName: res.fullName || '',
      role: res.role || 'ROLE_USER',
      roles: [res.role || 'ROLE_USER']
    };

    this.accessToken.set(token);
    this.currentUser.set(user);

    if (token) {
      if (rememberMe) {
        localStorage.setItem(TOKEN_KEY, token);
        localStorage.setItem(USER_KEY, JSON.stringify(user));
      } else {
        sessionStorage.setItem(TOKEN_KEY, token);
        sessionStorage.setItem(USER_KEY, JSON.stringify(user));
      }
    }
  }

  private storeToken(token: string): void {
    if (localStorage.getItem(TOKEN_KEY)) {
      localStorage.setItem(TOKEN_KEY, token);
    } else {
      sessionStorage.setItem(TOKEN_KEY, token);
    }
  }

  private storeUser(user: User): void {
    const payload = JSON.stringify(user);
    if (localStorage.getItem(TOKEN_KEY)) {
      localStorage.setItem(USER_KEY, payload);
    } else {
      sessionStorage.setItem(USER_KEY, payload);
    }
  }

  private getStoredToken(): string | null {
    if (typeof window === 'undefined') return null;
    return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY);
  }

  private getStoredUser(): User | null {
    if (typeof window === 'undefined') return null;
    const raw = localStorage.getItem(USER_KEY) || sessionStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch {
      return null;
    }
  }

  private clearSession(): void {
    this.accessToken.set(null);
    this.currentUser.set(null);
    if (typeof window !== 'undefined') {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      sessionStorage.removeItem(TOKEN_KEY);
      sessionStorage.removeItem(USER_KEY);
    }
  }

  private finalizeLogout(): void {
    this.clearSession();
    this.router.navigate(['/login']);
  }
}
