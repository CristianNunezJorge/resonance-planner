import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { Provider } from 'react-redux'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client'
import { setupStore } from '../../app/store'
import { catalogCharacters, catalogMaterials, gameRules } from '../../test/fixtures'
import { rover, signedIn } from '../../test/utils'
import { restoreSession } from '../auth/authSlice'
import { getCharacters, getGameRules, getMaterials } from '../catalog/catalogApi'
import { getMessages, getUsage, sendMessage } from '../chat/chatApi'
import type { ChatAnswer, ChatUsage } from '../chat/types'
import { getWaveplate } from '../waveplate/waveplateApi'
import {
  completeTask,
  confirmProposal,
  createBasicPlan,
  createTask,
  deleteTask,
  exportPlan,
  getCalendar,
  rejectProposal,
  updateTask,
} from './calendarApi'
import CalendarPage from './CalendarPage'
import { saveFile } from './exportFile'
import type { CalendarData, CalendarTask, GameEvent, Proposal } from './types'

vi.mock('../catalog/catalogApi', () => ({ getCharacters: vi.fn(), getGameRules: vi.fn(), getMaterials: vi.fn() }))
vi.mock('../waveplate/waveplateApi', () => ({ getWaveplate: vi.fn(), saveWaveplate: vi.fn() }))
vi.mock('../chat/chatApi', () => ({ getMessages: vi.fn(), getUsage: vi.fn(), sendMessage: vi.fn() }))
vi.mock('./calendarApi', () => ({
  getCalendar: vi.fn(),
  createTask: vi.fn(),
  updateTask: vi.fn(),
  deleteTask: vi.fn(),
  completeTask: vi.fn(),
  createBasicPlan: vi.fn(),
  confirmProposal: vi.fn(),
  rejectProposal: vi.fn(),
  exportPlan: vi.fn(),
}))
// jsdom cannot download files: the name and the content of the file are checked here, the link in exportFile.test.ts
vi.mock('./exportFile', async (importOriginal) => ({
  ...(await importOriginal<typeof import('./exportFile')>()),
  saveFile: vi.fn(),
}))
// FullCalendar needs a real browser to measure its grid: here a list of buttons stands in for it, with the same
// props. It reports the week of 12 October as the range on the screen, like FullCalendar does when it is drawn.
vi.mock('./CalendarView', async () => {
  const { useEffect } = await import('react')
  return {
    default: function FakeCalendar({
      view,
      items,
      onRangeChange,
      onItemClick,
    }: {
      view: string
      items: { id: string; title: string; extendedProps: unknown }[]
      onRangeChange: (range: { from: string; to: string }, title: string) => void
      onItemClick: (props: unknown) => void
    }) {
      useEffect(() => {
        onRangeChange({ from: '2026-10-12', to: '2026-10-18' }, '12 – 18 ott 2026')
      }, [onRangeChange])
      return (
        <div>
          <p>{`Vista: ${view}`}</p>
          <ul aria-label="Elementi del calendario">
            {items.map((item) => (
              <li key={item.id}>
                <button type="button" onClick={() => onItemClick(item.extendedProps)}>
                  {item.title}
                </button>
              </li>
            ))}
          </ul>
        </div>
      )
    },
  }
})

const TOKEN = 'test.jwt.token'
const WEEK = { from: '2026-10-12', to: '2026-10-18' }

const moonsGrace: GameEvent = {
  id: 3,
  name: "By Moon's Grace",
  type: 'POINTS_SHOP',
  description: 'Festa della Luna.',
  startDate: '2026-09-21',
  endDate: '2026-11-03',
  activeWeekdays: ['SATURDAY', 'SUNDAY'],
  activeDates: ['2026-10-17', '2026-10-18'],
  rewards: ['Palace Lantern'],
  activities: [{ title: 'Obiettivi della festa', minutes: 15, frequency: 'DAILY', unlockDay: null }],
  effect: null,
}

const manualTask: CalendarTask = {
  id: 40,
  origin: 'MANUAL',
  kind: 'CUSTOM',
  status: 'CONFIRMED',
  taskDate: '2026-10-16',
  title: 'Giro dei boss',
  notes: 'Prima Mourning Aix',
  minutes: 30,
  farmingSourceId: null,
  runs: null,
  waveplates: 0,
  eventId: null,
  characterId: null,
  completedAt: null,
}

const farmingTask: CalendarTask = {
  ...manualTask,
  id: 41,
  origin: 'AGENT',
  kind: 'FARMING',
  title: 'Boss Challenge: Mourning Aix ×2',
  notes: null,
  minutes: 8,
  farmingSourceId: 'boss-mourning-aix',
  runs: 2,
  waveplates: 120,
  characterId: 'jinhsi',
}

const week: CalendarData = { events: [moonsGrace], tasks: [manualTask, farmingTask], pendingProposal: null, planOutdated: false }

const proposedTask: CalendarTask = {
  ...farmingTask,
  id: 50,
  status: 'PROPOSED',
  taskDate: '2026-10-17',
  title: 'Simulation Training',
  farmingSourceId: 'sim-resonance-potion',
  runs: 3,
  waveplates: 120,
}

const proposal: Proposal = {
  id: 7,
  status: 'PENDING',
  summary: 'Piano base di 7 giorni, 1 attività.',
  createdAt: '2026-10-16T09:00:00Z',
  tasks: [proposedTask],
}

const chatUsage: ChatUsage = {
  used: 0,
  limit: 5,
  resetsAt: '2026-10-17T22:00:00Z',
  callsLimitReached: false,
  appLimitReached: false,
  guestLimitReached: false,
}

function stubScreen(isPhone: boolean) {
  vi.stubGlobal('matchMedia', (query: string) => ({
    matches: isPhone,
    media: query,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  }))
}

function renderPage() {
  const store = setupStore(signedIn())
  render(
    <Provider store={store}>
      <MemoryRouter>
        <CalendarPage />
      </MemoryRouter>
    </Provider>,
  )
  return store
}

function lastToast(store: ReturnType<typeof setupStore>) {
  return store.getState().toast.items.at(-1)
}

async function openItem(name: string) {
  fireEvent.click(await screen.findByRole('button', { name }))
  return screen.getByRole('dialog')
}

describe('CalendarPage', () => {
  beforeEach(() => {
    stubScreen(false)
    vi.mocked(getCharacters).mockResolvedValue(catalogCharacters)
    vi.mocked(getGameRules).mockResolvedValue(gameRules)
    vi.mocked(getMaterials).mockResolvedValue(catalogMaterials)
    vi.mocked(getWaveplate).mockRejectedValue(new ApiError('NOT_FOUND', 'Nessuna lettura.', 404))
    vi.mocked(getCalendar).mockResolvedValue(week)
    vi.mocked(getMessages).mockResolvedValue({ messages: [], hasMore: false })
    vi.mocked(getUsage).mockResolvedValue(chatUsage)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.resetAllMocks()
  })

  it('loads the events and the tasks of the days on the screen', async () => {
    renderPage()

    const list = await screen.findByRole('list', { name: 'Elementi del calendario' })
    expect(await within(list).findByRole('button', { name: "By Moon's Grace" })).toBeInTheDocument()
    expect(within(list).getByRole('button', { name: 'Giro dei boss' })).toBeInTheDocument()
    expect(getCalendar).toHaveBeenCalledWith(TOKEN, WEEK)
    expect(screen.getByRole('heading', { level: 2, name: '12 – 18 ott 2026' })).toBeInTheDocument()
    expect(document.title).toBe('Calendario · Resonance Planner')
  })

  it('asks again for the same days when a login in another tab forgets the data', async () => {
    const store = renderPage()
    await screen.findByRole('button', { name: 'Giro dei boss' })

    store.dispatch(restoreSession.fulfilled({ token: TOKEN, user: rover }, 'other-tab'))

    await waitFor(() => expect(getCalendar).toHaveBeenCalledTimes(2))
    expect(getCalendar).toHaveBeenLastCalledWith(TOKEN, WEEK)
    expect(await screen.findByRole('button', { name: 'Giro dei boss' })).toBeInTheDocument()
  })

  it('has the Waveplate panel next to the calendar', async () => {
    renderPage()

    expect(await screen.findByRole('region', { name: 'Waveplate' })).toBeInTheDocument()
    expect(await screen.findByText('Non hai ancora inserito le tue Waveplate.')).toBeInTheDocument()
  })

  it('switches between week and month on a computer', async () => {
    renderPage()
    await screen.findByText('Vista: dayGridWeek')

    fireEvent.click(screen.getByRole('button', { name: 'Mese' }))

    expect(screen.getByText('Vista: dayGridMonth')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Mese' })).toHaveAttribute('aria-pressed', 'true')
    // The arrows of the period work through FullCalendar: here they only must not break the page
    fireEvent.click(screen.getByRole('button', { name: 'Periodo successivo' }))
    fireEvent.click(screen.getByRole('button', { name: 'Oggi' }))
    fireEvent.click(screen.getByRole('button', { name: 'Periodo precedente' }))
  })

  it('shows a list by day on a phone, without the choice of the view (D67)', async () => {
    stubScreen(true)
    renderPage()

    expect(await screen.findByText('Vista: listWeek')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Mese' })).not.toBeInTheDocument()
  })

  it('says when there is no plan in the period, and points to the basic plan', async () => {
    vi.mocked(getCalendar).mockResolvedValue({ ...week, tasks: [] })
    renderPage()

    expect(await screen.findByText(/Non hai ancora un piano in questo periodo/)).toBeInTheDocument()
    // One button for one action: the one of the toolbar
    expect(screen.getAllByRole('button', { name: 'Genera piano base' })).toHaveLength(1)
  })

  describe('proposals (D68, D124)', () => {
    beforeEach(() => {
      vi.mocked(getCalendar).mockResolvedValue({ ...week, pendingProposal: proposal })
    })

    it('shows the pending proposal in a bar and its tasks as proposed in the calendar', async () => {
      renderPage()

      const bar = await screen.findByRole('region', { name: 'Proposta in attesa' })
      expect(within(bar).getByText('Piano base di 7 giorni, 1 attività.')).toBeInTheDocument()
      expect(within(bar).getByText('1 attività il 17/10')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Proposta: Simulation Training' })).toBeInTheDocument()
      // With a proposal on the screen the empty state is not needed
      expect(screen.queryByText(/Non hai ancora un piano/)).not.toBeInTheDocument()
    })

    it('confirms the proposal at once', async () => {
      vi.mocked(confirmProposal).mockResolvedValue({ ...proposal, status: 'CONFIRMED' })
      const store = renderPage()
      const bar = await screen.findByRole('region', { name: 'Proposta in attesa' })

      fireEvent.click(within(bar).getByRole('button', { name: 'Conferma' }))

      await waitFor(() => expect(lastToast(store)?.message).toBe('Piano confermato: le attività sono nel calendario.'))
      expect(confirmProposal).toHaveBeenCalledWith(TOKEN, 7)
      expect(getCalendar).toHaveBeenCalledTimes(2)
      // The bar is gone with its button: the focus is on the title of the page
      expect(screen.getByRole('heading', { level: 1, name: 'Calendario' })).toHaveFocus()
    })

    it('rejects the proposal at once', async () => {
      vi.mocked(rejectProposal).mockResolvedValue({ ...proposal, status: 'REJECTED', tasks: [] })
      const store = renderPage()
      const bar = await screen.findByRole('region', { name: 'Proposta in attesa' })

      fireEvent.click(within(bar).getByRole('button', { name: 'Scarta' }))

      await waitFor(() => expect(lastToast(store)?.message).toBe('Proposta scartata.'))
      expect(rejectProposal).toHaveBeenCalledWith(TOKEN, 7)
    })

    it('says why a proposal could not be confirmed', async () => {
      vi.mocked(confirmProposal).mockRejectedValue(
        new ApiError('CONFLICT', 'La proposta è di un altro giorno: generane una nuova.', 409),
      )
      const store = renderPage()
      const bar = await screen.findByRole('region', { name: 'Proposta in attesa' })

      fireEvent.click(within(bar).getByRole('button', { name: 'Conferma' }))

      await waitFor(() =>
        expect(lastToast(store)?.message).toBe('La proposta è di un altro giorno: generane una nuova.'),
      )
      expect(lastToast(store)?.variant).toBe('danger')
    })

    it('opens a proposed task without the actions of the calendar', async () => {
      renderPage()

      const dialog = await openItem('Proposta: Simulation Training')

      expect(within(dialog).getByText(/Fa parte della proposta in attesa/)).toBeInTheDocument()
      expect(within(dialog).queryByRole('button', { name: 'Segna come fatta' })).not.toBeInTheDocument()
      expect(within(dialog).queryByRole('button', { name: 'Modifica' })).not.toBeInTheDocument()
      expect(within(dialog).queryByRole('button', { name: 'Elimina' })).not.toBeInTheDocument()
    })
  })

  describe('basic plan (D64)', () => {
    it('is made from the toolbar, which waits for the answer', async () => {
      let resolvePlan: (value: Proposal) => void = () => undefined
      vi.mocked(createBasicPlan).mockReturnValue(new Promise((resolve) => (resolvePlan = resolve)))
      const store = renderPage()
      await screen.findByRole('button', { name: 'Giro dei boss' })

      fireEvent.click(screen.getByRole('button', { name: 'Genera piano base' }))
      expect(screen.getByRole('button', { name: 'Genera piano base' })).toBeDisabled()
      resolvePlan(proposal)

      await waitFor(() => expect(lastToast(store)?.message).toBe('Piano base pronto: controllalo e confermalo.'))
      expect(createBasicPlan).toHaveBeenCalledWith(TOKEN)
    })

    it('says why there is nothing to plan', async () => {
      vi.mocked(createBasicPlan).mockRejectedValue(
        new ApiError('CONFLICT', 'Il roster è vuoto: aggiungi un personaggio con un obiettivo per pianificare il farming.', 409),
      )
      const store = renderPage()
      await screen.findByRole('button', { name: 'Giro dei boss' })

      fireEvent.click(screen.getByRole('button', { name: 'Genera piano base' }))

      await waitFor(() => expect(lastToast(store)?.variant).toBe('danger'))
      expect(lastToast(store)?.message).toMatch(/^Il roster è vuoto/)
    })
  })

  describe('export to Excel (D133)', () => {
    it('downloads the period on the screen and says so, waiting for the file', async () => {
      const file = new Blob(['xlsx'])
      let resolveFile: (value: Blob) => void = () => undefined
      vi.mocked(exportPlan).mockReturnValue(new Promise((resolve) => (resolveFile = resolve)))
      const store = renderPage()
      await screen.findByRole('button', { name: 'Giro dei boss' })

      fireEvent.click(screen.getByRole('button', { name: 'Esporta in Excel' }))
      // It keeps the focus while it waits, and a second click does not ask for a second file
      const waiting = screen.getByRole('button', { name: 'Esportazione…' })
      expect(waiting).toHaveAttribute('aria-disabled', 'true')
      fireEvent.click(waiting)
      resolveFile(file)

      await waitFor(() => expect(lastToast(store)?.message).toBe('Piano esportato.'))
      expect(exportPlan).toHaveBeenCalledExactlyOnceWith(TOKEN, WEEK)
      expect(saveFile).toHaveBeenCalledExactlyOnceWith(file, 'resonance-planner_2026-10-12_2026-10-18.xlsx')
      expect(screen.getByRole('button', { name: 'Esporta in Excel' })).not.toHaveAttribute('aria-disabled')
    })

    it('says why the file could not be made', async () => {
      vi.mocked(exportPlan).mockRejectedValue(
        new ApiError('SERVICE_UNAVAILABLE', 'Il server non è ancora pronto: riprova tra qualche istante.', 503),
      )
      const store = renderPage()
      await screen.findByRole('button', { name: 'Giro dei boss' })

      fireEvent.click(screen.getByRole('button', { name: 'Esporta in Excel' }))

      await waitFor(() => expect(lastToast(store)?.variant).toBe('danger'))
      expect(lastToast(store)?.message).toBe('Il server non è ancora pronto: riprova tra qualche istante.')
      expect(saveFile).not.toHaveBeenCalled()
    })

    it('does not save the file of a session that ended while it was coming', async () => {
      let resolveFile: (value: Blob) => void = () => undefined
      vi.mocked(exportPlan).mockReturnValue(new Promise((resolve) => (resolveFile = resolve)))
      const store = renderPage()
      await screen.findByRole('button', { name: 'Giro dei boss' })

      fireEvent.click(screen.getByRole('button', { name: 'Esporta in Excel' }))
      store.dispatch(restoreSession.fulfilled({ token: 'another.jwt.token', user: rover }, 'other-tab'))
      resolveFile(new Blob(['xlsx']))

      await waitFor(() =>
        expect(screen.getByRole('button', { name: 'Esporta in Excel' })).not.toHaveAttribute('aria-disabled'),
      )
      expect(saveFile).not.toHaveBeenCalled()
      expect(lastToast(store)?.message).not.toBe('Piano esportato.')
    })
  })

  it('warns when roster, goals or events changed after the last plan (D23, D125)', async () => {
    vi.mocked(getCalendar).mockResolvedValue({ ...week, planOutdated: true })
    vi.mocked(createBasicPlan).mockResolvedValue(proposal)
    renderPage()

    expect(await screen.findByText('Roster, obiettivi o eventi sono cambiati dopo il tuo ultimo piano.')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Genera un nuovo piano' }))

    await waitFor(() => expect(createBasicPlan).toHaveBeenCalledWith(TOKEN))
  })

  it('shows why the calendar could not be loaded, and tries again', async () => {
    vi.mocked(getCalendar).mockRejectedValueOnce(new ApiError('NETWORK_ERROR', 'Impossibile contattare il server.', 0))
    renderPage()

    expect(await screen.findByText('Impossibile contattare il server.')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Riprova' }))

    expect(await screen.findByRole('button', { name: 'Giro dei boss' })).toBeInTheDocument()
  })

  describe('event window', () => {
    it('tells what the event is, when it runs and on which days', async () => {
      renderPage()

      const dialog = await openItem("By Moon's Grace")

      expect(dialog).toHaveAccessibleName("By Moon's Grace")
      expect(within(dialog).getByText('Negozio a punti')).toBeInTheDocument()
      expect(within(dialog).getByText('dal 21 settembre al 3 novembre 2026')).toBeInTheDocument()
      expect(within(dialog).getByText('sab, dom')).toBeInTheDocument()
      expect(within(dialog).getByText('Palace Lantern')).toBeInTheDocument()
      expect(within(dialog).getByText('Obiettivi della festa · 15 minuti · ogni giorno')).toBeInTheDocument()
      fireEvent.click(within(dialog).getByRole('button', { name: 'Chiudi' }))
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })
  })

  describe('task window', () => {
    it('shows a manual task with all its actions', async () => {
      renderPage()

      const dialog = await openItem('Giro dei boss')

      expect(within(dialog).getByText('venerdì 16 ottobre 2026')).toBeInTheDocument()
      expect(within(dialog).getByText('30 minuti')).toBeInTheDocument()
      expect(within(dialog).getByText('Prima Mourning Aix')).toBeInTheDocument()
      expect(within(dialog).getByText('Creata da te')).toBeInTheDocument()
      expect(within(dialog).getByRole('button', { name: 'Chiudi' })).toHaveFocus()
      for (const action of ['Segna come fatta', 'Modifica', 'Elimina']) {
        expect(within(dialog).getByRole('button', { name: action })).toBeInTheDocument()
      }
    })

    it('shows the runs, the Waveplate and the character of a farming task, which cannot be changed', async () => {
      renderPage()

      const dialog = await openItem('Boss Challenge: Mourning Aix ×2')

      expect(within(dialog).getByText('120')).toBeInTheDocument()
      expect(within(dialog).getByText('Jinhsi')).toBeInTheDocument()
      expect(within(dialog).getByText('Dal piano dell’agente')).toBeInTheDocument()
      expect(within(dialog).queryByRole('button', { name: 'Modifica' })).not.toBeInTheDocument()
      expect(within(dialog).queryByRole('button', { name: 'Elimina' })).not.toBeInTheDocument()
    })

    it('has no "Segna come fatta" for a task already done', async () => {
      vi.mocked(getCalendar).mockResolvedValue({ ...week, events: [], tasks: [{ ...manualTask, status: 'DONE' }] })
      renderPage()

      const dialog = await openItem('✓ Giro dei boss')

      expect(within(dialog).getByText('Fatta ✓')).toBeInTheDocument()
      expect(within(dialog).queryByRole('button', { name: 'Segna come fatta' })).not.toBeInTheDocument()
    })
  })

  describe('completion', () => {
    it('asks first, then says what entered the inventory and reloads the calendar (F7)', async () => {
      vi.mocked(completeTask).mockResolvedValue({
        task: { ...farmingTask, status: 'DONE' },
        addedToInventory: [
          { materialId: 'elegy-tacet-core', quantity: 4 },
          { materialId: 'shell-credit', quantity: 11520 },
        ],
      })
      const store = renderPage()
      const dialog = await openItem('Boss Challenge: Mourning Aix ×2')

      fireEvent.click(within(dialog).getByRole('button', { name: 'Segna come fatta' }))
      const confirm = screen.getByRole('dialog', { name: 'Segnare come fatta?' })
      expect(within(confirm).getByRole('button', { name: 'Annulla' })).toHaveFocus()
      fireEvent.click(within(confirm).getByRole('button', { name: 'Segna come fatta' }))

      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
      expect(completeTask).toHaveBeenCalledWith(TOKEN, 41)
      expect(getCalendar).toHaveBeenCalledTimes(2)
      expect(lastToast(store)?.message).toBe(
        'Attività fatta. Aggiunti all’inventario: 4 Elegy Tacet Core, 11.520 Shell Credit.',
      )
    })

    it('says only that it is done when nothing entered the inventory', async () => {
      vi.mocked(completeTask).mockResolvedValue({ task: { ...manualTask, status: 'DONE' }, addedToInventory: [] })
      const store = renderPage()
      const dialog = await openItem('Giro dei boss')

      fireEvent.click(within(dialog).getByRole('button', { name: 'Segna come fatta' }))
      fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Segna come fatta' }))

      await waitFor(() => expect(lastToast(store)?.message).toBe('Attività fatta.'))
    })

    it('shows the message of the server when it fails', async () => {
      vi.mocked(completeTask).mockRejectedValue(
        new ApiError('CONFLICT', 'Hai già segnato come fatta questa attività.', 409),
      )
      const store = renderPage()
      const dialog = await openItem('Giro dei boss')

      fireEvent.click(within(dialog).getByRole('button', { name: 'Segna come fatta' }))
      fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Segna come fatta' }))

      await waitFor(() => expect(lastToast(store)?.message).toBe('Hai già segnato come fatta questa attività.'))
      expect(lastToast(store)?.variant).toBe('danger')
    })

    it('goes back without doing anything with Annulla', async () => {
      renderPage()
      const dialog = await openItem('Giro dei boss')

      fireEvent.click(within(dialog).getByRole('button', { name: 'Segna come fatta' }))
      fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Annulla' }))

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
      expect(completeTask).not.toHaveBeenCalled()
    })
  })

  describe('manual task form', () => {
    it('creates a task and reloads the calendar (F9)', async () => {
      vi.mocked(createTask).mockResolvedValue(manualTask)
      const store = renderPage()
      fireEvent.click(await screen.findByRole('button', { name: '+ Attività' }))
      const dialog = screen.getByRole('dialog', { name: 'Nuova attività' })
      expect(within(dialog).getByLabelText('Titolo')).toHaveFocus()

      fireEvent.change(within(dialog).getByLabelText('Titolo'), { target: { value: 'Giro dei boss' } })
      fireEvent.change(within(dialog).getByLabelText('Giorno'), { target: { value: '2026-10-16' } })
      fireEvent.change(within(dialog).getByLabelText('Durata in minuti'), { target: { value: '45' } })
      fireEvent.change(within(dialog).getByLabelText('Note (facoltative)'), { target: { value: 'Con Jinhsi' } })
      fireEvent.click(within(dialog).getByRole('button', { name: 'Salva' }))

      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
      expect(createTask).toHaveBeenCalledWith(TOKEN, {
        title: 'Giro dei boss',
        taskDate: '2026-10-16',
        minutes: 45,
        notes: 'Con Jinhsi',
      })
      expect(getCalendar).toHaveBeenCalledTimes(2)
      expect(lastToast(store)?.message).toBe('Attività aggiunta: venerdì 16 ottobre 2026.')
    })

    it('keeps the task saved and says that the calendar is old when the reload fails', async () => {
      vi.mocked(createTask).mockResolvedValue(manualTask)
      const store = renderPage()
      await screen.findByRole('button', { name: 'Giro dei boss' })
      vi.mocked(getCalendar).mockRejectedValueOnce(new ApiError('NETWORK_ERROR', 'Impossibile contattare il server.', 0))
      fireEvent.click(screen.getByRole('button', { name: '+ Attività' }))
      const dialog = screen.getByRole('dialog')
      fireEvent.change(within(dialog).getByLabelText('Titolo'), { target: { value: 'Giro dei boss' } })
      fireEvent.change(within(dialog).getByLabelText('Giorno'), { target: { value: '2026-10-16' } })

      fireEvent.click(within(dialog).getByRole('button', { name: 'Salva' }))

      await waitFor(() => expect(lastToast(store)?.message).toBe('Attività aggiunta: venerdì 16 ottobre 2026.'))
      expect(await screen.findByText('Il calendario non è aggiornato. Impossibile contattare il server.')).toBeInTheDocument()
      // The data of before stay on the page
      expect(screen.getByRole('button', { name: 'Giro dei boss' })).toBeInTheDocument()
    })

    it('checks the day and the minutes before sending', async () => {
      renderPage()
      fireEvent.click(await screen.findByRole('button', { name: '+ Attività' }))
      const dialog = screen.getByRole('dialog')

      fireEvent.change(within(dialog).getByLabelText('Giorno'), { target: { value: '' } })
      fireEvent.change(within(dialog).getByLabelText('Durata in minuti'), { target: { value: '1,5' } })
      fireEvent.click(within(dialog).getByRole('button', { name: 'Salva' }))

      expect(within(dialog).getByText('Scegli il giorno.')).toBeInTheDocument()
      expect(within(dialog).getByText('Inserisci un numero intero di minuti, da 1 a 1440.')).toBeInTheDocument()
      expect(within(dialog).getByLabelText('Giorno')).toHaveFocus()
      expect(createTask).not.toHaveBeenCalled()
    })

    it('shows the messages of the server under their fields', async () => {
      vi.mocked(createTask).mockRejectedValue(
        new ApiError('VALIDATION_ERROR', 'Alcuni campi non sono validi.', 400, {
          title: 'Scrivi un titolo.',
          notes: 'Le note possono avere al massimo 1000 caratteri.',
        }),
      )
      renderPage()
      fireEvent.click(await screen.findByRole('button', { name: '+ Attività' }))
      const dialog = screen.getByRole('dialog')

      fireEvent.click(within(dialog).getByRole('button', { name: 'Salva' }))

      expect(await within(dialog).findByText('Scrivi un titolo.')).toBeInTheDocument()
      expect(within(dialog).getByText('Le note possono avere al massimo 1000 caratteri.')).toBeInTheDocument()
      expect(within(dialog).getByLabelText('Titolo')).toHaveFocus()
      expect(within(dialog).getByLabelText('Note (facoltative)')).toHaveAttribute('aria-invalid', 'true')
    })

    it('shows a failure that is not about a field above the form', async () => {
      vi.mocked(createTask).mockRejectedValue(
        new ApiError('CONFLICT', 'Hai già 500 attività: cancellane qualcuna prima di crearne altre.', 409),
      )
      renderPage()
      fireEvent.click(await screen.findByRole('button', { name: '+ Attività' }))
      const dialog = screen.getByRole('dialog')
      fireEvent.change(within(dialog).getByLabelText('Titolo'), { target: { value: 'Una in più' } })

      fireEvent.click(within(dialog).getByRole('button', { name: 'Salva' }))

      expect(await within(dialog).findByRole('alert')).toHaveTextContent('Hai già 500 attività')
    })

    it('changes a manual task with its values already in the form', async () => {
      vi.mocked(updateTask).mockResolvedValue(manualTask)
      const store = renderPage()
      const detail = await openItem('Giro dei boss')

      fireEvent.click(within(detail).getByRole('button', { name: 'Modifica' }))
      const dialog = screen.getByRole('dialog', { name: 'Modifica attività' })
      expect(within(dialog).getByLabelText('Titolo')).toHaveValue('Giro dei boss')
      expect(within(dialog).getByLabelText('Giorno')).toHaveValue('2026-10-16')
      expect(within(dialog).getByLabelText('Durata in minuti')).toHaveValue('30')
      fireEvent.change(within(dialog).getByLabelText('Titolo'), { target: { value: 'Giro dei boss doppio' } })
      fireEvent.click(within(dialog).getByRole('button', { name: 'Salva' }))

      await waitFor(() => expect(lastToast(store)?.message).toBe('Attività modificata.'))
      expect(updateTask).toHaveBeenCalledWith(TOKEN, 40, {
        title: 'Giro dei boss doppio',
        taskDate: '2026-10-16',
        minutes: 30,
        notes: 'Prima Mourning Aix',
      })
    })

    it('closes with Annulla without saving', async () => {
      renderPage()
      fireEvent.click(await screen.findByRole('button', { name: '+ Attività' }))

      fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Annulla' }))

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
      expect(createTask).not.toHaveBeenCalled()
    })
  })

  describe('deletion', () => {
    it('asks first, then deletes the task and reloads the calendar', async () => {
      vi.mocked(deleteTask).mockResolvedValue(undefined)
      const store = renderPage()
      const detail = await openItem('Giro dei boss')

      fireEvent.click(within(detail).getByRole('button', { name: 'Elimina' }))
      const confirm = screen.getByRole('dialog', { name: 'Eliminare l’attività?' })
      fireEvent.click(within(confirm).getByRole('button', { name: 'Elimina' }))

      await waitFor(() => expect(lastToast(store)?.message).toBe('Attività eliminata.'))
      expect(deleteTask).toHaveBeenCalledWith(TOKEN, 40)
      expect(getCalendar).toHaveBeenCalledTimes(2)
    })

    it('shows the message of the server when it fails', async () => {
      vi.mocked(deleteTask).mockRejectedValue(new ApiError('NOT_FOUND', 'Attività non trovata.', 404))
      const store = renderPage()
      const detail = await openItem('Giro dei boss')

      fireEvent.click(within(detail).getByRole('button', { name: 'Elimina' }))
      fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Elimina' }))

      await waitFor(() => expect(lastToast(store)?.message).toBe('Attività non trovata.'))
      expect(lastToast(store)?.variant).toBe('danger')
    })
  })

  describe('chat with the agent (D130)', () => {
    const answer: ChatAnswer = {
      message: { id: 6, role: 'ASSISTANT', content: 'Ecco un piano.', proposalId: 7, createdAt: '2026-10-17T12:00:00Z' },
      proposal,
      steps: ['Ho calcolato il piano', 'Ho preparato la proposta'],
      usage: { ...chatUsage, used: 1 },
    }

    it('shows the chat in the side column on a wide screen', async () => {
      renderPage()

      const aside = screen.getByRole('complementary', { name: 'Riepilogo' })
      expect(await within(aside).findByRole('heading', { level: 2, name: 'Agente' })).toBeInTheDocument()
      expect(within(aside).getByText('0 / 5 messaggi oggi')).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: 'Agente' })).not.toBeInTheDocument()
    })

    it('opens the chat from the side below 992 px', async () => {
      stubScreen(true)
      renderPage()
      expect(screen.queryByRole('heading', { level: 2, name: 'Agente' })).not.toBeInTheDocument()

      fireEvent.click(screen.getByRole('button', { name: 'Agente' }))

      const drawer = screen.getByRole('dialog', { name: 'Chat con l’agente' })
      expect(await within(drawer).findByText('0 / 5 messaggi oggi')).toBeInTheDocument()
      fireEvent.click(within(drawer).getByRole('button', { name: 'Chiudi la chat' }))
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })

    it('loads the calendar again when the answer brings a proposal, so the bar appears', async () => {
      vi.mocked(sendMessage).mockResolvedValue(answer)
      renderPage()
      await screen.findByRole('button', { name: 'Giro dei boss' })
      vi.mocked(getCalendar).mockResolvedValue({ ...week, pendingProposal: proposal })

      fireEvent.click(await screen.findByRole('button', { name: 'Organizzami la settimana' }))

      expect(await screen.findByRole('region', { name: 'Proposta in attesa' })).toBeInTheDocument()
      expect(getCalendar).toHaveBeenCalledTimes(2)
      expect(screen.getByText('Ecco un piano.')).toBeInTheDocument()
    })

    it('asks the agent about an event with «Spiegamelo»', async () => {
      vi.mocked(sendMessage).mockResolvedValue({ ...answer, proposal: null })
      renderPage()
      await screen.findByText('0 / 5 messaggi oggi')

      const dialog = await openItem("By Moon's Grace")
      fireEvent.click(within(dialog).getByRole('button', { name: 'Spiegamelo' }))

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
      await waitFor(() => expect(sendMessage).toHaveBeenCalledWith(TOKEN, "Spiegami l’evento «By Moon's Grace»."))
    })

    it('asks the agent to update an out of date plan, opening the chat below 992 px', async () => {
      stubScreen(true)
      vi.mocked(getCalendar).mockResolvedValue({ ...week, planOutdated: true })
      vi.mocked(sendMessage).mockResolvedValue({ ...answer, proposal: null })
      renderPage()
      await waitFor(() => expect(screen.getByRole('button', { name: 'Chiedi all’agente' })).toBeEnabled())

      fireEvent.click(screen.getByRole('button', { name: 'Chiedi all’agente' }))

      expect(screen.getByRole('dialog', { name: 'Chat con l’agente' })).toBeInTheDocument()
      await waitFor(() =>
        expect(sendMessage).toHaveBeenCalledWith(
          TOKEN,
          'Roster, obiettivi o eventi sono cambiati dopo il mio ultimo piano: aggiornalo.',
        ),
      )
    })

    it('does not offer «Spiegamelo» while the agent cannot answer today', async () => {
      vi.mocked(getUsage).mockResolvedValue({ ...chatUsage, appLimitReached: true })
      renderPage()
      await screen.findByText(/finito le richieste gratuite/)

      const dialog = await openItem("By Moon's Grace")

      expect(within(dialog).getByRole('button', { name: 'Spiegamelo' })).toBeDisabled()
    })
  })
})
