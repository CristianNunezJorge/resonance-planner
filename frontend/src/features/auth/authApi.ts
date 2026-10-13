import { apiRequest } from '../../api/client'
import type { AuthResponse, LoginRequest, RegisterRequest, User } from './types'

// Login and registration need no token; the profile does (D44)
export function login(credentials: LoginRequest): Promise<AuthResponse> {
  return apiRequest<AuthResponse>('/api/auth/login', { method: 'POST', body: credentials })
}

export function register(data: RegisterRequest): Promise<AuthResponse> {
  return apiRequest<AuthResponse>('/api/auth/register', { method: 'POST', body: data })
}

export function getProfile(token: string, signal?: AbortSignal): Promise<User> {
  return apiRequest<User>('/api/me', { token, signal })
}
