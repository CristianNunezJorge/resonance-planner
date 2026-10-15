import { describe, expect, it } from 'vitest'
import { gameRules } from '../../test/fixtures'
import { draftOf, formatPlayTime, isAscensionCap, isLowerThan, levelsOf, rankOf } from './levels'

// Level and ascension rank (D34, D104): rank 0 = levels 1-20, rank 1 = 20-40, rank 2 = only 40. The user picks a
// level and, only at a level where an ascension is needed, says whether it is done: the rank follows from the two.

describe('isAscensionCap', () => {
  it('is true only at the levels where an ascension is needed', () => {
    expect(isAscensionCap(20, gameRules)).toBe(true)
    expect(isAscensionCap(40, gameRules)).toBe(true)
    for (const level of [1, 19, 21, 39]) {
      expect(isAscensionCap(level, gameRules)).toBe(false)
    }
  })
})

describe('rankOf', () => {
  it.each([
    [1, false, 0],
    [19, false, 0],
    [20, false, 0],
    [20, true, 1],
    [21, false, 1],
    [39, true, 1],
    [40, false, 1],
    [40, true, 2],
  ])('level %i with the ascension done = %s is rank %i', (level, ascended, rank) => {
    expect(rankOf({ level, ascended }, gameRules)).toBe(rank)
  })

  it('ignores the ascension when the level is not one where it is needed', () => {
    expect(rankOf({ level: 5, ascended: true }, gameRules)).toBe(0)
    expect(rankOf({ level: 25, ascended: true }, gameRules)).toBe(1)
  })

  it('follows the caps of the game rules, not fixed numbers', () => {
    const raised = { ...gameRules, ascensionLevels: [20, 40, 60], maxLevel: 60 }

    expect(rankOf({ level: 41, ascended: false }, raised)).toBe(2)
    expect(rankOf({ level: 60, ascended: true }, raised)).toBe(3)
    expect(isAscensionCap(60, raised)).toBe(true)
  })
})

describe('isLowerThan', () => {
  it('compares the rank first and then the level', () => {
    expect(isLowerThan({ level: 25, ascended: false }, { level: 30, ascended: false }, gameRules)).toBe(true)
    // The same level 20, but the goal is to stay before the ascension that is already done
    expect(isLowerThan({ level: 20, ascended: false }, { level: 20, ascended: true }, gameRules)).toBe(true)
    // A higher level of a lower rank cannot happen with the picker, but the rank decides
    expect(isLowerThan({ level: 20, ascended: false }, { level: 25, ascended: false }, gameRules)).toBe(true)
  })

  it('accepts a goal equal to or above the present', () => {
    expect(isLowerThan({ level: 30, ascended: false }, { level: 30, ascended: false }, gameRules)).toBe(false)
    expect(isLowerThan({ level: 20, ascended: true }, { level: 20, ascended: false }, gameRules)).toBe(false)
    expect(isLowerThan({ level: 40, ascended: true }, { level: 1, ascended: false }, gameRules)).toBe(false)
  })
})

describe('levelsOf', () => {
  it('lists every level from 1 to the maximum of the game', () => {
    const levels = levelsOf(gameRules)

    expect(levels).toHaveLength(40)
    expect(levels[0]).toBe(1)
    expect(levels.at(-1)).toBe(40)
  })
})

describe('formatPlayTime', () => {
  it.each([
    [30, '30 minuti'],
    [45, '45 minuti'],
    [60, '1 ora'],
    [120, '2 ore'],
    [90, '1 ora e 30 minuti'],
    [150, '2 ore e 30 minuti'],
  ])('writes %i minutes as "%s"', (minutes, text) => {
    expect(formatPlayTime(minutes)).toBe(text)
  })
})

describe('draftOf', () => {
  it('ticks the ascension only at a cap whose ascension is done', () => {
    expect(draftOf(1, 0, gameRules)).toEqual({ level: 1, ascended: false })
    expect(draftOf(20, 0, gameRules)).toEqual({ level: 20, ascended: false })
    expect(draftOf(20, 1, gameRules)).toEqual({ level: 20, ascended: true })
    expect(draftOf(30, 1, gameRules)).toEqual({ level: 30, ascended: false })
    expect(draftOf(40, 1, gameRules)).toEqual({ level: 40, ascended: false })
    expect(draftOf(40, 2, gameRules)).toEqual({ level: 40, ascended: true })
  })

  it('gives back the same rank through rankOf', () => {
    for (const [level, rank] of [[1, 0], [20, 0], [20, 1], [35, 1], [40, 1], [40, 2]]) {
      expect(rankOf(draftOf(level, rank, gameRules), gameRules)).toBe(rank)
    }
  })
})
