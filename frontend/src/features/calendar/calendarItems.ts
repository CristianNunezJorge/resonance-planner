import type { EventInput } from '@fullcalendar/react'
import type { CalendarTask, DateRange, EventType, GameEvent } from './types'

// What FullCalendar draws (D120): every edition of an event as one piece for each run of consecutive active days,
// then the tasks of the user. The days are strings like "2026-10-18": their arithmetic is done in UTC, so a change
// of the clocks never moves a day.

/** A run of whole days, with the end on the day after the last one (as FullCalendar wants all-day items). */
export interface Span {
  start: string
  end: string
}

/** The data of an item, read back when it is clicked; rank keeps the events above the tasks of the same day. */
export type ItemProps = { kind: 'event'; key: string; rank: 0 } | { kind: 'task'; taskId: number; rank: 1 }

/** The data of a clicked item, checked: FullCalendar gives them back as a plain object of any shape. */
export function isItemProps(value: unknown): value is ItemProps {
  if (typeof value !== 'object' || value === null) {
    return false
  }
  const props = value as Record<string, unknown>
  return (
    (props.kind === 'event' && typeof props.key === 'string') ||
    (props.kind === 'task' && typeof props.taskId === 'number')
  )
}

// The suffix of the class and of the colour of each type of event
const EVENT_SUFFIX: Record<EventType, string> = {
  COMBAT_STAGES: 'combat-stages',
  DOUBLE_DROP: 'double-drop',
  POINTS_SHOP: 'points-shop',
}

export function addDays(day: string, days: number): string {
  const date = new Date(`${day}T00:00:00Z`)
  date.setUTCDate(date.getUTCDate() + days)
  return date.toISOString().slice(0, 10)
}

/** The active days grouped in runs of consecutive days (D96): an event active on Monday and Wednesday is two pieces. */
export function activeSpans(activeDates: string[]): Span[] {
  const spans: Span[] = []
  for (const day of [...activeDates].sort()) {
    const last = spans.at(-1)
    if (last && last.end === day) {
      spans[spans.length - 1] = { start: last.start, end: addDays(day, 1) }
    } else {
      spans.push({ start: day, end: addDays(day, 1) })
    }
  }
  return spans
}

/** An edition is an event and its first day: the same event can come back in the same range (D113). */
export function eventKey(event: Pick<GameEvent, 'id' | 'startDate'>): string {
  return `${event.id}:${event.startDate}`
}

export function toCalendarItems(events: GameEvent[], tasks: CalendarTask[]): EventInput[] {
  const eventItems = events.flatMap((event) =>
    activeSpans(event.activeDates).map((span, index) => ({
      id: `event-${eventKey(event)}-${index}`,
      title: event.name,
      start: span.start,
      end: span.end,
      allDay: true,
      interactive: true,
      className: `calendar-event calendar-event--${EVENT_SUFFIX[event.type]}`,
      // Colours of our theme (styles/_calendar.scss), with a dark text that keeps the contrast AA
      color: `var(--rp-event-${EVENT_SUFFIX[event.type]})`,
      contrastColor: 'var(--rp-on-event)',
      extendedProps: { kind: 'event', key: eventKey(event), rank: 0 } satisfies ItemProps,
    })),
  )
  const taskItems = tasks.map((task) => {
    const isDone = task.status === 'DONE'
    return {
      id: `task-${task.id}`,
      // The status is written, not only coloured (D70)
      title: isDone ? `✓ ${task.title}` : task.title,
      start: task.taskDate,
      allDay: true,
      interactive: true,
      className: `calendar-task calendar-task--${isDone ? 'done' : 'confirmed'}`,
      color: isDone ? 'var(--rp-task-done)' : 'var(--rp-task)',
      contrastColor: isDone ? 'var(--rp-on-task-done)' : 'var(--rp-on-event)',
      extendedProps: { kind: 'task', taskId: task.id, rank: 1 } satisfies ItemProps,
    }
  })
  return [...eventItems, ...taskItems]
}

// FullCalendar gives the local midnight of the first visible day and of the day after the last one
function localDay(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

/** The days to ask the server for: from the first visible day to the last one, both included. */
export function rangeOf(start: Date, end: Date): DateRange {
  return { from: localDay(start), to: addDays(localDay(end), -1) }
}
