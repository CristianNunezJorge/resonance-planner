import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { Provider } from 'react-redux'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client'
import { setupStore } from '../../app/store'
import { gameRules } from '../../test/fixtures'
import { signedIn } from '../../test/utils'
import { getWaveplate, saveWaveplate } from './waveplateApi'
import WaveplatePanel from './WaveplatePanel'
import type { WaveplateStatus } from './types'

vi.mock('./waveplateApi', () => ({ getWaveplate: vi.fn(), saveWaveplate: vi.fn() }))

const TOKEN = 'test.jwt.token'
const NOW = new Date(2026, 9, 16, 10, 0)

const status: WaveplateStatus = {
  value: 120,
  crystals: 30,
  max: 240,
  crystalsMax: 480,
  fullAt: new Date(2026, 9, 16, 22, 0).toISOString(),
  crystalsFullAt: new Date(2026, 9, 20, 2, 0).toISOString(),
  recordedAt: new Date(2026, 9, 16, 10, 0).toISOString(),
}

function renderPanel() {
  const store = setupStore(signedIn())
  render(
    <Provider store={store}>
      <WaveplatePanel rules={gameRules} />
    </Provider>,
  )
  return store
}

async function panel() {
  return screen.findByRole('region', { name: 'Waveplate' })
}

describe('WaveplatePanel', () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true, toFake: ['setInterval', 'clearInterval', 'Date'] })
    vi.setSystemTime(NOW)
    vi.mocked(getWaveplate).mockResolvedValue(status)
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.resetAllMocks()
  })

  it('shows the Waveplate computed by the server and when they will be full', async () => {
    renderPanel()

    const region = await panel()
    expect(await within(region).findByText('120 / 240')).toBeInTheDocument()
    expect(within(region).getByText('piene alle 22:00')).toBeInTheDocument()
    expect(within(region).getByText('Crystals 30 / 480')).toBeInTheDocument()
    expect(getWaveplate).toHaveBeenCalledWith(TOKEN)
  })

  it('asks the server again every minute, without reloading the page (F5)', async () => {
    renderPanel()
    await screen.findByText('120 / 240')
    vi.mocked(getWaveplate).mockResolvedValue({ ...status, value: 130 })

    await act(async () => {
      await vi.advanceTimersByTimeAsync(60_000)
    })

    expect(await screen.findByText('130 / 240')).toBeInTheDocument()
    expect(getWaveplate).toHaveBeenCalledTimes(2)
  })

  it('does not ask while the page is hidden, and asks as soon as it is visible again', async () => {
    renderPanel()
    await screen.findByText('120 / 240')
    const visibility = vi.spyOn(document, 'visibilityState', 'get').mockReturnValue('hidden')

    await act(async () => {
      await vi.advanceTimersByTimeAsync(60_000)
    })
    expect(getWaveplate).toHaveBeenCalledTimes(1)

    visibility.mockReturnValue('visible')
    await act(async () => {
      document.dispatchEvent(new Event('visibilitychange'))
    })
    await waitFor(() => expect(getWaveplate).toHaveBeenCalledTimes(2))
  })

  it('keeps the last values when a refresh fails, and says that they may be old', async () => {
    renderPanel()
    await screen.findByText('120 / 240')
    vi.mocked(getWaveplate).mockRejectedValue(new ApiError('NETWORK_ERROR', 'Impossibile contattare il server.', 0))

    await act(async () => {
      await vi.advanceTimersByTimeAsync(60_000)
    })

    expect(screen.getByText('120 / 240')).toBeInTheDocument()
    expect(await screen.findByText('Valori non aggiornati: il server non risponde.')).toBeInTheDocument()
  })

  it('invites to write the values when none was saved yet', async () => {
    vi.mocked(getWaveplate).mockRejectedValue(new ApiError('NOT_FOUND', 'Nessuna lettura.', 404))
    renderPanel()

    expect(await screen.findByText('Non hai ancora inserito le tue Waveplate.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Inserisci' })).toBeInTheDocument()
  })

  it('shows the error of the first load with a way to try again', async () => {
    vi.mocked(getWaveplate).mockRejectedValueOnce(new ApiError('NETWORK_ERROR', 'Impossibile contattare il server.', 0))
    renderPanel()

    fireEvent.click(await screen.findByRole('button', { name: 'Riprova' }))

    expect(await screen.findByText('120 / 240')).toBeInTheDocument()
  })

  describe('update window', () => {
    it('saves the values of the game and shows them at once', async () => {
      vi.mocked(saveWaveplate).mockResolvedValue({ ...status, value: 200, crystals: 0 })
      const store = renderPanel()
      fireEvent.click(await screen.findByRole('button', { name: 'Aggiorna' }))

      const dialog = screen.getByRole('dialog', { name: 'Aggiorna le Waveplate' })
      expect(within(dialog).getByLabelText('Waveplate attuali')).toHaveValue('120')
      expect(within(dialog).getByLabelText('Waveplate attuali')).toHaveFocus()
      fireEvent.change(within(dialog).getByLabelText('Waveplate attuali'), { target: { value: '200' } })
      fireEvent.change(within(dialog).getByLabelText('Waveplate Crystals attuali'), { target: { value: '' } })
      fireEvent.click(within(dialog).getByRole('button', { name: 'Salva' }))

      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
      expect(saveWaveplate).toHaveBeenCalledWith(TOKEN, { value: 200, crystals: 0 })
      expect(screen.getByText('200 / 240')).toBeInTheDocument()
      expect(store.getState().toast.items.at(-1)?.message).toBe('Waveplate aggiornate.')
    })

    it('checks the values before sending them', async () => {
      renderPanel()
      fireEvent.click(await screen.findByRole('button', { name: 'Aggiorna' }))
      const dialog = screen.getByRole('dialog')

      fireEvent.change(within(dialog).getByLabelText('Waveplate attuali'), { target: { value: '241' } })
      fireEvent.change(within(dialog).getByLabelText('Waveplate Crystals attuali'), { target: { value: '-1' } })
      fireEvent.click(within(dialog).getByRole('button', { name: 'Salva' }))

      expect(within(dialog).getByText('Inserisci un numero intero da 0 a 240.')).toBeInTheDocument()
      expect(within(dialog).getByText('Inserisci un numero intero da 0 a 480.')).toBeInTheDocument()
      expect(within(dialog).getByLabelText('Waveplate attuali')).toHaveFocus()
      expect(saveWaveplate).not.toHaveBeenCalled()
    })

    it('shows the messages of the server under the right field', async () => {
      vi.mocked(saveWaveplate).mockRejectedValue(
        new ApiError('VALIDATION_ERROR', 'Alcuni campi non sono validi.', 400, { crystals: 'Troppi Crystals.' }),
      )
      renderPanel()
      fireEvent.click(await screen.findByRole('button', { name: 'Aggiorna' }))
      const dialog = screen.getByRole('dialog')

      fireEvent.click(within(dialog).getByRole('button', { name: 'Salva' }))

      expect(await within(dialog).findByText('Troppi Crystals.')).toBeInTheDocument()
      expect(within(dialog).getByLabelText('Waveplate Crystals attuali')).toHaveFocus()
    })

    it('shows a general failure above the fields', async () => {
      vi.mocked(saveWaveplate).mockRejectedValue(new ApiError('NETWORK_ERROR', 'Impossibile contattare il server.', 0))
      renderPanel()
      fireEvent.click(await screen.findByRole('button', { name: 'Aggiorna' }))
      const dialog = screen.getByRole('dialog')

      fireEvent.click(within(dialog).getByRole('button', { name: 'Salva' }))

      expect(await within(dialog).findByRole('alert')).toHaveTextContent('Impossibile contattare il server.')
    })

    it('closes with Annulla without saving', async () => {
      renderPanel()
      fireEvent.click(await screen.findByRole('button', { name: 'Aggiorna' }))

      fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Annulla' }))

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
      expect(saveWaveplate).not.toHaveBeenCalled()
    })

    it('starts empty when no value was saved yet', async () => {
      vi.mocked(getWaveplate).mockRejectedValue(new ApiError('NOT_FOUND', 'Nessuna lettura.', 404))
      renderPanel()
      fireEvent.click(await screen.findByRole('button', { name: 'Inserisci' }))

      expect(within(screen.getByRole('dialog')).getByLabelText('Waveplate attuali')).toHaveValue('')
    })
  })
})
