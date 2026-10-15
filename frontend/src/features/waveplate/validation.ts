import type { GameRules } from '../catalog/types'

// The check of the Waveplate that the user types (D105), shared by the guided setup and the calendar: the page tells
// what to fix without waiting for the server, which applies the same rules and decides.

export const WAVEPLATE_KEY = 'waveplate'
export const CRYSTALS_KEY = 'crystals'

const WHOLE_NUMBER = /^\d+$/

// Only digits: no sign, no decimals, no exponent. Number() of a very long text is a huge number, so it fails the limit
export function isWholeNumberUpTo(text: string, max: number): boolean {
  return WHOLE_NUMBER.test(text) && Number(text) <= max
}

/** The Waveplate (required) and the Crystals (empty means zero), within the game's limits. */
export function validateWaveplate(values: { waveplate: string; crystals: string }, rules: GameRules): Record<string, string> {
  const errors: Record<string, string> = {}
  const waveplate = values.waveplate.trim()
  if (waveplate === '') {
    errors[WAVEPLATE_KEY] = 'Inserisci le Waveplate che vedi nel gioco.'
  } else if (!isWholeNumberUpTo(waveplate, rules.waveplate.max)) {
    errors[WAVEPLATE_KEY] = `Inserisci un numero intero da 0 a ${rules.waveplate.max}.`
  }
  const crystals = values.crystals.trim()
  if (crystals !== '' && !isWholeNumberUpTo(crystals, rules.waveplateCrystals.max)) {
    errors[CRYSTALS_KEY] = `Inserisci un numero intero da 0 a ${rules.waveplateCrystals.max}.`
  }
  return errors
}
