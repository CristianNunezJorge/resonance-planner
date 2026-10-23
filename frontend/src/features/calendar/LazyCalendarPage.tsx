import { lazy, Suspense } from 'react'
import LoadingState from '../../components/LoadingState'
import { usePageTitle } from '../../components/usePageTitle'

// The calendar page brings FullCalendar, the largest library of the app: it is downloaded only when the calendar
// opens, not with the presentation page or the login (D137)
const CalendarPage = lazy(() => import('./CalendarPage'))

function LazyCalendarPage() {
  // The tab already says where the user is while the page downloads (WCAG 2.4.2)
  usePageTitle('Calendario')

  return (
    <Suspense fallback={<LoadingState />}>
      <CalendarPage />
    </Suspense>
  )
}

export default LazyCalendarPage
