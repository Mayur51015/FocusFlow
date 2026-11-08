import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

/**
 * Cross-field validator ensuring password confirmation matches.
 */
export const passwordMatchValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const newPassword = control.get('newPassword');
  const confirmPassword = control.get('confirmPassword');
  if (!newPassword || !confirmPassword) return null;
  return newPassword.value === confirmPassword.value ? null : { passwordMismatch: true };
};

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.css'
})
export class ResetPasswordComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  resetToken = signal<string | null>(null);
  loading = signal<boolean>(false);
  successMessage = signal<string | null>(null);
  errorMessage = signal<string | null>(null);
  showPassword = signal<boolean>(false);
  showConfirmPassword = signal<boolean>(false);

  resetForm: FormGroup = this.fb.group({
    newPassword: ['', [
      Validators.required,
      Validators.minLength(8),
      Validators.pattern(/^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!._-]).*$/)
    ]],
    confirmPassword: ['', [Validators.required]]
  }, { validators: passwordMatchValidator });

  ngOnInit(): void {
    const token = this.route.snapshot.queryParams['token'];
    if (token) {
      this.resetToken.set(token);
    } else {
      this.errorMessage.set('Password reset token is missing or invalid. Please request a new recovery link.');
    }
  }

  togglePassword(): void {
    this.showPassword.update((v) => !v);
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword.update((v) => !v);
  }

  getPasswordStrength(): { level: number; text: string; color: string } {
    const pwd = this.resetForm.get('newPassword')?.value || '';
    if (!pwd) return { level: 0, text: '', color: '#e2e8f0' };

    let score = 0;
    if (pwd.length >= 8) score++;
    if (/[A-Z]/.test(pwd)) score++;
    if (/[0-9]/.test(pwd)) score++;
    if (/[^A-Za-z0-9]/.test(pwd)) score++;

    switch (score) {
      case 1:
        return { level: 25, text: 'Weak', color: '#ef4444' };
      case 2:
        return { level: 50, text: 'Fair', color: '#f59e0b' };
      case 3:
        return { level: 75, text: 'Good', color: '#3b82f6' };
      case 4:
        return { level: 100, text: 'Strong', color: '#10b981' };
      default:
        return { level: 10, text: 'Too short', color: '#ef4444' };
    }
  }

  onSubmit(): void {
    if (!this.resetToken()) {
      this.errorMessage.set('Cannot reset password without a valid reset token.');
      return;
    }

    if (this.resetForm.invalid) {
      this.resetForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const { newPassword, confirmPassword } = this.resetForm.value;

    this.authService.resetPassword({
      token: this.resetToken()!,
      newPassword,
      confirmPassword
    }).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.successMessage.set(res.message || 'Your password has been reset successfully! You can now log in.');
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Password reset failed. The reset token may be expired or invalid.');
      }
    });
  }
}

