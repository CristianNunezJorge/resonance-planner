import type { Weekday } from '../calendar/types'
import type { AdminEvent, AdminSchedule, EventDraft, EventRequest } from './types'

// The event form of the admin (D114, D122): from an event to the text of the form and back to the body of the
// request. The page checks only what it must to send a valid body (numbers that are numbers, at least one day);
// every other rule is the server's, and its messages come back under the field with the same name.

export const ALL_WEEKDAYS: Weekday[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']

const NOT_WHOLE = 'Inserisci un numero intero.'
const NOT_MULTIPLIER = 'Inserisci un numero con al massimo due decimali, per esempio 2 o 1,5.'
// At most nine digits, so the number always fits the integers of the server; only the offset can be negative
const WHOLE = /^\d{1,9}$/
const SIGNED_WHOLE = /^-?\d{1,9}$/
// "2", "1,5" or "1.25": the server accepts at most two decimals and at most 10
const MULTIPLIER = /^\d{1,2}([.,]\d{1,2})?$/

const dateFormat = new Intl.DateTimeFormat('it-IT', { day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'UTC' })
const decimalFormat = new Intl.NumberFormat('it-IT', { maximumFractionDigits: 2, useGrouping: false })

export function emptyDraft(): EventDraft {
  return {
    name: '',
    type: 'COMBAT_STAGES',
    description: '',
    scheduleType: 'FIXED',
    startDate: '',
    endDate: '',
    startOffsetDays: '',
    durationDays: '',
    repeatEveryDays: '',
    weekdays: [...ALL_WEEKDAYS],
    rewards: [],
    activities: [],
    effect: { sourceType: 'SIMULATION_TRAINING', multiplier: '2', dailyLimit: '' },
  }
}

function text(value: number | null): string {
  return value === null ? '' : String(value)
}

/** The form of an event that already exists: no chosen days (null) means every box checked (D96). */
export function draftOf(event: AdminEvent): EventDraft {
  const { schedule, effect } = event
  return {
    name: event.name,
    type: event.type,
    description: event.description,
    scheduleType: schedule.type,
    startDate: schedule.startDate ?? '',
    endDate: schedule.endDate ?? '',
    startOffsetDays: text(schedule.startOffsetDays),
    durationDays: text(schedule.durationDays),
    repeatEveryDays: text(schedule.repeatEveryDays),
    weekdays: event.activeWeekdays ?? [...ALL_WEEKDAYS],
    rewards: [...event.rewards],
    activities: event.activities.map((activity) => ({
      title: activity.title,
      minutes: String(activity.minutes),
      frequency: activity.frequency,
      unlockDay: text(activity.unlockDay),
    })),
    effect: effect
      ? { sourceType: effect.sourceType, multiplier: decimalFormat.format(effect.multiplier), dailyLimit: text(effect.dailyLimit) }
      : emptyDraft().effect,
  }
}

/**
 * The body of POST or PUT, or the fields typed in a way that cannot be sent, with the names the server uses
 * (schedule.durationDays, activities[1].minutes, effect.multiplier). An empty number is sent as missing: the server
 * says what is needed.
 */
export function toRequest(draft: EventDraft): { request: EventRequest } | { errors: Record<string, string> } {
  const errors: Record<string, string> = {}
  const whole = (value: string, field: string, pattern = WHOLE): number | null => {
    const trimmed = value.trim()
    if (trimmed === '') {
      return null
    }
    if (!pattern.test(trimmed)) {
      errors[field] = NOT_WHOLE
      return null
    }
    return Number(trimmed)
  }

  const isFixed = draft.scheduleType === 'FIXED'
  const schedule: AdminSchedule = {
    type: draft.scheduleType,
    startDate: isFixed && draft.startDate !== '' ? draft.startDate : null,
    endDate: isFixed && draft.endDate !== '' ? draft.endDate : null,
    startOffsetDays: isFixed ? null : whole(draft.startOffsetDays, 'schedule.startOffsetDays', SIGNED_WHOLE),
    durationDays: isFixed ? null : whole(draft.durationDays, 'schedule.durationDays'),
    repeatEveryDays: isFixed ? null : whole(draft.repeatEveryDays, 'schedule.repeatEveryDays'),
  }
  if (draft.weekdays.length === 0) {
    errors.activeWeekdays = 'Scegli almeno un giorno.'
  }
  const activities = draft.activities.map((activity, index) => ({
    title: activity.title,
    minutes: whole(activity.minutes, `activities[${index}].minutes`),
    frequency: activity.frequency,
    unlockDay: whole(activity.unlockDay, `activities[${index}].unlockDay`),
  }))

  let effect: EventRequest['effect'] = null
  if (draft.type === 'DOUBLE_DROP') {
    const multiplier = draft.effect.multiplier.trim()
    if (!MULTIPLIER.test(multiplier)) {
      errors['effect.multiplier'] = NOT_MULTIPLIER
    }
    effect = {
      type: 'DROP_MULTIPLIER',
      sourceType: draft.effect.sourceType,
      multiplier: Number(multiplier.replace(',', '.')),
      dailyLimit: whole(draft.effect.dailyLimit, 'effect.dailyLimit'),
    }
  }

  if (Object.keys(errors).length > 0) {
    return { errors }
  }
  return {
    request: {
      name: draft.name,
      type: draft.type,
      description: draft.description,
      schedule,
      activeWeekdays: ALL_WEEKDAYS.filter((day) => draft.weekdays.includes(day)),
      rewards: draft.rewards,
      activities,
      effect,
    },
  }
}

function dateText(day: string | null): string {
  return day ? dateFormat.format(new Date(`${day}T00:00:00Z`)) : '—'
}

function daysText(days: number | null): string {
  return days === 1 ? '1 giorno' : `${days ?? '—'} giorni`
}

/** "dal 12/10/2026 al 18/10/2026" or "ogni 14 giorni, per 7 giorni", for the table of the events. */
export function scheduleText(schedule: AdminSchedule): string {
  if (schedule.type === 'FIXED') {
    return `dal ${dateText(schedule.startDate)} al ${dateText(schedule.endDate)}`
  }
  return `ogni ${daysText(schedule.repeatEveryDays)}, per ${daysText(schedule.durationDays)}`
}
