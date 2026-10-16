import { apiRequest } from '../../api/client'
import type { AdminEvent, EventRequest } from './types'

// The events as the admin manages them (D14, D114); the server checks the role of the token every time (D102)

function eventPath(id: number): string {
  return `/api/admin/events/${encodeURIComponent(String(id))}`
}

export function listEvents(token: string): Promise<AdminEvent[]> {
  return apiRequest<AdminEvent[]>('/api/admin/events', { token })
}

export function createEvent(token: string, event: EventRequest): Promise<AdminEvent> {
  return apiRequest<AdminEvent>('/api/admin/events', { method: 'POST', body: event, token })
}

export function updateEvent(token: string, id: number, event: EventRequest): Promise<AdminEvent> {
  return apiRequest<AdminEvent>(eventPath(id), { method: 'PUT', body: event, token })
}

export function deleteEvent(token: string, id: number): Promise<void> {
  return apiRequest<void>(eventPath(id), { method: 'DELETE', token })
}
