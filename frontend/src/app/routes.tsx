import type { RouteObject } from 'react-router'
import ComingSoonPage from '../components/ComingSoonPage'
import ErrorPage from '../components/ErrorPage'
import NotFoundPage from '../components/NotFoundPage'
import GuestOnly from '../features/auth/GuestOnly'
import LoginPage from '../features/auth/LoginPage'
import ProtectedRoute from '../features/auth/ProtectedRoute'
import RegisterPage from '../features/auth/RegisterPage'
import CalendarPage from '../features/calendar/CalendarPage'
import LandingPage from '../features/landing/LandingPage'
import OnboardingPage from '../features/onboarding/OnboardingPage'
import RosterPage from '../features/roster/RosterPage'
import RootLayout from './RootLayout'

// Route list shared by the browser router and the tests (paths in Italian, D71).
// The access rules are in docs/07-schermate.md (D73); the pages marked ComingSoonPage arrive with later cards.
export const routes: RouteObject[] = [
  {
    path: '/',
    element: <RootLayout />,
    errorElement: <ErrorPage />,
    children: [
      { index: true, element: <LandingPage /> },
      {
        // Visitors only: someone already logged in goes to the calendar or to the setup
        element: <GuestOnly />,
        children: [
          { path: 'login', element: <LoginPage /> },
          { path: 'registrati', element: <RegisterPage /> },
        ],
      },
      {
        // Only for who still has the guided setup to do
        element: <ProtectedRoute onboarding="incomplete" />,
        children: [{ path: 'configurazione', element: <OnboardingPage /> }],
      },
      {
        // Only after the guided setup (T26 for the calendar)
        element: <ProtectedRoute onboarding="complete" />,
        children: [
          { path: 'calendario', element: <CalendarPage /> },
          { path: 'roster', element: <RosterPage /> },
        ],
      },
      {
        element: <ProtectedRoute />,
        children: [{ path: 'profilo', element: <ComingSoonPage title="Profilo" /> }],
      },
      {
        // Admin only (T27)
        element: <ProtectedRoute role="ADMIN" />,
        children: [{ path: 'admin/eventi', element: <ComingSoonPage title="Gestione eventi" /> }],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]
