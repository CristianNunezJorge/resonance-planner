import { apiRequest } from '../../api/client'
import type { User } from '../auth/types'
import type { OnboardingRequest } from './types'

// Saves the three steps together and answers with the updated user (D50, D109)
export function postOnboarding(token: string, request: OnboardingRequest): Promise<User> {
  return apiRequest<User>('/api/me/onboarding', { method: 'POST', body: request, token })
}
