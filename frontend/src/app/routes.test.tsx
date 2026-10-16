import { act, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/client'
import { getProfile, login as loginRequest, register as registerRequest } from '../features/auth/authApi'
import { expireSession, restoreSession } from '../features/auth/authSlice'
import { readToken, saveToken } from '../features/auth/tokenStorage'
import { getCharacters, getGameRules } from '../features/catalog/catalogApi'
import { postOnboarding } from '../features/onboarding/onboardingApi'
import { catalogCharacters, gameRules } from '../test/fixtures'
import { rover, renderRoutes, signedIn } from '../test/utils'
import { routes } from './routes'

// The landing page and the guided setup load the catalog and the auth pages call the API: no real network in these tests
vi.mock('../features/catalog/catalogApi', () => ({ getCharacters: vi.fn(), getGameRules: vi.fn() }))
vi.mock('../features/onboarding/onboardingApi', () => ({ postOnboarding: vi.fn() }))
vi.mock('../features/auth/authApi', () => ({ login: vi.fn(), register: vi.fn(), getProfile: vi.fn() }))

function typeInto(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } })
}

describe('routes', () => {
  beforeEach(() => {
    vi.mocked(getCharacters).mockResolvedValue([])
    vi.mocked(getGameRules).mockResolvedValue(gameRules)
  })

  afterEach(() => {
    localStorage.clear()
    vi.mocked(getCharacters).mockReset()
    vi.mocked(getGameRules).mockReset()
    vi.mocked(postOnboarding).mockReset()
    vi.mocked(loginRequest).mockReset()
    vi.mocked(registerRequest).mockReset()
    vi.mocked(getProfile).mockReset()
  })

  it('shows the landing page at the root path, inside the frame of the app', async () => {
    renderRoutes(routes, '/')

    expect(await screen.findByRole('heading', { name: 'Resonance Planner' })).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Navigazione principale' })).toBeInTheDocument()
    expect(screen.getByRole('contentinfo')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Vai al contenuto' })).toHaveAttribute('href', '#contenuto')
  })

  it('shows the not found page with a link home for an unknown path', async () => {
    renderRoutes(routes, '/pagina-inesistente')

    expect(await screen.findByRole('heading', { name: 'Pagina non trovata' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Torna alla presentazione' })).toHaveAttribute('href', '/')
  })

  it('sends a logged user from the not found page back to the calendar', async () => {
    renderRoutes(routes, '/pagina-inesistente', signedIn())

    expect(await screen.findByRole('link', { name: 'Torna al calendario' })).toHaveAttribute('href', '/calendario')
  })

  describe('F2 · login', () => {
    it('goes to the page that was asked for before the login, and shows the account in the navbar', async () => {
      vi.mocked(loginRequest).mockResolvedValue({ token: 'fresh.jwt.token', user: rover })
      renderRoutes(routes, '/profilo')
      // A visitor asking for a private page ends up at the login
      expect(await screen.findByRole('heading', { name: 'Accedi' })).toBeInTheDocument()

      typeInto('Email', 'rover@example.com')
      typeInto('Password', 'S3cret-pass')
      fireEvent.click(screen.getByRole('button', { name: 'Accedi' }))

      expect(await screen.findByRole('heading', { name: 'Profilo' })).toBeInTheDocument()
      expect(screen.getByText('rover')).toBeInTheDocument()
      expect(readToken()).toBe('fresh.jwt.token')
      // The page changed: the focus moves to the content, where the old button or field is gone
      await waitFor(() => expect(screen.getByRole('main')).toHaveFocus())
    })

    it('keeps the page that was asked for even if the visitor goes to register instead', async () => {
      vi.mocked(registerRequest).mockResolvedValue({ token: 'new.jwt.token', user: rover })
      renderRoutes(routes, '/profilo')
      expect(await screen.findByRole('heading', { name: 'Accedi' })).toBeInTheDocument()

      // The link inside the page: the navbar has one with the same name
      fireEvent.click(within(screen.getByRole('main')).getByRole('link', { name: 'Registrati' }))
      expect(await screen.findByRole('heading', { name: 'Registrati' })).toBeInTheDocument()
      typeInto('Username', 'rover')
      typeInto('Email', 'rover@example.com')
      typeInto('Password', 'S3cret-pass')
      fireEvent.click(screen.getByRole('button', { name: 'Registrati' }))

      expect(await screen.findByRole('heading', { name: 'Profilo' })).toBeInTheDocument()
    })

    it('goes to the calendar when nothing was asked and the setup is complete', async () => {
      vi.mocked(loginRequest).mockResolvedValue({ token: 't', user: rover })
      renderRoutes(routes, '/login')

      typeInto('Email', 'rover@example.com')
      typeInto('Password', 'S3cret-pass')
      fireEvent.click(screen.getByRole('button', { name: 'Accedi' }))

      expect(await screen.findByRole('heading', { name: 'Calendario' })).toBeInTheDocument()
    })

    it('goes to the setup when it is still to do', async () => {
      vi.mocked(loginRequest).mockResolvedValue({ token: 't', user: { ...rover, onboardingCompleted: false } })
      renderRoutes(routes, '/login')

      typeInto('Email', 'rover@example.com')
      typeInto('Password', 'S3cret-pass')
      fireEvent.click(screen.getByRole('button', { name: 'Accedi' }))

      expect(await screen.findByRole('heading', { name: 'Configurazione guidata' })).toBeInTheDocument()
    })

    it('stays on the login with one message when the credentials are wrong', async () => {
      vi.mocked(loginRequest).mockRejectedValue(new ApiError('BAD_CREDENTIALS', 'Email o password non corrette.', 401))
      renderRoutes(routes, '/login')

      typeInto('Email', 'rover@example.com')
      typeInto('Password', 'wrong-password')
      fireEvent.click(screen.getByRole('button', { name: 'Accedi' }))

      expect(await screen.findByRole('alert')).toHaveTextContent('Email o password non corrette.')
      expect(screen.getByRole('heading', { name: 'Accedi' })).toBeInTheDocument()
      expect(readToken()).toBeNull()
    })
  })

  describe('F1 · registration', () => {
    it('logs in the new user and brings them to the guided setup', async () => {
      vi.mocked(registerRequest).mockResolvedValue({ token: 'new.jwt.token', user: { ...rover, onboardingCompleted: false } })
      renderRoutes(routes, '/registrati')

      typeInto('Username', 'rover')
      typeInto('Email', 'rover@example.com')
      typeInto('Password', 'S3cret-pass')
      fireEvent.click(screen.getByRole('button', { name: 'Registrati' }))

      expect(await screen.findByRole('heading', { name: 'Configurazione guidata' })).toBeInTheDocument()
      expect(readToken()).toBe('new.jwt.token')
      // From the setup, the pages that need it complete send the user back to it
      expect(screen.getByRole('link', { name: 'Configurazione' })).toBeInTheDocument()
    })
  })

  describe('F3 · guided setup', () => {
    it('goes from the three steps to the calendar, with a notification', async () => {
      vi.mocked(getCharacters).mockResolvedValue(catalogCharacters)
      vi.mocked(postOnboarding).mockResolvedValue({ ...rover, onboardingCompleted: true, playTimeMinutes: 60 })
      renderRoutes(routes, '/configurazione', signedIn({ onboardingCompleted: false }))

      // Step 1: the characters
      expect(await screen.findByRole('heading', { name: /Passo 1 di 3/ })).toBeInTheDocument()
      fireEvent.click(screen.getByRole('checkbox', { name: /Jinhsi/ }))
      fireEvent.click(screen.getByRole('button', { name: 'Avanti' }))
      // Step 2: levels and goals
      expect(await screen.findByRole('heading', { name: /Passo 2 di 3/ })).toBeInTheDocument()
      fireEvent.click(screen.getByRole('button', { name: 'Avanti' }))
      // Step 3: Waveplate and play time
      expect(await screen.findByRole('heading', { name: /Passo 3 di 3/ })).toBeInTheDocument()
      typeInto('Waveplate attuali', '180')
      fireEvent.click(screen.getByRole('radio', { name: '1 ora' }))
      fireEvent.click(screen.getByRole('button', { name: 'Completa la configurazione' }))

      // The setup is complete: the page of the setup is not for this user anymore, the calendar is
      expect(await screen.findByRole('heading', { name: 'Calendario' })).toBeInTheDocument()
      expect(screen.getByText('Configurazione completata.')).toBeInTheDocument()
      expect(screen.queryByRole('heading', { name: 'Configurazione guidata' })).not.toBeInTheDocument()
    })

    it('sends someone who has finished the setup away from it, to the calendar', async () => {
      renderRoutes(routes, '/configurazione', signedIn())

      expect(await screen.findByRole('heading', { name: 'Calendario' })).toBeInTheDocument()
    })

    it('sends someone who has not finished it back to it from the calendar', async () => {
      renderRoutes(routes, '/calendario', signedIn({ onboardingCompleted: false }))

      expect(await screen.findByRole('heading', { name: 'Configurazione guidata' })).toBeInTheDocument()
    })
  })

  describe('F11 · logout', () => {
    it('goes back to the presentation and forgets the token', async () => {
      saveToken('test.jwt.token')
      renderRoutes(routes, '/profilo', signedIn())
      expect(await screen.findByRole('heading', { name: 'Profilo' })).toBeInTheDocument()

      // The button of the navigation bar: the profile page has one of its own too
      const navigation = screen.getByRole('navigation', { name: 'Navigazione principale' })
      fireEvent.click(within(navigation).getByRole('button', { name: 'Esci' }))

      expect(await screen.findByRole('heading', { name: 'Resonance Planner' })).toBeInTheDocument()
      expect(screen.getByRole('link', { name: 'Accedi' })).toBeInTheDocument()
      expect(readToken()).toBeNull()
      // The title of the tab is the one of the presentation, not the one of the page that was left
      expect(document.title).toBe('Presentazione · Resonance Planner')
      expect(await screen.findByText('Sei uscito dall\'account.')).toBeInTheDocument()
    })
  })

  describe('saved session', () => {
    it('is restored at the start, without passing through the login', async () => {
      saveToken('saved.jwt.token')
      vi.mocked(getProfile).mockResolvedValue(rover)
      // Like main.tsx: the saved token is checked before the first render
      renderRoutes(routes, '/profilo', undefined, (store) => {
        void store.dispatch(restoreSession())
      })

      // While the server answers there is a loading state, and the login is never shown
      expect(within(screen.getByRole('main')).getByRole('status')).toHaveTextContent('Caricamento')
      expect(screen.queryByRole('heading', { name: 'Accedi' })).not.toBeInTheDocument()
      expect(await screen.findByRole('heading', { name: 'Profilo' })).toBeInTheDocument()
      expect(getProfile).toHaveBeenCalledWith('saved.jwt.token')
      expect(screen.queryByRole('heading', { name: 'Accedi' })).not.toBeInTheDocument()
    })

    it('goes to the login, with a notice, when the server refuses the token while the user is working', async () => {
      saveToken('test.jwt.token')
      const { store } = renderRoutes(routes, '/profilo', signedIn())
      expect(await screen.findByRole('heading', { name: 'Profilo' })).toBeInTheDocument()

      act(() => {
        store.dispatch(expireSession('test.jwt.token'))
      })

      await waitFor(() => expect(screen.getByRole('heading', { name: 'Accedi' })).toBeInTheDocument())
      expect(screen.getByText('La sessione è scaduta: accedi di nuovo.')).toBeInTheDocument()
      expect(readToken()).toBeNull()
    })

    it('brings the user back to the page they were on after the new login', async () => {
      vi.mocked(loginRequest).mockResolvedValue({ token: 'again.jwt.token', user: rover })
      const { store } = renderRoutes(routes, '/profilo', signedIn())
      expect(await screen.findByRole('heading', { name: 'Profilo' })).toBeInTheDocument()
      act(() => {
        store.dispatch(expireSession('test.jwt.token'))
      })
      await screen.findByRole('heading', { name: 'Accedi' })

      typeInto('Email', 'rover@example.com')
      typeInto('Password', 'S3cret-pass')
      fireEvent.click(screen.getByRole('button', { name: 'Accedi' }))

      expect(await screen.findByRole('heading', { name: 'Profilo' })).toBeInTheDocument()
    })
  })
})
