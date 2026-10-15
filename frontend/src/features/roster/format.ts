import type { GameRules } from '../catalog/types'
import { draftOf } from '../progress/levels'
import type { MaterialNeed, Progress } from './types'

// Texts of the roster page. The numbers come from the server (D3): here they are only written

const EXP_LINE = 'character-exp'
const numberFormat = new Intl.NumberFormat('it-IT')

/** 208800 → "208.800". */
export function formatNumber(value: number): string {
  return numberFormat.format(value)
}

/** "3 Elegy Tacet Core"; the EXP is a sum of potions, written in points: "208.800 EXP" (D112). */
export function formatNeed({ materialId, name, missing }: Pick<MaterialNeed, 'materialId' | 'name' | 'missing'>): string {
  return materialId === EXP_LINE ? `${formatNumber(missing)} EXP` : `${formatNumber(missing)} ${name}`
}

/** The materials still missing, in the order of the calculation; empty when nothing is missing. */
export function missingSummary(materials: Pick<MaterialNeed, 'materialId' | 'name' | 'missing'>[]): string {
  return materials
    .filter((line) => line.missing > 0)
    .map(formatNeed)
    .join(', ')
}

export function formatDays(days: number): string {
  return days === 1 ? '1 giorno' : `${days} giorni`
}

/** "Livello 1 → 40, con l’ascensione al 40": the rank is never shown, only the ascension it means (D111). */
export function describeGoal(progress: Progress, rules: GameRules): string {
  const current = draftOf(progress.currentLevel, progress.currentRank, rules)
  const target = draftOf(progress.targetLevel, progress.targetRank, rules)
  if (current.level === target.level) {
    if (current.ascended === target.ascended) {
      return target.ascended
        ? `Livello ${target.level}, ascensione al ${target.level} già fatta`
        : `Livello ${target.level}`
    }
    // The level stays the same: only the ascension is still to do
    return `Livello ${target.level}, manca l’ascensione al ${target.level}`
  }
  const ascension = target.ascended ? `, con l’ascensione al ${target.level}` : ''
  return `Livello ${current.level} → ${target.level}${ascension}`
}
