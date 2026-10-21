import { apiDownload, apiRequest } from '../../api/client'
import type { CalendarData, CalendarTask, Completion, DateRange, Proposal, TaskForm } from './types'

// The calendar of the user of the token (D44, D51). Values go into the address encoded, so they can never change it.

const EXCEL_TYPE = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'

function taskPath(id: number): string {
  return `/api/me/tasks/${encodeURIComponent(String(id))}`
}

function proposalPath(id: number): string {
  return `/api/me/proposals/${encodeURIComponent(String(id))}`
}

export function getCalendar(token: string, range: DateRange): Promise<CalendarData> {
  const query = new URLSearchParams({ from: range.from, to: range.to })
  return apiRequest<CalendarData>(`/api/me/calendar?${query.toString()}`, { token })
}

/** The plan of a range as an Excel file (D133): the confirmed and done tasks and the events, without the proposal. */
export function exportPlan(token: string, range: DateRange): Promise<Blob> {
  const query = new URLSearchParams({ from: range.from, to: range.to })
  return apiDownload(`/api/me/calendar/export?${query.toString()}`, EXCEL_TYPE, { token })
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

/** The basic plan without AI (D64): it becomes the pending proposal, in place of the one before. */
export function createBasicPlan(token: string): Promise<Proposal> {
  return apiRequest<Proposal>('/api/me/proposals/basic', { method: 'POST', token })
}

export function confirmProposal(token: string, id: number): Promise<Proposal> {
  return apiRequest<Proposal>(`${proposalPath(id)}/confirm`, { method: 'POST', token })
}

export function rejectProposal(token: string, id: number): Promise<Proposal> {
  return apiRequest<Proposal>(`${proposalPath(id)}/reject`, { method: 'POST', token })
}
