import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { Provider } from 'react-redux'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client'
import { setupStore } from '../../app/store'
import { gameRules } from '../../test/fixtures'
import { signedIn } from '../../test/utils'
import { getGameRules } from '../catalog/catalogApi'
import { createEvent, deleteEvent, listEvents, updateEvent } from './adminEventsApi'
import AdminEventsPage from './AdminEventsPage'
import type { AdminEvent } from './types'

vi.mock('../catalog/catalogApi', () => ({ getCharacters: vi.fn(), getGameRules: vi.fn(), getMaterials: vi.fn() }))
vi.mock('./adminEventsApi', () => ({
  listEvents: vi.fn(),
  createEvent: vi.fn(),
  updateEvent: vi.fn(),
  deleteEvent: vi.fn(),
}))

const TOKEN = 'test.jwt.token'

const bountiful: AdminEvent = {
  id: 1,
  code: 'bountiful-crescendo',
  name: 'Bountiful Crescendo',
  type: 'DOUBLE_DROP',
  description: 'Ricompense doppie.',
  schedule: { type: 'RECURRING', startDate: null, endDate: null, startOffsetDays: 3, durationDays: 7, repeatEveryDays: 14 },
  activeWeekdays: null,
  rewards: ['Ricompense doppie'],
  activities: [],
  effect: { type: 'DROP_MULTIPLIER', sourceType: 'SIMULATION_TRAINING', multiplier: 2, dailyLimit: 3 },
}

const tournament: AdminEvent = {
  id: 4,
  code: null,
  name: 'Torneo del weekend',
  type: 'COMBAT_STAGES',
  description: 'Solo sabato e domenica.',
  schedule: { type: 'FIXED', startDate: '2026-10-12', endDate: '2026-10-18', startOffsetDays: null, durationDays: null, repeatEveryDays: null },
  activeWeekdays: ['SATURDAY', 'SUNDAY'],
  rewards: [],
  activities: [{ title: 'Livello 1', minutes: 20, frequency: 'ONCE', unlockDay: 6 }],
  effect: null,
}

function renderPage() {
  const store = setupStore(signedIn({ role: 'ADMIN' }))
  render(
    <Provider store={store}>
      <MemoryRouter>
        <AdminEventsPage />
      </MemoryRouter>
    </Provider>,
  )
  return store
}

function lastToast(store: ReturnType<typeof setupStore>) {
  return store.getState().toast.items.at(-1)
}

function type(dialog: HTMLElement, label: string, value: string) {
  fireEvent.change(within(dialog).getByLabelText(label), { target: { value } })
}

async function openNewEvent() {
  fireEvent.click(await screen.findByRole('button', { name: '+ Nuovo evento' }))
  return screen.getByRole('dialog', { name: 'Nuovo evento' })
}

describe('AdminEventsPage', () => {
  beforeEach(() => {
    vi.mocked(getGameRules).mockResolvedValue(gameRules)
    vi.mocked(listEvents).mockResolvedValue([bountiful, tournament])
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  it('lists the events with their type, dates and days', async () => {
    renderPage()

    const table = await screen.findByRole('table', { name: 'Eventi del gioco' })
    const [, first, second] = within(table).getAllByRole('row')
    expect(within(first).getByRole('rowheader')).toHaveTextContent('Bountiful CrescendoDemo')
    expect(within(first).getByText('Ricompense doppie')).toBeInTheDocument()
    expect(within(first).getByText('ogni 14 giorni, per 7 giorni')).toBeInTheDocument()
    expect(within(first).getByText('tutti i giorni')).toBeInTheDocument()
    expect(within(second).getByText('dal 12/10/2026 al 18/10/2026')).toBeInTheDocument()
    expect(within(second).getByText('sab, dom')).toBeInTheDocument()
    expect(listEvents).toHaveBeenCalledWith(TOKEN)
    expect(document.title).toBe('Gestione eventi · Resonance Planner')
  })

  it('says when there are no events', async () => {
    vi.mocked(listEvents).mockResolvedValue([])
    renderPage()

    expect(await screen.findByText('Non ci sono eventi. Crea il primo con «+ Nuovo evento».')).toBeInTheDocument()
  })

  it('shows why the list could not be loaded, and tries again', async () => {
    vi.mocked(listEvents).mockRejectedValueOnce(new ApiError('FORBIDDEN', 'Non hai i permessi.', 403))
    renderPage()

    expect(await screen.findByRole('alert')).toHaveTextContent('Non hai i permessi.')
    fireEvent.click(screen.getByRole('button', { name: 'Riprova' }))

    expect(await screen.findByRole('table')).toBeInTheDocument()
  })

  describe('new event', () => {
    it('sends every part of the form and reloads the list', async () => {
      vi.mocked(createEvent).mockResolvedValue(tournament)
      const store = renderPage()
      const dialog = await openNewEvent()
      expect(within(dialog).getByLabelText('Nome')).toHaveFocus()
      // Every day is checked at the start (P7)
      expect(within(dialog).getByRole('checkbox', { name: 'Lunedì' })).toBeChecked()

      type(dialog, 'Nome', 'Torneo del weekend')
      type(dialog, 'Descrizione', 'Solo sabato e domenica.')
      type(dialog, 'Primo giorno', '2026-10-12')
      type(dialog, 'Ultimo giorno', '2026-10-18')
      for (const day of ['Lunedì', 'Martedì', 'Mercoledì', 'Giovedì', 'Venerdì']) {
        fireEvent.click(within(dialog).getByRole('checkbox', { name: day }))
      }
      fireEvent.click(within(dialog).getByRole('button', { name: '+ Ricompensa' }))
      type(dialog, 'Ricompensa 1', 'Astrite')
      fireEvent.click(within(dialog).getByRole('button', { name: '+ Attività' }))
      type(dialog, 'Titolo', 'Livello 1')
      type(dialog, 'Minuti', '20')
      type(dialog, 'Dal giorno', '6')
      fireEvent.click(within(dialog).getByRole('button', { name: 'Salva' }))

      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
      expect(createEvent).toHaveBeenCalledWith(TOKEN, {
        name: 'Torneo del weekend',
        type: 'COMBAT_STAGES',
        description: 'Solo sabato e domenica.',
        schedule: { type: 'FIXED', startDate: '2026-10-12', endDate: '2026-10-18', startOffsetDays: null, durationDays: null, repeatEveryDays: null },
        activeWeekdays: ['SATURDAY', 'SUNDAY'],
        rewards: ['Astrite'],
        activities: [{ title: 'Livello 1', minutes: 20, frequency: 'ONCE', unlockDay: 6 }],
        effect: null,
      })
      expect(listEvents).toHaveBeenCalledTimes(2)
      expect(lastToast(store)?.message).toBe('Evento creato.')
    })

    it('asks for the effect only for double rewards, with the multiplier written with a comma', async () => {
      vi.mocked(createEvent).mockResolvedValue(bountiful)
      renderPage()
      const dialog = await openNewEvent()
      expect(within(dialog).queryByLabelText('Moltiplicatore')).not.toBeInTheDocument()

      fireEvent.change(within(dialog).getByLabelText('Tipo'), { target: { value: 'DOUBLE_DROP' } })
      fireEvent.change(within(dialog).getByLabelText('Tipo di fonte'), { target: { value: 'BOSS_CHALLENGE' } })
      type(dialog, 'Moltiplicatore', '1,5')
      type(dialog, 'Run al giorno', '2')
      fireEvent.click(within(dialog).getByRole('button', { name: 'Salva' }))

      await waitFor(() => expect(createEvent).toHaveBeenCalled())
      expect(vi.mocked(createEvent).mock.calls[0][1].effect).toEqual({
        type: 'DROP_MULTIPLIER',
        sourceType: 'BOSS_CHALLENGE',
        multiplier: 1.5,
        dailyLimit: 2,
      })
    })

    it('counts a recurring event from the anchor date of the rules', async () => {
      renderPage()
      const dialog = await openNewEvent()

      fireEvent.click(within(dialog).getByRole('radio', { name: 'Si ripete' }))

      expect(within(dialog).queryByLabelText('Primo giorno')).not.toBeInTheDocument()
      expect(within(dialog).getByText('Contati dal 01/10/2026; anche negativi.')).toBeInTheDocument()
    })

    it('checks the numbers and the days before sending, with the focus on the first problem', async () => {
      renderPage()
      const dialog = await openNewEvent()

      fireEvent.click(within(dialog).getByRole('radio', { name: 'Si ripete' }))
      type(dialog, 'Durata in giorni', 'sette')
      for (const day of ['Lunedì', 'Martedì', 'Mercoledì', 'Giovedì', 'Venerdì', 'Sabato', 'Domenica']) {
        fireEvent.click(within(dialog).getByRole('checkbox', { name: day }))
      }
      fireEvent.click(within(dialog).getByRole('button', { name: 'Salva' }))

      expect(within(dialog).getByText('Inserisci un numero intero.')).toBeInTheDocument()
      expect(within(dialog).getByText('Scegli almeno un giorno.')).toBeInTheDocument()
      await waitFor(() => expect(within(dialog).getByLabelText('Durata in giorni')).toHaveFocus())
      expect(createEvent).not.toHaveBeenCalled()
    })

    it('shows the messages of the server under their fields, also inside the rows', async () => {
      vi.mocked(createEvent).mockRejectedValue(
        new ApiError('VALIDATION_ERROR', 'Alcuni campi non sono validi.', 400, {
          name: 'Inserisci il nome.',
          'activities[0].unlockDay': 'Il giorno di sblocco supera la durata.',
          schedule: 'Le date non sono valide.',
        }),
      )
      renderPage()
      const dialog = await openNewEvent()
      fireEvent.click(within(dialog).getByRole('button', { name: '+ Attività' }))

      fireEvent.click(within(dialog).getByRole('button', { name: 'Salva' }))

      expect(await within(dialog).findByText('Inserisci il nome.')).toBeInTheDocument()
      expect(within(dialog).getByText('Il giorno di sblocco supera la durata.')).toBeInTheDocument()
      expect(within(dialog).getByLabelText('Dal giorno')).toHaveAttribute('aria-invalid', 'true')
      // A message for a field that the form does not show goes on top
      expect(within(dialog).getByRole('alert')).toHaveTextContent('Le date non sono valide.')
      await waitFor(() => expect(within(dialog).getByLabelText('Nome')).toHaveFocus())
    })

    it('shows the message of a failure without fields on top', async () => {
      vi.mocked(createEvent).mockRejectedValue(new ApiError('NETWORK_ERROR', 'Impossibile contattare il server.', 0))
      renderPage()
      const dialog = await openNewEvent()

      fireEvent.click(within(dialog).getByRole('button', { name: 'Salva' }))

      const alert = await within(dialog).findByRole('alert')
      expect(alert).toHaveTextContent('Impossibile contattare il server.')
      await waitFor(() => expect(alert).toHaveFocus())
    })

    it('adds and removes rewards and activities', async () => {
      renderPage()
      const dialog = await openNewEvent()

      fireEvent.click(within(dialog).getByRole('button', { name: '+ Ricompensa' }))
      fireEvent.click(within(dialog).getByRole('button', { name: '+ Ricompensa' }))
      fireEvent.click(within(dialog).getByRole('button', { name: 'Togli la ricompensa 1' }))
      fireEvent.click(within(dialog).getByRole('button', { name: '+ Attività' }))
      fireEvent.click(within(dialog).getByRole('button', { name: 'Togli l’attività 1' }))

      expect(within(dialog).getByLabelText('Ricompensa 1')).toBeInTheDocument()
      expect(within(dialog).queryByLabelText('Ricompensa 2')).not.toBeInTheDocument()
      expect(within(dialog).queryByRole('heading', { name: 'Attività 1' })).not.toBeInTheDocument()
    })

    it('takes away the errors of a list when one of its rows is removed, and keeps the focus in the window', async () => {
      vi.mocked(createEvent).mockRejectedValue(
        new ApiError('VALIDATION_ERROR', 'Alcuni campi non sono validi.', 400, {
          'activities[1].unlockDay': 'Il giorno di sblocco supera la durata.',
          name: 'Inserisci il nome.',
        }),
      )
      renderPage()
      const dialog = await openNewEvent()
      fireEvent.click(within(dialog).getByRole('button', { name: '+ Attività' }))
      fireEvent.click(within(dialog).getByRole('button', { name: '+ Attività' }))
      fireEvent.click(within(dialog).getByRole('button', { name: 'Salva' }))
      expect(await within(dialog).findByText('Il giorno di sblocco supera la durata.')).toBeInTheDocument()

      fireEvent.click(within(dialog).getByRole('button', { name: 'Togli l’attività 1' }))

      // The error was for the second activity, which is now the first: it does not move onto another row
      expect(within(dialog).queryByText('Il giorno di sblocco supera la durata.')).not.toBeInTheDocument()
      expect(within(dialog).getByText('Inserisci il nome.')).toBeInTheDocument()
      expect(within(dialog).getByRole('button', { name: '+ Attività' })).toHaveFocus()
    })

    it('closes with Annulla without saving', async () => {
      renderPage()
      const dialog = await openNewEvent()

      fireEvent.click(within(dialog).getByRole('button', { name: 'Annulla' }))

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
      expect(createEvent).not.toHaveBeenCalled()
    })
  })

  it('changes an event with its values already in the form', async () => {
    vi.mocked(updateEvent).mockResolvedValue(bountiful)
    const store = renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'Modifica Bountiful Crescendo' }))
    const dialog = screen.getByRole('dialog', { name: 'Modifica «Bountiful Crescendo»' })

    expect(within(dialog).getByLabelText('Nome')).toHaveValue('Bountiful Crescendo')
    expect(within(dialog).getByRole('radio', { name: 'Si ripete' })).toBeChecked()
    expect(within(dialog).getByLabelText('Durata in giorni')).toHaveValue('7')
    expect(within(dialog).getByLabelText('Moltiplicatore')).toHaveValue('2')
    type(dialog, 'Run al giorno', '4')
    fireEvent.click(within(dialog).getByRole('button', { name: 'Salva' }))

    await waitFor(() => expect(lastToast(store)?.message).toBe('Evento modificato.'))
    expect(updateEvent).toHaveBeenCalledWith(TOKEN, 1, expect.objectContaining({
      name: 'Bountiful Crescendo',
      effect: { type: 'DROP_MULTIPLIER', sourceType: 'SIMULATION_TRAINING', multiplier: 2, dailyLimit: 4 },
    }))
  })

  describe('deletion', () => {
    it('warns that a demo event comes back, then deletes it', async () => {
      vi.mocked(deleteEvent).mockResolvedValue(undefined)
      const store = renderPage()
      fireEvent.click(await screen.findByRole('button', { name: 'Elimina Bountiful Crescendo' }))

      const confirm = screen.getByRole('dialog', { name: 'Eliminare l’evento?' })
      expect(confirm).toHaveTextContent('Le attività già collegate restano')
      expect(confirm).toHaveTextContent('ricompare al prossimo riavvio del server')
      fireEvent.click(within(confirm).getByRole('button', { name: 'Elimina' }))

      await waitFor(() => expect(lastToast(store)?.message).toBe('Evento eliminato.'))
      expect(deleteEvent).toHaveBeenCalledWith(TOKEN, 1)
    })

    it('does not speak of the restart for an event of the admin', async () => {
      renderPage()
      fireEvent.click(await screen.findByRole('button', { name: 'Elimina Torneo del weekend' }))

      expect(screen.getByRole('dialog')).not.toHaveTextContent('ricompare')
    })

    it('shows the message of the server when it fails', async () => {
      vi.mocked(deleteEvent).mockRejectedValue(new ApiError('NOT_FOUND', 'Evento non trovato.', 404))
      const store = renderPage()
      fireEvent.click(await screen.findByRole('button', { name: 'Elimina Torneo del weekend' }))

      fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Elimina' }))

      await waitFor(() => expect(lastToast(store)?.message).toBe('Evento non trovato.'))
      expect(lastToast(store)?.variant).toBe('danger')
    })
  })
})
