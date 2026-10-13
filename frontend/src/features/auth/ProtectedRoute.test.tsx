import { act, fireEvent, screen } from '@testing-library/react'
import type { RouteObject } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { renderRoutes, rover, signedIn } from '../../test/utils'
import { getProfile } from './authApi'
import { login } from './authSlice'
import GuestOnly from './GuestOnly'
import ProtectedRoute from './ProtectedRoute'
import { saveToken } from './tokenStorage'

vi.mock('./authApi', () => ({ login: vi.fn(), register: vi.fn(), getProfile: vi.fn() }))

// Pages that only show their name, to see where a rule sends the user (D73)
const page = (name: string) => <h1>{name}</h1>

const routes: RouteObject[] = [
  { path: '/', element: page('Presentazione') },
  { element: <GuestOnly />, children: [{ path: '/login', element: page('Accedi') }] },
  { element: <ProtectedRoute onboarding="incomplete" />, children: [{ path: '/configurazione', element: page('Configurazione') }] },
  { element: <ProtectedRoute onboarding="complete" />, children: [{ path: '/calendario', element: page('Calendario') }] },
  { element: <ProtectedRoute />, children: [{ path: '/profilo', element: page('Profilo') }] },
  { element: <ProtectedRoute role="ADMIN" />, children: [{ path: '/admin/eventi', element: page('Gestione eventi') }] },
]

describe('ProtectedRoute', () => {
  afterEach(() => {
    localStorage.clear()
    vi.mocked(getProfile).mockReset()
  })

  it('sends a visitor to the login and remembers the page that was asked for', () => {
    const { router } = renderRoutes(routes, '/calendario?week=2')

    expect(screen.getByRole('heading', { name: 'Accedi' })).toBeInTheDocument()
    expect(router.state.location.state).toMatchObject({ from: { pathname: '/calendario', search: '?week=2' } })
  })

  it('shows the page to a user who may see it', () => {
    renderRoutes(routes, '/calendario', signedIn())

    expect(screen.getByRole('heading', { name: 'Calendario' })).toBeInTheDocument()
  })

  it('sends a user with the guided setup still to do to the setup, from the pages that need it complete', () => {
    renderRoutes(routes, '/calendario', signedIn({ onboardingCompleted: false }))

    expect(screen.getByRole('heading', { name: 'Configurazione' })).toBeInTheDocument()
  })

  it('sends a user with the setup complete away from the setup, to the calendar', () => {
    renderRoutes(routes, '/configurazione', signedIn({ onboardingCompleted: true }))

    expect(screen.getByRole('heading', { name: 'Calendario' })).toBeInTheDocument()
  })

  it('lets every logged user see the profile, whatever the state of the setup', () => {
    renderRoutes(routes, '/profilo', signedIn({ onboardingCompleted: false }))

    expect(screen.getByRole('heading', { name: 'Profilo' })).toBeInTheDocument()
  })

  it('lets the admin into the events management even before the guided setup', () => {
    renderRoutes(routes, '/admin/eventi', signedIn({ role: 'ADMIN', onboardingCompleted: false }))

    expect(screen.getByRole('heading', { name: 'Gestione eventi' })).toBeInTheDocument()
  })

  it('sends the others away from the admin area with a notice', () => {
    const { store } = renderRoutes(routes, '/admin/eventi', signedIn({ role: 'USER' }))

    expect(screen.getByRole('heading', { name: 'Calendario' })).toBeInTheDocument()
    expect(store.getState().toast.items).toEqual([expect.objectContaining({ message: 'Area riservata.', variant: 'danger' })])
  })

  it('waits while the saved session is being restored, without sending the user to the login', () => {
    renderRoutes(routes, '/calendario', {
      auth: { token: 'saved.jwt.token', user: null, sessionStatus: 'restoring', sessionError: null },
    })

    expect(screen.getByRole('status')).toHaveTextContent('Caricamento')
    expect(screen.queryByRole('heading', { name: 'Accedi' })).not.toBeInTheDocument()
  })

  it('shows the problem and a way to try again when the saved session could not be restored', async () => {
    saveToken('saved.jwt.token')
    vi.mocked(getProfile).mockResolvedValue({
      id: 7, username: 'rover', email: 'rover@example.com', role: 'USER', playTimeMinutes: 60, onboardingCompleted: true,
    })
    renderRoutes(routes, '/calendario', {
      auth: { token: 'saved.jwt.token', user: null, sessionStatus: 'failed', sessionError: 'Impossibile contattare il server.' },
    })

    expect(screen.getByRole('alert')).toHaveTextContent('Impossibile contattare il server.')
    fireEvent.click(screen.getByRole('button', { name: 'Riprova' }))

    expect(await screen.findByRole('heading', { name: 'Calendario' })).toBeInTheDocument()
  })
})

describe('retry after a failed restore', () => {
  afterEach(() => {
    localStorage.clear()
  })

  it('goes to the login when the token disappeared meanwhile, for example after a logout in another tab', () => {
    renderRoutes(routes, '/calendario', {
      auth: { token: null, user: null, sessionStatus: 'failed', sessionError: 'Impossibile contattare il server.' },
    })

    fireEvent.click(screen.getByRole('button', { name: 'Riprova' }))

    expect(screen.getByRole('heading', { name: 'Accedi' })).toBeInTheDocument()
  })
})

describe('GuestOnly', () => {
  it('waits while the saved session is being restored, instead of showing the login', () => {
    renderRoutes(routes, '/login', {
      auth: { token: 'saved.jwt.token', user: null, sessionStatus: 'restoring', sessionError: null },
    })

    expect(screen.getByRole('status')).toHaveTextContent('Caricamento')
    expect(screen.queryByRole('heading', { name: 'Accedi' })).not.toBeInTheDocument()
  })

  it('shows the login to a visitor', () => {
    renderRoutes(routes, '/login')

    expect(screen.getByRole('heading', { name: 'Accedi' })).toBeInTheDocument()
  })

  it('sends a logged user with the setup complete to the calendar', () => {
    renderRoutes(routes, '/login', signedIn({ onboardingCompleted: true }))
    expect(screen.getByRole('heading', { name: 'Calendario' })).toBeInTheDocument()
  })

  it('sends a logged user with the setup to do to the setup', () => {
    renderRoutes(routes, '/login', signedIn({ onboardingCompleted: false }))

    expect(screen.getByRole('heading', { name: 'Configurazione' })).toBeInTheDocument()
  })

  it('brings a user who has just logged in back to the page that was asked for', async () => {
    const { router, store } = renderRoutes(routes, '/profilo')
    expect(screen.getByRole('heading', { name: 'Accedi' })).toBeInTheDocument()

    act(() => {
      store.dispatch(login.fulfilled({ token: 't', user: rover }, 'request-id', { email: 'x', password: 'y' }))
    })

    expect(await screen.findByRole('heading', { name: 'Profilo' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/profilo')
  })
})
