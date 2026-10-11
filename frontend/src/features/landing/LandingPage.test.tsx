import { fireEvent, render, screen } from '@testing-library/react'
import { StrictMode } from 'react'
import { Provider } from 'react-redux'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client'
import { setupStore } from '../../app/store'
import { getCharacters } from '../catalog/catalogApi'
import LandingPage from './LandingPage'

vi.mock('../catalog/catalogApi', () => ({ getCharacters: vi.fn() }))

function renderPage() {
  render(
    <Provider store={setupStore()}>
      <LandingPage />
    </Provider>,
  )
}

describe('LandingPage', () => {
  afterEach(() => {
    vi.mocked(getCharacters).mockReset()
  })

  it('shows the characters of the catalog with their element', async () => {
    vi.mocked(getCharacters).mockResolvedValue([
      {
        id: 'jinhsi',
        name: 'Jinhsi',
        element: 'SPECTRO',
        weaponType: 'BROADBLADE',
        rarity: 5,
        role: 'Main Damage Dealer',
        icon: 'characters/jinhsi.webp',
      },
    ])

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

    render(
      <StrictMode>
        <Provider store={setupStore()}>
          <LandingPage />
        </Provider>
      </StrictMode>,
    )

    expect(await screen.findByText('Nessun personaggio disponibile.')).toBeInTheDocument()
    expect(getCharacters).toHaveBeenCalledOnce()
  })
})
