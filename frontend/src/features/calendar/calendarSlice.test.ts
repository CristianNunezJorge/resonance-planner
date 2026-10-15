import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client'
import { setupStore } from '../../app/store'
import { signedIn } from '../../test/utils'
import { sessionEnded } from '../auth/authSlice'
import { loadRoster } from '../roster/rosterSlice'
import { getRoster } from '../roster/rosterApi'
import { completeTask, createTask, deleteTask, getCalendar, updateTask } from './calendarApi'
import {
  completeCalendarTask,
  createCalendarTask,
  deleteCalendarTask,
  loadCalendar,
  updateCalendarTask,
} from './calendarSlice'
import type { CalendarData, CalendarTask } from './types'

vi.mock('./calendarApi', () => ({
  getCalendar: vi.fn(),
  createTask: vi.fn(),
  updateTask: vi.fn(),
  deleteTask: vi.fn(),
  completeTask: vi.fn(),
}))
vi.mock('../roster/rosterApi', () => ({
  getRoster: vi.fn(),
  getInventory: vi.fn(),
  getFarmingPlan: vi.fn(),
  getRequirements: vi.fn(),
}))

const TOKEN = 'test.jwt.token'
const WEEK = { from: '2026-10-12', to: '2026-10-18' }
const NEXT_WEEK = { from: '2026-10-19', to: '2026-10-25' }
const form = { title: 'Giro dei boss', taskDate: '2026-10-16', minutes: 30, notes: '' }

const task: CalendarTask = {
  id: 40,
  origin: 'MANUAL',
  kind: 'CUSTOM',
  status: 'CONFIRMED',
  taskDate: '2026-10-16',
  title: 'Giro dei boss',
  notes: null,
  minutes: 30,
  farmingSourceId: null,
  runs: null,
  waveplates: 0,
  eventId: null,
  characterId: null,
  completedAt: null,
}

const week: CalendarData = { events: [], tasks: [task] }

function newStore() {
  return setupStore(signedIn())
}

// Lets the reload that a change starts arrive before the test looks at the store
async function settle() {
  await new Promise((resolve) => setTimeout(resolve, 0))
}

describe('calendarSlice', () => {
  beforeEach(() => {
    vi.mocked(getCalendar).mockResolvedValue(week)
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  describe('loadCalendar', () => {
    it('keeps the events and the tasks of the range it asked for', async () => {
      const store = newStore()

      await store.dispatch(loadCalendar(WEEK))

      expect(getCalendar).toHaveBeenCalledWith(TOKEN, WEEK)
      const state = store.getState().calendar
      expect(state.status).toBe('succeeded')
      expect(state.range).toEqual(WEEK)
      expect(state.tasks).toEqual([task])
    })

    it('keeps the old data on the page while another range arrives', async () => {
      const store = newStore()
      await store.dispatch(loadCalendar(WEEK))
      let resolveNext: (data: CalendarData) => void = () => undefined
      vi.mocked(getCalendar).mockReturnValueOnce(new Promise((resolve) => (resolveNext = resolve)))

      const next = store.dispatch(loadCalendar(NEXT_WEEK))

      expect(store.getState().calendar.refreshing).toBe(true)
      expect(store.getState().calendar.tasks).toEqual([task])
      resolveNext({ events: [], tasks: [] })
      await next
      expect(store.getState().calendar.refreshing).toBe(false)
      expect(store.getState().calendar.tasks).toEqual([])
    })

    it('ignores the answer of a range that was replaced by a newer one', async () => {
      const store = newStore()
      let resolveOld: (data: CalendarData) => void = () => undefined
      vi.mocked(getCalendar)
        .mockReturnValueOnce(new Promise((resolve) => (resolveOld = resolve)))
        .mockResolvedValueOnce({ events: [], tasks: [] })

      const old = store.dispatch(loadCalendar(WEEK))
      await store.dispatch(loadCalendar(NEXT_WEEK))
      resolveOld(week)
      await old

      expect(store.getState().calendar.range).toEqual(NEXT_WEEK)
      expect(store.getState().calendar.tasks).toEqual([])
    })

    it('says why the first load failed', async () => {
      vi.mocked(getCalendar).mockRejectedValue(new ApiError('NETWORK_ERROR', 'Impossibile contattare il server.', 0))
      const store = newStore()

      await store.dispatch(loadCalendar(WEEK))

      expect(store.getState().calendar.status).toBe('failed')
      expect(store.getState().calendar.error).toBe('Impossibile contattare il server.')
    })

    it('keeps the data of before when a reload fails, with the message', async () => {
      const store = newStore()
      await store.dispatch(loadCalendar(WEEK))
      vi.mocked(getCalendar).mockRejectedValue(new ApiError('NETWORK_ERROR', 'Impossibile contattare il server.', 0))

      await store.dispatch(loadCalendar(WEEK))

      expect(store.getState().calendar.status).toBe('succeeded')
      expect(store.getState().calendar.tasks).toEqual([task])
      expect(store.getState().calendar.error).toBe('Impossibile contattare il server.')
    })

    it('needs a session', async () => {
      const store = setupStore()

      await store.dispatch(loadCalendar(WEEK))

      expect(getCalendar).not.toHaveBeenCalled()
      expect(store.getState().calendar.status).toBe('failed')
    })

    it('forgets everything when the session ends (D103)', async () => {
      const store = newStore()
      await store.dispatch(loadCalendar(WEEK))

      store.dispatch(sessionEnded())

      expect(store.getState().calendar.status).toBe('idle')
      expect(store.getState().calendar.tasks).toEqual([])
      expect(store.getState().calendar.range).toBeNull()
    })
  })

  describe('changes', () => {
    it('create a task and reload the range on the page', async () => {
      vi.mocked(createTask).mockResolvedValue(task)
      const store = newStore()
      await store.dispatch(loadCalendar(WEEK))

      const result = await store.dispatch(createCalendarTask(form))

      expect(result.payload).toEqual(task)
      expect(createTask).toHaveBeenCalledWith(TOKEN, form)
      expect(getCalendar).toHaveBeenCalledTimes(2)
      expect(getCalendar).toHaveBeenLastCalledWith(TOKEN, WEEK)
    })

    it('change and delete a task', async () => {
      vi.mocked(updateTask).mockResolvedValue(task)
      vi.mocked(deleteTask).mockResolvedValue(undefined)
      const store = newStore()
      await store.dispatch(loadCalendar(WEEK))

      await store.dispatch(updateCalendarTask({ id: 40, form }))
      await store.dispatch(deleteCalendarTask(40))

      expect(updateTask).toHaveBeenCalledWith(TOKEN, 40, form)
      expect(deleteTask).toHaveBeenCalledWith(TOKEN, 40)
      expect(getCalendar).toHaveBeenCalledTimes(3)
    })

    it('return the failure with the messages of the fields, without reloading', async () => {
      const failure = new ApiError('VALIDATION_ERROR', 'Alcuni campi non sono validi.', 400, { title: 'Scrivi un titolo.' })
      vi.mocked(createTask).mockRejectedValue(failure)
      const store = newStore()
      await store.dispatch(loadCalendar(WEEK))

      const result = await store.dispatch(createCalendarTask(form))

      expect(result.payload).toMatchObject({ code: 'VALIDATION_ERROR', fields: { title: 'Scrivi un titolo.' } })
      expect(getCalendar).toHaveBeenCalledTimes(1)
    })

    it('complete a task, and the roster asks again for the inventory it changed (D118)', async () => {
      const completion = { task: { ...task, status: 'DONE' as const }, addedToInventory: [{ materialId: 'shell-credit', quantity: 100 }] }
      vi.mocked(completeTask).mockResolvedValue(completion)
      vi.mocked(getRoster).mockResolvedValue([])
      const store = newStore()
      await store.dispatch(loadRoster())
      await store.dispatch(loadCalendar(WEEK))

      const result = await store.dispatch(completeCalendarTask(40))
      await settle()

      expect(result.payload).toEqual(completion)
      expect(completeTask).toHaveBeenCalledWith(TOKEN, 40)
      expect(store.getState().roster.status).toBe('idle')
    })

    it('do not reload anything for a session that ended in the meantime', async () => {
      let resolveCreate: (value: CalendarTask) => void = () => undefined
      vi.mocked(createTask).mockReturnValue(new Promise((resolve) => (resolveCreate = resolve)))
      const store = newStore()
      await store.dispatch(loadCalendar(WEEK))

      const pending = store.dispatch(createCalendarTask(form))
      store.dispatch(sessionEnded())
      resolveCreate(task)
      await pending

      expect(getCalendar).toHaveBeenCalledTimes(1)
    })

    it('need a session', async () => {
      const store = setupStore()

      const result = await store.dispatch(deleteCalendarTask(40))

      expect(deleteTask).not.toHaveBeenCalled()
      expect(result.payload).toMatchObject({ code: 'NO_SESSION' })
    })
  })
})
