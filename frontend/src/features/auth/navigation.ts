import type { User } from './types'

// Where each kind of user lands (D73, docs/07-schermate.md)
export const LOGIN_PATH = '/login'
export const SETUP_PATH = '/configurazione'
export const CALENDAR_PATH = '/calendario'

/** The home of a logged user: the guided setup if it is still to do, otherwise the calendar. */
export function homePath(user: User): string {
  return user.onboardingCompleted ? CALENDAR_PATH : SETUP_PATH
}

/**
 * The page a visitor asked for before being sent to the login, read from the state of the redirect.
 * Only a path inside the app is accepted: an address like //other.site would send the user away.
 */
export function requestedPath(state: unknown): string | null {
  if (typeof state !== 'object' || state === null || !('from' in state)) {
    return null
  }
  const from = state.from
  if (typeof from !== 'object' || from === null || !('pathname' in from) || typeof from.pathname !== 'string') {
    return null
  }
  if (!from.pathname.startsWith('/') || from.pathname.startsWith('//')) {
    return null
  }
  const search = 'search' in from && typeof from.search === 'string' ? from.search : ''
  const hash = 'hash' in from && typeof from.hash === 'string' ? from.hash : ''
  return `${from.pathname}${search}${hash}`
}
