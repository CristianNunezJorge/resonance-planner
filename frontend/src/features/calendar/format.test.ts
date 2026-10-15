import { describe, expect, it } from 'vitest'
import { catalogMaterials } from '../../test/fixtures'
import {
  activityText,
  addedText,
  dayText,
  editionText,
  effectText,
  weekdaysText,
} from './format'

describe('weekdaysText', () => {
  it('says every day when the event has no chosen days', () => {
    expect(weekdaysText(null)).toBe('tutti i giorni')
  })

  it('lists the days from Monday, in short form', () => {
    expect(weekdaysText(['FRIDAY', 'MONDAY', 'WEDNESDAY'])).toBe('lun, mer, ven')
    expect(weekdaysText(['SUNDAY', 'SATURDAY'])).toBe('sab, dom')
  })
})

describe('dayText and editionText', () => {
  it('writes a day with the name of the weekday', () => {
    expect(dayText('2026-10-16')).toBe('venerdì 16 ottobre 2026')
  })

  it('writes the dates of an edition', () => {
    expect(editionText('2026-10-18', '2026-10-24')).toBe('dal 18 ottobre al 24 ottobre 2026')
    expect(editionText('2026-12-28', '2027-01-03')).toBe('dal 28 dicembre 2026 al 3 gennaio 2027')
    expect(editionText('2026-10-18', '2026-10-18')).toBe('il 18 ottobre 2026')
  })
})

describe('effectText', () => {
  it('describes the multiplier, the type of source and the daily limit', () => {
    expect(effectText({ type: 'DROP_MULTIPLIER', sourceType: 'SIMULATION_TRAINING', multiplier: 2, dailyLimit: 3 }))
      .toBe('Ricompense ×2 in Simulation Training, per le prime 3 run del giorno')
    expect(effectText({ type: 'DROP_MULTIPLIER', sourceType: 'BOSS_CHALLENGE', multiplier: 1.5, dailyLimit: null }))
      .toBe('Ricompense ×1,5 in Boss Challenge, per tutte le run')
    expect(effectText({ type: 'DROP_MULTIPLIER', sourceType: 'OVERWORLD_COLLECTION', multiplier: 2, dailyLimit: 1 }))
      .toBe('Ricompense ×2 nella raccolta, per la prima run del giorno')
  })
})

describe('activityText', () => {
  it('says how long, how often and from which day', () => {
    expect(activityText({ title: 'Drill 1', minutes: 15, frequency: 'ONCE', unlockDay: null }))
      .toBe('Drill 1 · 15 minuti · una volta')
    expect(activityText({ title: 'Obiettivi', minutes: 75, frequency: 'DAILY', unlockDay: 3 }))
      .toBe('Obiettivi · 1 ora e 15 minuti · ogni giorno · dal 3° giorno')
  })
})

describe('addedText', () => {
  it('lists what a done task added to the inventory, with the names of the catalog', () => {
    expect(
      addedText(
        [
          { materialId: 'elegy-tacet-core', quantity: 4 },
          { materialId: 'shell-credit', quantity: 28800 },
        ],
        catalogMaterials,
      ),
    ).toBe('Aggiunti all’inventario: 4 Elegy Tacet Core, 28.800 Shell Credit.')
  })

  it('uses the id when the material is not in the catalog', () => {
    expect(addedText([{ materialId: 'mystery', quantity: 1 }], [])).toBe('Aggiunti all’inventario: 1 mystery.')
  })

  it('is empty when nothing was added', () => {
    expect(addedText([], catalogMaterials)).toBe('')
  })
})
