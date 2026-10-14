import type { GameRules } from '../catalog/types'
import { isLowerThan } from './levels'
import type { DraftState, Step } from './types'

// The checks that the page does before each step, so the user sees what to fix without waiting for the server.
// They repeat the rules that the server applies (D104, D105): the server decides, and its messages come back to the
// same fields (failureMapping.ts). Every error is stored under a key that names a field of the page.

export type FieldErrors = Record<string, string>

export const CHARACTERS_KEY = 'characters'
export const WAVEPLATE_KEY = 'waveplate'
export const CRYSTALS_KEY = 'crystals'
export const PLAY_TIME_KEY = 'playTimeMinutes'

export function progressKey(characterId: string, side: 'current' | 'target'): string {
  return `progress.${characterId}.${side}`
}

const WHOLE_NUMBER = /^\d+$/

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
  const errors: FieldErrors = {}
  const waveplate = draft.waveplate.trim()
  if (waveplate === '') {
    errors[WAVEPLATE_KEY] = 'Inserisci le Waveplate che vedi nel gioco.'
  } else if (!isWholeNumberUpTo(waveplate, rules.waveplate.max)) {
    errors[WAVEPLATE_KEY] = `Inserisci un numero intero da 0 a ${rules.waveplate.max}.`
  }
  const crystals = draft.crystals.trim()
  if (crystals !== '' && !isWholeNumberUpTo(crystals, rules.waveplateCrystals.max)) {
    errors[CRYSTALS_KEY] = `Inserisci un numero intero da 0 a ${rules.waveplateCrystals.max}.`
  }
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

// Only digits: no sign, no decimals, no exponent. Number() of a very long text is a huge number, so it fails the limit.
function isWholeNumberUpTo(text: string, max: number): boolean {
  return WHOLE_NUMBER.test(text) && Number(text) <= max
}
