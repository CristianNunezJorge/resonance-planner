import type { GameRules } from '../catalog/types'
import { isLowerThan } from '../progress/levels'
import { CRYSTALS_KEY, WAVEPLATE_KEY, validateWaveplate } from '../waveplate/validation'
import type { DraftState, Step } from './types'

// The checks that the page does before each step, so the user sees what to fix without waiting for the server.
// They repeat the rules that the server applies (D104, D105): the server decides, and its messages come back to the
// same fields (failureMapping.ts). Every error is stored under a key that names a field of the page.

export type FieldErrors = Record<string, string>

export const CHARACTERS_KEY = 'characters'
// The keys of the Waveplate fields are shared with the calendar (validation of the waveplate feature)
export { CRYSTALS_KEY, WAVEPLATE_KEY }
export const PLAY_TIME_KEY = 'playTimeMinutes'

export function progressKey(characterId: string, side: 'current' | 'target'): string {
  return `progress.${characterId}.${side}`
}

/** Step 1: at least one character. */
export function validateSelection(selected: string[]): FieldErrors {
  return selected.length === 0 ? { [CHARACTERS_KEY]: 'Scegli almeno un personaggio.' } : {}
}

/** Step 2: the goal of a chosen character cannot be lower than where it is now. */
export function validateProgress(
  selected: string[],
  drafts: DraftState['drafts'],
  rules: GameRules,
): FieldErrors {
  const errors: FieldErrors = {}
  for (const id of selected) {
    const draft = drafts[id]
    if (draft && isLowerThan(draft.target, draft.current, rules)) {
      errors[progressKey(id, 'target')] = "L'obiettivo non può essere più basso della situazione attuale."
    }
  }
  return errors
}

/** Step 3: the Waveplate (required), the Crystals (empty means zero) and the play time, all within the game's limits. */
export function validateStatus(
  draft: Pick<DraftState, 'waveplate' | 'crystals' | 'playTimeMinutes'>,
  rules: GameRules,
): FieldErrors {
  const errors: FieldErrors = validateWaveplate(draft, rules)
  if (draft.playTimeMinutes === null) {
    errors[PLAY_TIME_KEY] = 'Scegli quanto giochi al giorno.'
  }
  return errors
}

export function validateStep(step: Step, draft: DraftState, rules: GameRules): FieldErrors {
  switch (step) {
    case 1:
      return validateSelection(draft.selected)
    case 2:
      return validateProgress(draft.selected, draft.drafts, rules)
    case 3:
      return validateStatus(draft, rules)
  }
}
