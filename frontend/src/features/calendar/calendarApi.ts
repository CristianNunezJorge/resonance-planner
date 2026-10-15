import { apiRequest } from '../../api/client'
import type { CalendarData, CalendarTask, Completion, DateRange, TaskForm } from './types'

// The calendar of the user of the token (D44, D51). Values go into the address encoded, so they can never change it.

function taskPath(id: number): string {
  return `/api/me/tasks/${encodeURIComponent(String(id))}`
}

export function getCalendar(token: string, range: DateRange): Promise<CalendarData> {
  const query = new URLSearchParams({ from: range.from, to: range.to })
  return apiRequest<CalendarData>(`/api/me/calendar?${query.toString()}`, { token })
}

export function createTask(token: string, form: TaskForm): Promise<CalendarTask> {
  return apiRequest<CalendarTask>('/api/me/tasks', { method: 'POST', body: form, token })
}

export function updateTask(token: string, id: number, form: TaskForm): Promise<CalendarTask> {
  return apiRequest<CalendarTask>(taskPath(id), { method: 'PUT', body: form, token })
}

export function deleteTask(token: string, id: number): Promise<void> {
  return apiRequest<void>(taskPath(id), { method: 'DELETE', token })
}

export function completeTask(token: string, id: number): Promise<Completion> {
  return apiRequest<Completion>(`${taskPath(id)}/complete`, { method: 'POST', token })
}
