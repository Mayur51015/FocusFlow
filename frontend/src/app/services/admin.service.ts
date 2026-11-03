import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserProfile } from '../models/user.model';

export interface AdminOverview {
  totalUsers: number;
  activeUsers: number;
  disabledUsers: number;
  lockedUsers: number;
  adminCount: number;
  studentCount: number;
  totalSubjects: number;
  totalStudySessions: number;
  systemStatus: string;
}

export interface TrafficAnalytics {
  totalRequests: number;
  requestsPerSecond: number;
  requestsPerMinute: number;
  avgLatencyMs: number;
  topEndpoints: { endpoint: string; count: number }[];
  statusDistribution: { [key: string]: number };
}

export interface ServiceMetrics {
  uptimeSeconds: number;
  status: string;
  databaseStatus: string;
  heapUsedMb: number;
  heapMaxMb: number;
  heapCommittedMb: number;
  threadCount: number;
  peakThreadCount: number;
  availableProcessors: number;
  connectionPool?: {
    activeConnections?: number;
    idleConnections?: number;
    totalConnections?: number;
    threadsAwaitingConnection?: number;
    maxPoolSize?: number;
    poolName?: string;
  };
  diskFreeGb?: number;
  diskTotalGb?: number;
  micrometerEnabled?: boolean;
  prometheusEndpoint?: string;
}

export interface LoginAttemptRecord {
  id: number;
  email: string;
  ipAddress: string;
  success: boolean;
  failureReason: string | null;
  attemptedAt: string;
}

export interface SecurityOverview {
  recentAttempts: LoginAttemptRecord[];
  failedAttemptCount: number;
  lockedAccountsCount: number;
  bruteForceAlert: boolean;
}

export interface AuditLogRecord {
  id: number;
  userId: number | null;
  actorEmail: string;
  action: string;
  resourceType: string;
  resourceId: string;
  details: string;
  ipAddress: string;
  userAgent: string;
  status: string;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/admin';

  getOverview(): Observable<AdminOverview> {
    return this.http.get<AdminOverview>(`${this.baseUrl}/overview`);
  }

  getUsers(): Observable<UserProfile[]> {
    return this.http.get<UserProfile[]>(`${this.baseUrl}/users`);
  }

  updateUserStatus(id: number, enabled?: boolean, accountNonLocked?: boolean): Observable<any> {
    let params: string[] = [];
    if (enabled !== undefined) params.push(`enabled=${enabled}`);
    if (accountNonLocked !== undefined) params.push(`accountNonLocked=${accountNonLocked}`);
    const query = params.length > 0 ? `?${params.join('&')}` : '';
    return this.http.patch(`${this.baseUrl}/users/${id}/status${query}`, {});
  }

  updateUserRole(id: number, role: string): Observable<any> {
    return this.http.patch(`${this.baseUrl}/users/${id}/role?role=${encodeURIComponent(role)}`, {});
  }

  resetUserPassword(id: number): Observable<{ success: boolean; message: string; temporaryPassword?: string }> {
    return this.http.post<{ success: boolean; message: string; temporaryPassword?: string }>(
      `${this.baseUrl}/users/${id}/reset-password`,
      {}
    );
  }

  getTraffic(): Observable<TrafficAnalytics> {
    return this.http.get<TrafficAnalytics>(`${this.baseUrl}/traffic`);
  }

  getServiceMetrics(): Observable<ServiceMetrics> {
    return this.http.get<ServiceMetrics>(`${this.baseUrl}/service-metrics`);
  }

  getSecurity(): Observable<SecurityOverview> {
    return this.http.get<SecurityOverview>(`${this.baseUrl}/security`);
  }

  getAuditLogs(): Observable<AuditLogRecord[]> {
    return this.http.get<AuditLogRecord[]>(`${this.baseUrl}/audit-logs`);
  }
}

