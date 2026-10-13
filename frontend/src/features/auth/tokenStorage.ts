// The token lives in the localStorage (D45), so the session survives a reload of the page.
// Every access is protected: a private window or blocked site data can throw, and then the session
// simply does not survive the reload.
export const TOKEN_KEY = 'resonance-planner.token'

// A JWT is three parts of letters, digits, "-" and "_" separated by dots, and a few hundred characters long.
// Anything else in the storage is not ours: it is dropped, so it is never sent to the server as a header.
const TOKEN_FORMAT = /^[\w-]+\.[\w-]+\.[\w-]+$/
const MAX_TOKEN_LENGTH = 2048

export function readToken(): string | null {
  try {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token === null) {
      return null
    }
    if (token.length <= MAX_TOKEN_LENGTH && TOKEN_FORMAT.test(token)) {
      return token
    }
    localStorage.removeItem(TOKEN_KEY)
    return null
  } catch {
    return null
  }
}

export function saveToken(token: string): void {
  try {
    localStorage.setItem(TOKEN_KEY, token)
  } catch {
    // Nothing to do: the token stays in memory for this visit
  }
}

/**
 * Removes the token. With `onlyIfEquals` it removes it only if it is still that one: another tab may have
 * logged in again in the meantime, and its new token must not be thrown away by an old one that failed.
 */
export function clearToken(onlyIfEquals?: string): void {
  try {
    if (onlyIfEquals !== undefined && localStorage.getItem(TOKEN_KEY) !== onlyIfEquals) {
      return
    }
    localStorage.removeItem(TOKEN_KEY)
  } catch {
    // Nothing to remove
  }
}
