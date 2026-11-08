import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

interface PasswordStrength {
  score: number;
  label: string;
  color: string;
  percent: number;
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  registerForm: FormGroup = this.fb.group({
    fullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
    confirmPassword: ['', [Validators.required]]
  });

  // State Signals
  showPassword = signal<boolean>(false);
  showConfirmPassword = signal<boolean>(false);
  isLoading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  // Password Strength Calculation (Reactive Signal)
  passwordValue = signal<string>('');
  confirmPasswordValue = signal<string>('');

  constructor() {
    this.registerForm.get('password')?.valueChanges.subscribe(val => {
      this.passwordValue.set(val || '');
    });
    this.registerForm.get('confirmPassword')?.valueChanges.subscribe(val => {
      this.confirmPasswordValue.set(val || '');
    });
  }

  passwordStrength = computed<PasswordStrength>(() => {
    const pwd = this.passwordValue();
    if (!pwd) {
      return { score: 0, label: 'None', color: '#cbd5e1', percent: 0 };
    }

    let score = 0;
    if (pwd.length >= 8) score++;
    if (/[A-Z]/.test(pwd) && /[a-z]/.test(pwd)) score++;
    if (/[0-9]/.test(pwd)) score++;
    if (/[^A-Za-z0-9]/.test(pwd)) score++;

    switch (score) {
      case 1:
        return { score: 1, label: 'Weak', color: '#ef4444', percent: 25 };
      case 2:
        return { score: 2, label: 'Fair', color: '#f59e0b', percent: 50 };
      case 3:
        return { score: 3, label: 'Good', color: '#3b82f6', percent: 75 };
      case 4:
        return { score: 4, label: 'Strong', color: '#10b981', percent: 100 };
      default:
        return { score: 0, label: 'Too Weak', color: '#ef4444', percent: 10 };
    }
  });

  passwordsMatch = computed<boolean>(() => {
    const pwd = this.passwordValue();
    const confirm = this.confirmPasswordValue();
    if (!confirm) return true; // don't flag as mismatch before user types
    return pwd === confirm;
  });

  togglePasswordVisibility(): void {
    this.showPassword.update(v => !v);
  }

  toggleConfirmPasswordVisibility(): void {
    this.showConfirmPassword.update(v => !v);
  }

  onSubmit(): void {
    this.errorMessage.set(null);
    this.successMessage.set(null);

    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      this.errorMessage.set('Please fill out all required fields correctly.');
      return;
    }

    if (!this.passwordsMatch()) {
      this.errorMessage.set('Passwords do not match. Please ensure both fields match.');
      return;
    }

    this.isLoading.set(true);

    const request = {
      fullName: this.registerForm.value.fullName.trim(),
      email: this.registerForm.value.email.trim(),
      password: this.registerForm.value.password,
      confirmPassword: this.registerForm.value.confirmPassword
    };

    this.authService.register(request).subscribe({
      next: (response) => {
        this.isLoading.set(false);
        this.successMessage.set(response.message || 'Registration successful! Your student account has been created.');
        this.registerForm.reset();
      },
      error: (err) => {
        this.isLoading.set(false);
        const serverMsg = err.error?.message || err.message || 'Registration failed. Please check your information and try again.';
        this.errorMessage.set(serverMsg);
      }
    });
  }
}

