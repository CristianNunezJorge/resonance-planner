// Shapes of the authentication API (docs/05-api.md)
export type Role = 'USER' | 'ADMIN' | 'GUEST'

export interface User {
  id: number
  username: string
  email: string
  role: Role
  playTimeMinutes: number | null
  onboardingCompleted: boolean
}

export interface AuthResponse {
  token: string
  user: User
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
}
