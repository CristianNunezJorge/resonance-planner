import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiRequest } from '../../api/client'
import { createEvent, deleteEvent, listEvents, updateEvent } from './adminEventsApi'
import type { EventRequest } from './types'

vi.mock('../../api/client', () => ({ apiRequest: vi.fn() }))

const TOKEN = 'admin.jwt.token'
const event: EventRequest = {
  name: 'Torneo',
  type: 'COMBAT_STAGES',
  description: 'Prova.',
  schedule: { type: 'FIXED', startDate: '2026-10-12', endDate: '2026-10-18', startOffsetDays: null, durationDays: null, repeatEveryDays: null },
  activeWeekdays: ['SATURDAY'],
  rewards: [],
  activities: [],
  effect: null,
}

describe('adminEventsApi', () => {
  afterEach(() => {
    vi.mocked(apiRequest).mockReset()
  })

  it('lists, creates, replaces and deletes the events', async () => {
    vi.mocked(apiRequest).mockResolvedValue(undefined)

    await listEvents(TOKEN)
    await createEvent(TOKEN, event)
    await updateEvent(TOKEN, 4, event)
    await deleteEvent(TOKEN, 4)

    expect(apiRequest).toHaveBeenNthCalledWith(1, '/api/admin/events', { token: TOKEN })
    expect(apiRequest).toHaveBeenNthCalledWith(2, '/api/admin/events', { method: 'POST', body: event, token: TOKEN })
    expect(apiRequest).toHaveBeenNthCalledWith(3, '/api/admin/events/4', { method: 'PUT', body: event, token: TOKEN })
    expect(apiRequest).toHaveBeenNthCalledWith(4, '/api/admin/events/4', { method: 'DELETE', token: TOKEN })
  })
})
