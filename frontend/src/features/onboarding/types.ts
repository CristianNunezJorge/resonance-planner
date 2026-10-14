// Shapes of the guided setup (D20, D50) and of its request (docs/05-api.md)

export type Step = 1 | 2 | 3

// A level and, only at a level where an ascension is needed, whether that ascension is done (D104): together they
// say the rank, which the user never sees
export interface LevelDraft {
  level: number
  ascended: boolean
}

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
