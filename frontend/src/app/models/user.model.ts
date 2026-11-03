export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export interface LoginRequest {
  email: string;
  password: string;
  rememberMe?: boolean;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
  confirmPassword: string;
}

export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface AuthResponse {
  success: boolean;
  message: string;
  userId?: number;
  email?: string;
  fullName?: string;
  role?: string;
  accessToken?: string;
}

export interface User {
  id: number;
  email: string;
  fullName: string;
  role: string;
  roles?: string[];
  enabled?: boolean;
  emailVerified?: boolean;
  accountNonLocked?: boolean;
  createdAt?: string;
}

export interface UserProfile {
  id: number;
  email: string;
  fullName: string;
  role: string;
  roles: string[];
  emailVerified: boolean;
  accountNonLocked: boolean;
  enabled: boolean;
  createdAt?: string;
}
