// Shapes of the guided setup (D20, D50) and of its request (docs/05-api.md)

import type { LevelDraft } from '../progress/types'

export type Step = 1 | 2 | 3

export interface CharacterDraft {
  current: LevelDraft
  target: LevelDraft
}

// What the user has typed and picked so far, kept in Redux until the last step sends everything together (D50).
// The Waveplate are kept as typed: a number is checked only when the user goes on.
export interface DraftState {
  selected: string[]
  drafts: Record<string, CharacterDraft>
  waveplate: string
  crystals: string
  playTimeMinutes: number | null
}

export interface CharacterRequest {
  characterId: string
  currentLevel: number
  currentRank: number
  targetLevel: number
  targetRank: number
}

export interface OnboardingRequest {
  characters: CharacterRequest[]
  waveplate: { value: number; crystals: number }
  playTimeMinutes: number
}
