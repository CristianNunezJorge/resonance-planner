import type { RouteObject } from 'react-router'
import ErrorPage from '../components/ErrorPage'
import NotFoundPage from '../components/NotFoundPage'
import AdminEventsPage from '../features/adminEvents/AdminEventsPage'
import GuestOnly from '../features/auth/GuestOnly'
import LoginPage from '../features/auth/LoginPage'
import ProtectedRoute from '../features/auth/ProtectedRoute'
import RegisterPage from '../features/auth/RegisterPage'
import LazyCalendarPage from '../features/calendar/LazyCalendarPage'
import LandingPage from '../features/landing/LandingPage'
import OnboardingPage from '../features/onboarding/OnboardingPage'
import ProfilePage from '../features/profile/ProfilePage'
import RosterPage from '../features/roster/RosterPage'
import RootLayout from './RootLayout'

// Route list of the browser router (paths in Italian, D71).
// The access rules are in docs/07-schermate.md (D73).
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
          { path: 'calendario', element: <LazyCalendarPage /> },
          { path: 'roster', element: <RosterPage /> },
        ],
      },
      {
        element: <ProtectedRoute />,
        children: [{ path: 'profilo', element: <ProfilePage /> }],
      },
      {
        // Admin only (T27)
        element: <ProtectedRoute role="ADMIN" />,
        children: [{ path: 'admin/eventi', element: <AdminEventsPage /> }],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]
