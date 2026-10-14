import type { GameRules } from '../catalog/types'
import type { LevelDraft } from './types'

// Level and ascension rank (D34, D104). The caps (20 and 40) come from the game rules: rank 0 is the levels up to the
// first cap, rank 1 up to the second, and so on; the cap itself exists both before and after its ascension.

/** True at the levels where an ascension is needed: only there the user is asked whether it is done. */
export function isAscensionCap(level: number, rules: GameRules): boolean {
  return rules.ascensionLevels.includes(level)
}

/** The rank of a level: the caps that are below it, plus one if it sits on a cap whose ascension is done. */
export function rankOf({ level, ascended }: LevelDraft, rules: GameRules): number {
  const capsBelow = rules.ascensionLevels.filter((cap) => cap < level).length
  return capsBelow + (ascended && isAscensionCap(level, rules) ? 1 : 0)
}

/** A goal is lower than the present when its rank is lower, or the rank is the same and the level is lower. */
export function isLowerThan(target: LevelDraft, current: LevelDraft, rules: GameRules): boolean {
  const targetRank = rankOf(target, rules)
  const currentRank = rankOf(current, rules)
  return targetRank < currentRank || (targetRank === currentRank && target.level < current.level)
}

/** Every level of the game, from 1 to the maximum. */
export function levelsOf(rules: GameRules): number[] {
  return Array.from({ length: rules.maxLevel }, (_, index) => index + 1)
}

const MINUTES_PER_HOUR = 60

/** 30 → "30 minuti", 60 → "1 ora", 120 → "2 ore", 90 → "1 ora e 30 minuti". */
export function formatPlayTime(minutes: number): string {
  const hours = Math.floor(minutes / MINUTES_PER_HOUR)
  const rest = minutes % MINUTES_PER_HOUR
  const restText = `${rest} ${rest === 1 ? 'minuto' : 'minuti'}`
  if (hours === 0) {
    return restText
  }
  const hoursText = hours === 1 ? '1 ora' : `${hours} ore`
  return rest === 0 ? hoursText : `${hoursText} e ${restText}`
}
