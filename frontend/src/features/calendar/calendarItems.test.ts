import { describe, expect, it } from 'vitest'
import { activeSpans, addDays, eventKey, isItemProps, rangeOf, toCalendarItems } from './calendarItems'
import type { CalendarTask, GameEvent } from './types'

const bountiful: GameEvent = {
  id: 1,
  name: 'Bountiful Crescendo',
  type: 'DOUBLE_DROP',
  description: 'Ricompense doppie.',
  startDate: '2026-10-18',
  endDate: '2026-10-24',
  activeWeekdays: null,
  activeDates: ['2026-10-18', '2026-10-19', '2026-10-20'],
  rewards: [],
  activities: [],
  effect: { type: 'DROP_MULTIPLIER', sourceType: 'SIMULATION_TRAINING', multiplier: 2, dailyLimit: 3 },
}

const task: CalendarTask = {
  id: 40,
  origin: 'MANUAL',
  kind: 'CUSTOM',
  status: 'CONFIRMED',
  taskDate: '2026-10-19',
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

describe('addDays', () => {
  it('moves across the end of a month and of a year', () => {
    expect(addDays('2026-10-31', 1)).toBe('2026-11-01')
    expect(addDays('2026-12-31', 1)).toBe('2027-01-01')
    expect(addDays('2026-03-01', -1)).toBe('2026-02-28')
  })

  it('does not lose a day when the clocks change', () => {
    // In Italy summer time ends on 25 October 2026: the day still counts as one
    expect(addDays('2026-10-25', 1)).toBe('2026-10-26')
  })
})

describe('activeSpans', () => {
  it('joins the consecutive days, with the end the day after the last one', () => {
    expect(activeSpans(['2026-10-18', '2026-10-19', '2026-10-20'])).toEqual([
      { start: '2026-10-18', end: '2026-10-21' },
    ])
  })

  it('splits an event that is active only on some days (D96)', () => {
    // Monday and Wednesday: two pieces, nothing on Tuesday
    expect(activeSpans(['2026-10-19', '2026-10-21'])).toEqual([
      { start: '2026-10-19', end: '2026-10-20' },
      { start: '2026-10-21', end: '2026-10-22' },
    ])
  })

  it('keeps the order of the days even if they arrive mixed', () => {
    expect(activeSpans(['2026-10-24', '2026-10-17', '2026-10-18'])).toEqual([
      { start: '2026-10-17', end: '2026-10-19' },
      { start: '2026-10-24', end: '2026-10-25' },
    ])
  })

  it('is empty without active days', () => {
    expect(activeSpans([])).toEqual([])
  })
})

describe('toCalendarItems', () => {
  it('draws an event once for every piece of active days, before the tasks', () => {
    const weekends: GameEvent = { ...bountiful, id: 4, type: 'POINTS_SHOP', activeDates: ['2026-10-24', '2026-10-25', '2026-10-31'] }

    const items = toCalendarItems([weekends], [task])

    expect(items).toHaveLength(3)
    expect(items[0]).toMatchObject({
      title: 'Bountiful Crescendo',
      start: '2026-10-24',
      end: '2026-10-26',
      allDay: true,
      className: 'calendar-event calendar-event--points-shop',
      color: 'var(--rp-event-points-shop)',
      contrastColor: 'var(--rp-on-event)',
      extendedProps: { kind: 'event', key: eventKey(weekends), rank: 0 },
    })
    expect(items[1]).toMatchObject({ start: '2026-10-31', end: '2026-11-01' })
    expect(new Set(items.map((item) => item.id)).size).toBe(3)
  })

  it('shows a task on its day with its status written, not only coloured (D70)', () => {
    const [confirmed, done] = toCalendarItems([], [task, { ...task, id: 41, status: 'DONE', title: 'Raccolta' }])

    expect(confirmed).toMatchObject({
      id: 'task-40',
      title: 'Giro dei boss',
      start: '2026-10-19',
      allDay: true,
      className: 'calendar-task calendar-task--confirmed',
      extendedProps: { kind: 'task', taskId: 40, rank: 1 },
    })
    expect(done).toMatchObject({
      title: '✓ Raccolta',
      className: 'calendar-task calendar-task--done',
      color: 'var(--rp-task-done)',
    })
  })

  it('gives every type of event its own class', () => {
    const items = toCalendarItems(
      [bountiful, { ...bountiful, id: 2, type: 'COMBAT_STAGES' }],
      [],
    )

    expect(items.map((item) => item.className)).toEqual([
      'calendar-event calendar-event--double-drop',
      'calendar-event calendar-event--combat-stages',
    ])
  })
})

describe('eventKey', () => {
  it('tells apart two editions of the same event', () => {
    expect(eventKey(bountiful)).not.toBe(eventKey({ ...bountiful, startDate: '2026-11-01' }))
  })
})

describe('rangeOf', () => {
  it('turns the visible dates of the calendar into the days to ask, the last one included', () => {
    // FullCalendar gives the local midnight of the first day and of the day after the last one
    expect(rangeOf(new Date(2026, 9, 12), new Date(2026, 9, 19))).toEqual({ from: '2026-10-12', to: '2026-10-18' })
  })

  it('works across the change of the clocks', () => {
    expect(rangeOf(new Date(2026, 9, 19), new Date(2026, 9, 26))).toEqual({ from: '2026-10-19', to: '2026-10-25' })
  })
})

describe('isItemProps', () => {
  it('accepts only the data that the page itself put on an item', () => {
    for (const item of toCalendarItems([bountiful], [task])) {
      expect(isItemProps(item.extendedProps)).toBe(true)
    }
    expect(isItemProps({ kind: 'task', taskId: '40' })).toBe(false)
    expect(isItemProps({ kind: 'event' })).toBe(false)
    expect(isItemProps({ kind: 'other' })).toBe(false)
    expect(isItemProps(null)).toBe(false)
    expect(isItemProps('task')).toBe(false)
  })
})
