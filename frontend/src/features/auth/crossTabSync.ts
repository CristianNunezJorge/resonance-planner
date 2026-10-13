import type { AppDispatch } from '../../app/store'
import { restoreSession, sessionEnded } from './authSlice'
import { readToken, TOKEN_KEY } from './tokenStorage'

/**
 * Keeps all the open tabs of the app in the same session. The browser tells a tab when ANOTHER tab changes the
 * localStorage: if the token is gone (logout) this tab logs out too; if there is a new one (login) this tab reads
 * who it belongs to. Returns the function that stops listening.
 */
export function syncSessionAcrossTabs(dispatch: AppDispatch): () => void {
  const onStorage = (event: StorageEvent) => {
    // key null: the whole storage was cleared
    if (event.key !== null && event.key !== TOKEN_KEY) {
      return
    }
    if (readToken() === null) {
      dispatch(sessionEnded())
    } else {
      void dispatch(restoreSession())
    }
  }
  window.addEventListener('storage', onStorage)
  return () => window.removeEventListener('storage', onStorage)
}
