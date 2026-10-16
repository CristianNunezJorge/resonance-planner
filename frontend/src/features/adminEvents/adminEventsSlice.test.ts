import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client'
import { setupStore } from '../../app/store'
import { signedIn } from '../../test/utils'
import { sessionEnded } from '../auth/authSlice'
import { createEvent, deleteEvent, listEvents, updateEvent } from './adminEventsApi'
import { createAdminEvent, deleteAdminEvent, loadAdminEvents, updateAdminEvent } from './adminEventsSlice'
import type { AdminEvent, EventRequest } from './types'

vi.mock('./adminEventsApi', () => ({
  listEvents: vi.fn(),
  createEvent: vi.fn(),
  updateEvent: vi.fn(),
  deleteEvent: vi.fn(),
}))

const TOKEN = 'test.jwt.token'

const request: EventRequest = {
  name: 'Torneo',
  type: 'COMBAT_STAGES',
  description: 'Prova.',
  schedule: { type: 'FIXED', startDate: '2026-10-12', endDate: '2026-10-18', startOffsetDays: null, durationDays: null, repeatEveryDays: null },
  activeWeekdays: ['SATURDAY'],
  rewards: [],
  activities: [],
  effect: null,
}
const tournament: AdminEvent = { id: 4, code: null, ...request, activities: [] }

function newStore() {
  return setupStore(signedIn({ role: 'ADMIN' }))
}

describe('adminEventsSlice', () => {
  beforeEach(() => {
    vi.mocked(listEvents).mockResolvedValue([tournament])
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  it('loads the events', async () => {
    const store = newStore()

    await store.dispatch(loadAdminEvents())

    expect(listEvents).toHaveBeenCalledWith(TOKEN)
    expect(store.getState().adminEvents.status).toBe('succeeded')
    expect(store.getState().adminEvents.events).toEqual([tournament])
  })

  it('ignores the answer of a load replaced by a newer one', async () => {
    const store = newStore()
    let resolveOld: (events: AdminEvent[]) => void = () => undefined
    vi.mocked(listEvents)
      .mockReturnValueOnce(new Promise((resolve) => (resolveOld = resolve)))
      .mockResolvedValueOnce([])

    const old = store.dispatch(loadAdminEvents())
    await store.dispatch(loadAdminEvents())
    resolveOld([tournament])
    await old

    expect(store.getState().adminEvents.events).toEqual([])
  })

  it('says why the first load failed, and keeps the data when a reload fails', async () => {
    const store = newStore()
    vi.mocked(listEvents).mockRejectedValueOnce(new ApiError('FORBIDDEN', 'Non hai i permessi.', 403))

    await store.dispatch(loadAdminEvents())
    expect(store.getState().adminEvents.status).toBe('failed')
    expect(store.getState().adminEvents.error).toBe('Non hai i permessi.')

    await store.dispatch(loadAdminEvents())
    vi.mocked(listEvents).mockRejectedValueOnce(new ApiError('NETWORK_ERROR', 'Impossibile contattare il server.', 0))
    await store.dispatch(loadAdminEvents())
    expect(store.getState().adminEvents.status).toBe('succeeded')
    expect(store.getState().adminEvents.events).toEqual([tournament])
    expect(store.getState().adminEvents.error).toBe('Impossibile contattare il server.')
  })

  it('shows a reload while the old data stay', async () => {
    const store = newStore()
    await store.dispatch(loadAdminEvents())
    let resolve: (events: AdminEvent[]) => void = () => undefined
    vi.mocked(listEvents).mockReturnValueOnce(new Promise((done) => (resolve = done)))

    const reload = store.dispatch(loadAdminEvents())

    expect(store.getState().adminEvents.refreshing).toBe(true)
    resolve([])
    await reload
    expect(store.getState().adminEvents.refreshing).toBe(false)
  })

  it('needs a session, and forgets everything when it ends', async () => {
    const guest = setupStore()
    await guest.dispatch(loadAdminEvents())
    expect(listEvents).not.toHaveBeenCalled()

    const store = newStore()
    await store.dispatch(loadAdminEvents())
    store.dispatch(sessionEnded())
    expect(store.getState().adminEvents.status).toBe('idle')
    expect(store.getState().adminEvents.events).toEqual([])
  })

  it('creates, replaces and deletes an event, reloading the list after each', async () => {
    vi.mocked(createEvent).mockResolvedValue(tournament)
    vi.mocked(updateEvent).mockResolvedValue(tournament)
    vi.mocked(deleteEvent).mockResolvedValue(undefined)
    const store = newStore()
    await store.dispatch(loadAdminEvents())

    expect((await store.dispatch(createAdminEvent(request))).payload).toEqual(tournament)
    await store.dispatch(updateAdminEvent({ id: 4, event: request }))
    expect((await store.dispatch(deleteAdminEvent(4))).payload).toBe(4)

    expect(createEvent).toHaveBeenCalledWith(TOKEN, request)
    expect(updateEvent).toHaveBeenCalledWith(TOKEN, 4, request)
    expect(deleteEvent).toHaveBeenCalledWith(TOKEN, 4)
    expect(listEvents).toHaveBeenCalledTimes(4)
  })

  it('returns the failure with the fields, without reloading', async () => {
    vi.mocked(createEvent).mockRejectedValue(
      new ApiError('VALIDATION_ERROR', 'Alcuni campi non sono validi.', 400, { 'schedule.endDate': 'La fine viene prima.' }),
    )
    const store = newStore()
    await store.dispatch(loadAdminEvents())

    const result = await store.dispatch(createAdminEvent(request))

    expect(result.payload).toMatchObject({ fields: { 'schedule.endDate': 'La fine viene prima.' } })
    expect(listEvents).toHaveBeenCalledTimes(1)
  })

  it('does not reload for a session that ended while the change was running', async () => {
    let resolveDelete: () => void = () => undefined
    vi.mocked(deleteEvent).mockReturnValue(new Promise<void>((resolve) => (resolveDelete = resolve)))
    const store = newStore()
    await store.dispatch(loadAdminEvents())

    const pending = store.dispatch(deleteAdminEvent(4))
    store.dispatch(sessionEnded())
    resolveDelete()
    await pending

    expect(listEvents).toHaveBeenCalledTimes(1)
  })

  it('refuses a change without a session', async () => {
    const result = await setupStore().dispatch(deleteAdminEvent(4))

    expect(deleteEvent).not.toHaveBeenCalled()
    expect(result.payload).toMatchObject({ code: 'NO_SESSION' })
  })
})
