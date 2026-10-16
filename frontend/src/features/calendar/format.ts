import type { MaterialSummary } from '../catalog/types'
import { formatPlayTime } from '../progress/levels'
import { formatNumber } from '../roster/format'
import type { AddedMaterial, EventActivity, EventEffect, EventType, FarmingSourceType, Weekday } from './types'

// Texts of the calendar page. The numbers come from the server (D3): here they are only written

const WEEKDAYS: Weekday[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
const WEEKDAY_SHORT: Record<Weekday, string> = {
  MONDAY: 'lun',
  TUESDAY: 'mar',
  WEDNESDAY: 'mer',
  THURSDAY: 'gio',
  FRIDAY: 'ven',
  SATURDAY: 'sab',
  SUNDAY: 'dom',
}

export const EVENT_TYPE_LABEL: Record<EventType, string> = {
  COMBAT_STAGES: 'Combattimento',
  DOUBLE_DROP: 'Ricompense doppie',
  POINTS_SHOP: 'Negozio a punti',
}

// "in Simulation Training", "nella raccolta": the preposition goes with the name
const SOURCE_TYPE_TEXT: Record<FarmingSourceType, string> = {
  SIMULATION_TRAINING: 'in Simulation Training',
  BOSS_CHALLENGE: 'in Boss Challenge',
  OVERWORLD_ENEMIES: 'contro i nemici comuni',
  OVERWORLD_COLLECTION: 'nella raccolta',
}

// A day is a date without a time: it is written in UTC, so it is the same day in every time zone
const dayFormat = new Intl.DateTimeFormat('it-IT', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', timeZone: 'UTC' })
const dayMonthFormat = new Intl.DateTimeFormat('it-IT', { day: 'numeric', month: 'long', timeZone: 'UTC' })
const dayMonthYearFormat = new Intl.DateTimeFormat('it-IT', { day: 'numeric', month: 'long', year: 'numeric', timeZone: 'UTC' })
const multiplierFormat = new Intl.NumberFormat('it-IT', { maximumFractionDigits: 2 })

function asDate(day: string): Date {
  return new Date(`${day}T00:00:00Z`)
}

/** null = every day (D96); otherwise the chosen days from Monday: "lun, mer, ven". */
export function weekdaysText(weekdays: Weekday[] | null): string {
  if (weekdays === null) {
    return 'tutti i giorni'
  }
  return WEEKDAYS.filter((day) => weekdays.includes(day))
    .map((day) => WEEKDAY_SHORT[day])
    .join(', ')
}

/** "venerdì 16 ottobre 2026" */
export function dayText(day: string): string {
  return dayFormat.format(asDate(day))
}

/** "dal 18 ottobre al 24 ottobre 2026"; the first year is written only when it is not the same. */
export function editionText(start: string, end: string): string {
  if (start === end) {
    return `il ${dayMonthYearFormat.format(asDate(start))}`
  }
  const sameYear = start.slice(0, 4) === end.slice(0, 4)
  const first = (sameYear ? dayMonthFormat : dayMonthYearFormat).format(asDate(start))
  return `dal ${first} al ${dayMonthYearFormat.format(asDate(end))}`
}

/** "Ricompense ×2 in Simulation Training, per le prime 3 run del giorno" (D94). */
export function effectText(effect: EventEffect): string {
  const limit =
    effect.dailyLimit === null
      ? 'per tutte le run'
      : effect.dailyLimit === 1
        ? 'per la prima run del giorno'
        : `per le prime ${effect.dailyLimit} run del giorno`
  return `Ricompense ×${multiplierFormat.format(effect.multiplier)} ${SOURCE_TYPE_TEXT[effect.sourceType]}, ${limit}`
}

/** "Drill 1 · 15 minuti · una volta · dal 3° giorno" */
export function activityText(activity: EventActivity): string {
  const parts = [
    activity.title,
    formatPlayTime(activity.minutes),
    activity.frequency === 'DAILY' ? 'ogni giorno' : 'una volta',
  ]
  if (activity.unlockDay !== null) {
    parts.push(`dal ${activity.unlockDay}° giorno`)
  }
  return parts.join(' · ')
}

/** What a done task added to the inventory (D118), with the names of the catalog; empty if nothing. */
export function addedText(added: AddedMaterial[], materials: MaterialSummary[]): string {
  if (added.length === 0) {
    return ''
  }
  const names = new Map(materials.map((material) => [material.id, material.name]))
  const items = added.map(({ materialId, quantity }) => `${formatNumber(quantity)} ${names.get(materialId) ?? materialId}`)
  return `Aggiunti all’inventario: ${items.join(', ')}.`
}

/** "2026-10-17" as "17/10", for a short span of days. */
export function shortDayText(day: string): string {
  return `${day.slice(8, 10)}/${day.slice(5, 7)}`
}
