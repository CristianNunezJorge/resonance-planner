import { useEffect } from 'react'

const APP_NAME = 'Resonance Planner'

// Every page has its own title in the browser tab, so a screen reader announces where the user is (WCAG 2.4.2)
export function usePageTitle(title: string): void {
  useEffect(() => {
    document.title = `${title} · ${APP_NAME}`
  }, [title])
}
