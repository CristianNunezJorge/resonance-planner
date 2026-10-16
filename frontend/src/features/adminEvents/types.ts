import type { EventActivity, EventEffect, EventType, FarmingSourceType, Weekday } from '../calendar/types'

// Shapes of the admin events API (/api/admin/events, D14, D114)

export type ScheduleType = 'FIXED' | 'RECURRING'

/** FIXED has the two dates, RECURRING the three numbers counted from the anchor date (D29); the others are null. */
export interface AdminSchedule {
  type: ScheduleType
  startDate: string | null
  endDate: string | null
  startOffsetDays: number | null
  durationDays: number | null
  repeatEveryDays: number | null
}

/** An event as stored: code is the id of events.json for the demo events, null for those made by the admin (D39). */
export interface AdminEvent {
  id: number
  code: string | null
  name: string
  type: EventType
  description: string
  schedule: AdminSchedule
  // null = every day (D96)
  activeWeekdays: Weekday[] | null
  rewards: string[]
  activities: EventActivity[]
  effect: EventEffect | null
}

/**
 * The body of POST and PUT: the same fields without id and code. An empty number is sent as null, and the server
 * says what is needed (D122).
 */
export type EventRequest = Omit<AdminEvent, 'id' | 'code' | 'activities'> & {
  activities: (Omit<EventActivity, 'minutes'> & { minutes: number | null })[]
}

/** One activity of the form, as typed. */
export interface ActivityDraft {
  title: string
  minutes: string
  frequency: EventActivity['frequency']
  unlockDay: string
}

/**
 * The event form as the admin types it (D122): numbers stay text until they are checked, so a half-typed value is
 * never lost or changed. The effect is used only for DOUBLE_DROP.
 */
export interface EventDraft {
  name: string
  type: EventType
  description: string
  scheduleType: ScheduleType
  startDate: string
  endDate: string
  startOffsetDays: string
  durationDays: string
  repeatEveryDays: string
  weekdays: Weekday[]
  rewards: string[]
  activities: ActivityDraft[]
  effect: { sourceType: FarmingSourceType; multiplier: string; dailyLimit: string }
}
