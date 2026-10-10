import type { RouteObject } from 'react-router'
import ErrorPage from '../components/ErrorPage'
import NotFoundPage from '../components/NotFoundPage'
import LandingPage from '../features/landing/LandingPage'
import RootLayout from './RootLayout'

// Route list shared by the browser router and the tests (paths in Italian, D71)
export const routes: RouteObject[] = [
  {
    path: '/',
    element: <RootLayout />,
    errorElement: <ErrorPage />,
    children: [
      { index: true, element: <LandingPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]
