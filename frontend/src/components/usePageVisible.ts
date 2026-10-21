import { useSyncExternalStore } from 'react'

function subscribe(onChange: () => void) {
  document.addEventListener('visibilitychange', onChange)
  return () => document.removeEventListener('visibilitychange', onChange)
}

/** True while the tab is on the screen; it changes when the user switches tab or minimizes the window. */
export function usePageVisible(): boolean {
  return useSyncExternalStore(subscribe, () => document.visibilityState === 'visible', () => true)
}
