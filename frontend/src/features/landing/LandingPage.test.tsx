import { fireEvent, render, screen, within } from '@testing-library/react'
import { StrictMode } from 'react'
import { Provider } from 'react-redux'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client'
import { setupStore, type RootState } from '../../app/store'
import { signedIn } from '../../test/utils'
import { getCharacters } from '../catalog/catalogApi'
import LandingPage from './LandingPage'

vi.mock('../catalog/catalogApi', () => ({ getCharacters: vi.fn() }))

const jinhsi = {
  id: 'jinhsi',
  name: 'Jinhsi',
  element: 'SPECTRO' as const,
  weaponType: 'BROADBLADE' as const,
  rarity: 5,
  role: 'Main Damage Dealer',
  icon: 'characters/jinhsi.webp',
}

function renderPage(preloadedState?: Partial<RootState>, strict = false) {
  const page = (
    <Provider store={setupStore(preloadedState)}>
      <MemoryRouter>
        <LandingPage />
      </MemoryRouter>
    </Provider>
  )
  render(strict ? <StrictMode>{page}</StrictMode> : page)
}

describe('LandingPage', () => {
  afterEach(() => {
    vi.mocked(getCharacters).mockReset()
  })

  it('explains the app in a sentence and in three steps', () => {
    vi.mocked(getCharacters).mockResolvedValue([])

    renderPage()

    expect(screen.getByRole('heading', { level: 1, name: 'Resonance Planner' })).toBeInTheDocument()
    const steps = within(screen.getByRole('list', { name: 'Come funziona' })).getAllByRole('listitem')
    expect(steps).toHaveLength(3)
  })

  it('sends a visitor to the registration with "Prova la demo"', () => {
    vi.mocked(getCharacters).mockResolvedValue([])

    renderPage()

    expect(screen.getByRole('link', { name: 'Prova la demo' })).toHaveAttribute('href', '/registrati')
    expect(screen.queryByRole('link', { name: 'Vai al calendario' })).not.toBeInTheDocument()
  })

  it('sends a logged user to the calendar instead of the registration', () => {
    vi.mocked(getCharacters).mockResolvedValue([])

    renderPage(signedIn())

    expect(screen.getByRole('link', { name: 'Vai al calendario' })).toHaveAttribute('href', '/calendario')
    expect(screen.queryByRole('link', { name: 'Prova la demo' })).not.toBeInTheDocument()
  })

  it('sends a logged user with the setup still to do to the guided setup', () => {
    vi.mocked(getCharacters).mockResolvedValue([])

    renderPage(signedIn({ onboardingCompleted: false }))

    expect(screen.getByRole('link', { name: 'Continua la configurazione' })).toHaveAttribute('href', '/configurazione')
  })

  it('shows no button while the saved session is being checked, so it does not flash the wrong one', () => {
    vi.mocked(getCharacters).mockResolvedValue([])

    renderPage({ auth: { token: 'saved.jwt.token', user: null, sessionStatus: 'restoring', sessionError: null } })

    // The NavBar is not mounted here: any link on the page would be a button of the presentation
    expect(screen.queryByRole('link')).not.toBeInTheDocument()
  })

  it('treats a visitor the same when the saved session could not be restored, so the page is never empty', () => {
    vi.mocked(getCharacters).mockResolvedValue([])

    renderPage({
      auth: { token: 'saved.jwt.token', user: null, sessionStatus: 'failed', sessionError: 'Server non raggiungibile.' },
    })

    expect(screen.getByRole('link', { name: 'Prova la demo' })).toHaveAttribute('href', '/registrati')
  })

  it('shows the characters of the catalog with their element', async () => {
    vi.mocked(getCharacters).mockResolvedValue([jinhsi])

    renderPage()

    const card = (await screen.findByRole('heading', { name: 'Jinhsi' })).closest('article')
    expect(screen.getByText('Spectro')).toBeInTheDocument()
    // The name follows the icon, so the icon has an empty alt text and is not read twice
    expect(card?.querySelector('img')).toHaveAttribute('src', '/characters/jinhsi.webp')
    expect(card?.querySelector('img')).toHaveAttribute('alt', '')
    expect(screen.getByLabelText('5 stelle')).toBeInTheDocument()
  })

  it('shows the error, loads again on retry and keeps the keyboard focus in the section', async () => {
    vi.mocked(getCharacters)
      .mockRejectedValueOnce(new ApiError('NETWORK_ERROR', 'Impossibile contattare il server.', 0))
      .mockResolvedValueOnce([])

    renderPage()

    expect(await screen.findByRole('alert')).toHaveTextContent('Impossibile contattare il server.')
    fireEvent.click(screen.getByRole('button', { name: 'Riprova' }))

    expect(screen.getByRole('heading', { name: 'Personaggi della demo' })).toHaveFocus()
    expect(await screen.findByText('Nessun personaggio disponibile.')).toBeInTheDocument()
    expect(getCharacters).toHaveBeenCalledTimes(2)
  })

  it('asks the server only once in StrictMode, which runs effects twice', async () => {
    vi.mocked(getCharacters).mockResolvedValue([])

    renderPage(undefined, true)

    expect(await screen.findByText('Nessun personaggio disponibile.')).toBeInTheDocument()
    expect(getCharacters).toHaveBeenCalledOnce()
  })
})
