import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiRequest } from '../../api/client'
import { completeTask, createTask, deleteTask, getCalendar, updateTask } from './calendarApi'

vi.mock('../../api/client', () => ({ apiRequest: vi.fn() }))

const TOKEN = 'some.jwt.token'
const form = { title: 'Giro dei boss', taskDate: '2026-10-16', minutes: 30, notes: '' }

describe('calendarApi', () => {
  afterEach(() => {
    vi.mocked(apiRequest).mockReset()
  })

  it('reads the calendar of a range, with the dates in the query', async () => {
    vi.mocked(apiRequest).mockResolvedValue({ events: [], tasks: [] })

    await getCalendar(TOKEN, { from: '2026-10-12', to: '2026-10-18' })

    expect(apiRequest).toHaveBeenCalledWith('/api/me/calendar?from=2026-10-12&to=2026-10-18', { token: TOKEN })
  })

  it('creates, changes, deletes and completes a task', async () => {
    vi.mocked(apiRequest).mockResolvedValue(undefined)

    await createTask(TOKEN, form)
    await updateTask(TOKEN, 40, form)
    await deleteTask(TOKEN, 40)
    await completeTask(TOKEN, 40)

    expect(apiRequest).toHaveBeenNthCalledWith(1, '/api/me/tasks', { method: 'POST', body: form, token: TOKEN })
    expect(apiRequest).toHaveBeenNthCalledWith(2, '/api/me/tasks/40', { method: 'PUT', body: form, token: TOKEN })
    expect(apiRequest).toHaveBeenNthCalledWith(3, '/api/me/tasks/40', { method: 'DELETE', token: TOKEN })
    expect(apiRequest).toHaveBeenNthCalledWith(4, '/api/me/tasks/40/complete', { method: 'POST', token: TOKEN })
  })

  it('never lets a value change the address', async () => {
    vi.mocked(apiRequest).mockResolvedValue(undefined)

    await getCalendar(TOKEN, { from: '2026-10-12&x=1', to: '../admin' })
    await deleteTask(TOKEN, Number.NaN)

    expect(apiRequest).toHaveBeenNthCalledWith(1, '/api/me/calendar?from=2026-10-12%26x%3D1&to=..%2Fadmin', { token: TOKEN })
    expect(apiRequest).toHaveBeenNthCalledWith(2, '/api/me/tasks/NaN', { method: 'DELETE', token: TOKEN })
  })
})
