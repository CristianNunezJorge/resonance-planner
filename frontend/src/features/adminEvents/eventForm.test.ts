import { describe, expect, it } from 'vitest'
import { ALL_WEEKDAYS, draftOf, emptyDraft, scheduleText, toRequest } from './eventForm'
import type { AdminEvent, EventDraft } from './types'

const bountiful: AdminEvent = {
  id: 1,
  code: 'bountiful-crescendo',
  name: 'Bountiful Crescendo',
  type: 'DOUBLE_DROP',
  description: 'Ricompense doppie.',
  schedule: { type: 'RECURRING', startDate: null, endDate: null, startOffsetDays: 3, durationDays: 7, repeatEveryDays: 14 },
  activeWeekdays: null,
  rewards: ['Ricompense doppie'],
  activities: [],
  effect: { type: 'DROP_MULTIPLIER', sourceType: 'SIMULATION_TRAINING', multiplier: 2, dailyLimit: 3 },
}

const tournament: AdminEvent = {
  id: 4,
  code: null,
  name: 'Torneo del weekend',
  type: 'COMBAT_STAGES',
  description: 'Solo sabato e domenica.',
  schedule: { type: 'FIXED', startDate: '2026-10-12', endDate: '2026-10-18', startOffsetDays: null, durationDays: null, repeatEveryDays: null },
  activeWeekdays: ['SATURDAY', 'SUNDAY'],
  rewards: ['Astrite'],
  activities: [{ title: 'Livello 1', minutes: 20, frequency: 'ONCE', unlockDay: 6 }],
  effect: null,
}

function filled(overrides: Partial<EventDraft> = {}): EventDraft {
  return { ...draftOf(tournament), ...overrides }
}

describe('emptyDraft', () => {
  it('starts with every day of the week checked and dates fixed (P7)', () => {
    const draft = emptyDraft()

    expect(draft.weekdays).toEqual(ALL_WEEKDAYS)
    expect(draft.scheduleType).toBe('FIXED')
    expect(draft.rewards).toEqual([])
    expect(draft.activities).toEqual([])
    expect(draft.effect).toEqual({ sourceType: 'SIMULATION_TRAINING', multiplier: '2', dailyLimit: '' })
  })
})

describe('draftOf and toRequest', () => {
  it('give back the same event, for fixed dates', () => {
    expect(toRequest(draftOf(tournament))).toEqual({
      request: {
        name: 'Torneo del weekend',
        type: 'COMBAT_STAGES',
        description: 'Solo sabato e domenica.',
        schedule: { type: 'FIXED', startDate: '2026-10-12', endDate: '2026-10-18', startOffsetDays: null, durationDays: null, repeatEveryDays: null },
        activeWeekdays: ['SATURDAY', 'SUNDAY'],
        rewards: ['Astrite'],
        activities: [{ title: 'Livello 1', minutes: 20, frequency: 'ONCE', unlockDay: 6 }],
        effect: null,
      },
    })
  })

  it('give back the same event, for a recurring one with its effect', () => {
    const draft = draftOf(bountiful)

    expect(draft.weekdays).toEqual(ALL_WEEKDAYS)
    expect(draft.effect).toEqual({ sourceType: 'SIMULATION_TRAINING', multiplier: '2', dailyLimit: '3' })
    expect(toRequest(draft)).toEqual({
      request: {
        name: 'Bountiful Crescendo',
        type: 'DOUBLE_DROP',
        description: 'Ricompense doppie.',
        schedule: { type: 'RECURRING', startDate: null, endDate: null, startOffsetDays: 3, durationDays: 7, repeatEveryDays: 14 },
        activeWeekdays: ALL_WEEKDAYS,
        rewards: ['Ricompense doppie'],
        activities: [],
        effect: { type: 'DROP_MULTIPLIER', sourceType: 'SIMULATION_TRAINING', multiplier: 2, dailyLimit: 3 },
      },
    })
  })

  it('send only the values of the kind of dates chosen', () => {
    const result = toRequest(filled({ scheduleType: 'RECURRING', startOffsetDays: '-2', durationDays: '39', repeatEveryDays: '42' }))

    expect(result).toMatchObject({
      request: {
        schedule: { type: 'RECURRING', startDate: null, endDate: null, startOffsetDays: -2, durationDays: 39, repeatEveryDays: 42 },
      },
    })
  })

  it('send the effect only for double rewards, and an empty daily limit as none', () => {
    expect(toRequest(filled({ type: 'POINTS_SHOP' }))).toMatchObject({ request: { effect: null } })
    expect(
      toRequest(filled({ type: 'DOUBLE_DROP', effect: { sourceType: 'BOSS_CHALLENGE', multiplier: '1,5', dailyLimit: ' ' } })),
    ).toMatchObject({
      request: { effect: { type: 'DROP_MULTIPLIER', sourceType: 'BOSS_CHALLENGE', multiplier: 1.5, dailyLimit: null } },
    })
  })

  it('send empty numbers as missing, for the server to say what is needed', () => {
    const result = toRequest(
      filled({
        scheduleType: 'RECURRING',
        startOffsetDays: '',
        durationDays: ' ',
        repeatEveryDays: '',
        activities: [{ title: 'Drill', minutes: '', frequency: 'DAILY', unlockDay: '' }],
      }),
    )

    expect(result).toMatchObject({
      request: {
        schedule: { startOffsetDays: null, durationDays: null, repeatEveryDays: null },
        activities: [{ title: 'Drill', minutes: null, frequency: 'DAILY', unlockDay: null }],
      },
    })
  })

  it('send empty dates as missing', () => {
    expect(toRequest(filled({ startDate: '', endDate: '' }))).toMatchObject({
      request: { schedule: { startDate: null, endDate: null } },
    })
  })
})

describe('toRequest checks', () => {
  it('refuse numbers that are not whole, with the names of the fields of the server', () => {
    const result = toRequest(
      filled({
        scheduleType: 'RECURRING',
        startOffsetDays: 'tre',
        durationDays: '7.5',
        repeatEveryDays: '1e3',
        activities: [
          { title: 'A', minutes: '20', frequency: 'ONCE', unlockDay: '' },
          { title: 'B', minutes: '15 min', frequency: 'ONCE', unlockDay: '-1' },
        ],
      }),
    )

    expect(result).toEqual({
      errors: {
        'schedule.startOffsetDays': 'Inserisci un numero intero.',
        'schedule.durationDays': 'Inserisci un numero intero.',
        'schedule.repeatEveryDays': 'Inserisci un numero intero.',
        'activities[1].minutes': 'Inserisci un numero intero.',
        'activities[1].unlockDay': 'Inserisci un numero intero.',
      },
    })
  })

  it('accept a negative offset, which starts before the anchor date', () => {
    expect(toRequest(filled({ scheduleType: 'RECURRING', startOffsetDays: '-10', durationDays: '44', repeatEveryDays: '49' })))
      .toHaveProperty('request')
  })

  it('refuse a multiplier that is not a number with at most two decimals', () => {
    for (const multiplier of ['due', '1,555', '2..0', '']) {
      expect(toRequest(filled({ type: 'DOUBLE_DROP', effect: { sourceType: 'SIMULATION_TRAINING', multiplier, dailyLimit: '3' } })))
        .toEqual({ errors: { 'effect.multiplier': 'Inserisci un numero con al massimo due decimali, per esempio 2 o 1,5.' } })
    }
  })

  it('refuse a daily limit that is not whole', () => {
    expect(
      toRequest(filled({ type: 'DOUBLE_DROP', effect: { sourceType: 'SIMULATION_TRAINING', multiplier: '2', dailyLimit: '2,5' } })),
    ).toEqual({ errors: { 'effect.dailyLimit': 'Inserisci un numero intero.' } })
  })

  it('needs at least one day of the week', () => {
    expect(toRequest(filled({ weekdays: [] }))).toEqual({ errors: { activeWeekdays: 'Scegli almeno un giorno.' } })
  })

  it('ignores the effect fields when the type has no effect', () => {
    expect(
      toRequest(filled({ type: 'COMBAT_STAGES', effect: { sourceType: 'SIMULATION_TRAINING', multiplier: 'x', dailyLimit: 'y' } })),
    ).toHaveProperty('request')
  })
})

describe('scheduleText', () => {
  it('writes the dates of a fixed event and the rhythm of a recurring one', () => {
    expect(scheduleText(tournament.schedule)).toBe('dal 12/10/2026 al 18/10/2026')
    expect(scheduleText(bountiful.schedule)).toBe('ogni 14 giorni, per 7 giorni')
    expect(scheduleText({ ...bountiful.schedule, durationDays: 1, repeatEveryDays: 7 })).toBe('ogni 7 giorni, per 1 giorno')
  })
})
