import { describe, expect, it } from 'vitest'
import { gameRules } from '../../test/fixtures'
import { describeGoal, formatDays, formatNeed, formatNumber, missingSummary } from './format'

describe('roster format', () => {
  it('writes numbers the Italian way', () => {
    expect(formatNumber(208800)).toBe('208.800')
    expect(formatNumber(10000)).toBe('10.000')
    expect(formatNumber(4)).toBe('4')
  })

  it('writes what is missing of a material, and the EXP in points', () => {
    expect(formatNeed({ materialId: 'elegy-tacet-core', name: 'Elegy Tacet Core', missing: 3 })).toBe('3 Elegy Tacet Core')
    expect(formatNeed({ materialId: 'character-exp', name: 'EXP personaggio', missing: 208800 })).toBe('208.800 EXP')
    expect(formatNeed({ materialId: 'shell-credit', name: 'Shell Credit', missing: 88080 })).toBe('88.080 Shell Credit')
  })

  it('lists only the materials that are missing', () => {
    const materials = [
      { materialId: 'elegy-tacet-core', name: 'Elegy Tacet Core', missing: 3 },
      { materialId: 'lf-howler-core', name: 'LF Howler Core', missing: 0 },
      { materialId: 'character-exp', name: 'EXP personaggio', missing: 1000 },
    ]

    // Italian writes four digits without the dot (1000), five or more with it (10.000)
    expect(missingSummary(materials)).toBe('3 Elegy Tacet Core, 1000 EXP')
    expect(missingSummary([])).toBe('')
  })

  it('writes days in the singular and in the plural', () => {
    expect(formatDays(1)).toBe('1 giorno')
    expect(formatDays(3)).toBe('3 giorni')
  })

  it('describes the goal with the ascension when the goal includes it', () => {
    const goal = (currentLevel: number, currentRank: number, targetLevel: number, targetRank: number) =>
      describeGoal({ currentLevel, currentRank, targetLevel, targetRank }, gameRules)

    expect(goal(1, 0, 40, 2)).toBe('Livello 1 → 40, con l’ascensione al 40')
    expect(goal(1, 0, 40, 1)).toBe('Livello 1 → 40')
    expect(goal(20, 0, 20, 1)).toBe('Livello 20, manca l’ascensione al 20')
    expect(goal(40, 1, 40, 2)).toBe('Livello 40, manca l’ascensione al 40')
    expect(goal(40, 2, 40, 2)).toBe('Livello 40, ascensione al 40 già fatta')
    expect(goal(25, 1, 25, 1)).toBe('Livello 25')
  })
})
