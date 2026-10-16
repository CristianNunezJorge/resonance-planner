import { apiRequest } from '../../api/client'
import type { User } from '../auth/types'

// The profile of the user of the token (D44, D102)

/** Only the fields that changed: a missing one stays as it is. */
export interface ProfileChanges {
  username?: string
  playTimeMinutes?: number
}

export function updateProfile(token: string, changes: ProfileChanges): Promise<User> {
  return apiRequest<User>('/api/me', { method: 'PATCH', body: changes, token })
}

/** Deletes the account and all its data (D42); 204 without content. */
export function deleteAccount(token: string): Promise<void> {
  return apiRequest<void>('/api/me', { method: 'DELETE', token })
}
