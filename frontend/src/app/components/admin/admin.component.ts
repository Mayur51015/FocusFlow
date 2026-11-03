import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  AdminService,
  AdminOverview,
  TrafficAnalytics,
  ServiceMetrics,
  SecurityOverview,
  AuditLogRecord
} from '../../services/admin.service';
import { UserProfile } from '../../models/user.model';

type AdminTab = 'overview' | 'users' | 'traffic' | 'services' | 'security' | 'audit';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.css'
})
export class AdminComponent implements OnInit, OnDestroy {
  private adminService = inject(AdminService);

  activeTab = signal<AdminTab>('overview');
  loading = signal<boolean>(false);
  autoRefresh = signal<boolean>(false);
  private refreshTimer: any = null;

  overview = signal<AdminOverview | null>(null);
  users = signal<UserProfile[]>([]);
  traffic = signal<TrafficAnalytics | null>(null);
  serviceMetrics = signal<ServiceMetrics | null>(null);
  security = signal<SecurityOverview | null>(null);
  auditLogs = signal<AuditLogRecord[]>([]);

  // Search & Filters
  userSearch = signal<string>('');
  userRoleFilter = signal<string>('ALL');
  auditFilter = signal<string>('');

  // Alerts & Notifications
  actionMessage = signal<{ type: 'success' | 'error'; text: string } | null>(null);
  tempPasswordModal = signal<{ email: string; tempPass: string } | null>(null);

  // Filtered Users computed signal
  filteredUsers = computed(() => {
    const list = this.users();
    const query = this.userSearch().toLowerCase().trim();
    const role = this.userRoleFilter();

    return list.filter((u) => {
      const matchesQuery = !query ||
        u.email.toLowerCase().includes(query) ||
        u.fullName.toLowerCase().includes(query) ||
        String(u.id).includes(query);

      const matchesRole = role === 'ALL' || u.role === role || (u.roles && u.roles.includes(role));

      return matchesQuery && matchesRole;
    });
  });

  // Filtered Audit Logs computed signal
  filteredAuditLogs = computed(() => {
    const logs = this.auditLogs();
    const query = this.auditFilter().toLowerCase().trim();
    if (!query) return logs;

    return logs.filter((log) =>
      log.action.toLowerCase().includes(query) ||
      (log.actorEmail && log.actorEmail.toLowerCase().includes(query)) ||
      (log.details && log.details.toLowerCase().includes(query)) ||
      (log.resourceType && log.resourceType.toLowerCase().includes(query))
    );
  });

  ngOnInit(): void {
    this.loadAll();
  }

  ngOnDestroy(): void {
    this.stopAutoRefresh();
  }

  setTab(tab: AdminTab): void {
    this.activeTab.set(tab);
    this.refreshCurrentTab();
  }

  toggleAutoRefresh(): void {
    const nextState = !this.autoRefresh();
    this.autoRefresh.set(nextState);
    if (nextState) {
      this.refreshTimer = setInterval(() => this.refreshCurrentTab(), 8000);
    } else {
      this.stopAutoRefresh();
    }
  }

  private stopAutoRefresh(): void {
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer);
      this.refreshTimer = null;
    }
  }

  loadAll(): void {
    this.loading.set(true);
    this.adminService.getOverview().subscribe({
      next: (data) => this.overview.set(data),
      error: () => {}
    });
    this.adminService.getUsers().subscribe({
      next: (data) => this.users.set(data),
      error: () => {}
    });
    this.adminService.getTraffic().subscribe({
      next: (data) => this.traffic.set(data),
      error: () => {}
    });
    this.adminService.getServiceMetrics().subscribe({
      next: (data) => this.serviceMetrics.set(data),
      error: () => {}
    });
    this.adminService.getSecurity().subscribe({
      next: (data) => this.security.set(data),
      error: () => {}
    });
    this.adminService.getAuditLogs().subscribe({
      next: (data) => {
        this.auditLogs.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  refreshCurrentTab(): void {
    switch (this.activeTab()) {
      case 'overview':
        this.adminService.getOverview().subscribe({ next: (d) => this.overview.set(d) });
        break;
      case 'users':
        this.adminService.getUsers().subscribe({ next: (d) => this.users.set(d) });
        break;
      case 'traffic':
        this.adminService.getTraffic().subscribe({ next: (d) => this.traffic.set(d) });
        break;
      case 'services':
        this.adminService.getServiceMetrics().subscribe({ next: (d) => this.serviceMetrics.set(d) });
        break;
      case 'security':
        this.adminService.getSecurity().subscribe({ next: (d) => this.security.set(d) });
        break;
      case 'audit':
        this.adminService.getAuditLogs().subscribe({ next: (d) => this.auditLogs.set(d) });
        break;
    }
  }

  // --- User Management Actions ---

  toggleStatus(user: UserProfile, field: 'enabled' | 'lock'): void {
    const isEnabling = field === 'enabled' ? !user.enabled : undefined;
    const isLocking = field === 'lock' ? !user.accountNonLocked : undefined;

    this.adminService.updateUserStatus(user.id, isEnabling, isLocking).subscribe({
      next: (res) => {
        this.showMessage('success', res.message || 'User status updated successfully.');
        this.refreshCurrentTab();
      },
      error: (err) => {
        this.showMessage('error', err.error?.message || 'Failed to update user status.');
      }
    });
  }

  changeRole(user: UserProfile, newRole: string): void {
    if (user.role === newRole) return;
    this.adminService.updateUserRole(user.id, newRole).subscribe({
      next: (res) => {
        this.showMessage('success', `User role successfully updated to ${newRole}.`);
        this.refreshCurrentTab();
      },
      error: (err) => {
        this.showMessage('error', err.error?.message || 'Failed to update user role.');
      }
    });
  }

  resetPassword(user: UserProfile): void {
    if (!confirm(`Are you sure you want to administratively reset the password for ${user.email}?`)) {
      return;
    }

    this.adminService.resetUserPassword(user.id).subscribe({
      next: (res) => {
        if (res.temporaryPassword) {
          this.tempPasswordModal.set({
            email: user.email,
            tempPass: res.temporaryPassword
          });
        }
        this.showMessage('success', 'Temporary password generated successfully.');
        this.refreshCurrentTab();
      },
      error: (err) => {
        this.showMessage('error', err.error?.message || 'Failed to reset password.');
      }
    });
  }

  closeModal(): void {
    this.tempPasswordModal.set(null);
  }

  // --- Export Audit Logs to CSV ---

  exportAuditCsv(): void {
    const logs = this.filteredAuditLogs();
    if (logs.length === 0) {
      alert('No audit logs available to export.');
      return;
    }

    const headers = ['ID', 'Timestamp', 'Actor', 'Action', 'Resource', 'Resource ID', 'Details', 'IP Address'];
    const rows = logs.map(l => [
      l.id,
      `"${l.createdAt || ''}"`,
      `"${l.actorEmail || ''}"`,
      `"${l.action || ''}"`,
      `"${l.resourceType || ''}"`,
      `"${l.resourceId || ''}"`,
      `"${(l.details || '').replace(/"/g, '""')}"`,
      `"${l.ipAddress || ''}"`
    ]);

    const csvContent = 'data:text/csv;charset=utf-8,' +
      [headers.join(','), ...rows.map(e => e.join(','))].join('\n');

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `focusflow_audit_logs_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  formatUptime(seconds?: number): string {
    if (!seconds && seconds !== 0) return 'N/A';
    const d = Math.floor(seconds / 86400);
    const h = Math.floor((seconds % 86400) / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    if (d > 0) return `${d}d ${h}h ${m}m`;
    if (h > 0) return `${h}h ${m}m ${s}s`;
    return `${m}m ${s}s`;
  }

  private showMessage(type: 'success' | 'error', text: string): void {
    this.actionMessage.set({ type, text });
    setTimeout(() => this.actionMessage.set(null), 5000);
  }
}

