import { useSyncExternalStore } from 'react'

// Below the "md" breakpoint of Bootstrap the calendar becomes a list by day (D66, D67)
const PHONE_QUERY = '(max-width: 767.98px)'

function mediaQuery(): MediaQueryList | null {
  return typeof window.matchMedia === 'function' ? window.matchMedia(PHONE_QUERY) : null
}

function subscribe(onChange: () => void): () => void {
  const query = mediaQuery()
  query?.addEventListener('change', onChange)
  return () => query?.removeEventListener('change', onChange)
}

/** True on a narrow screen; it follows the window when it is resized or the phone is turned. */
export function useIsPhone(): boolean {
  return useSyncExternalStore(subscribe, () => mediaQuery()?.matches ?? false, () => false)
}
