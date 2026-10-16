// Shapes of the public catalog API (/api/game/**)

// Not called Element, to avoid hiding the DOM type with the same name
export type GameElement = 'AERO' | 'ELECTRO' | 'FUSION' | 'GLACIO' | 'HAVOC' | 'SPECTRO'

export type WeaponType = 'BROADBLADE' | 'SWORD' | 'PISTOLS' | 'GAUNTLETS' | 'RECTIFIER'

export interface CharacterSummary {
  id: string
  name: string
  element: GameElement
  weaponType: WeaponType
  rarity: number
  role: string
  // Path inside public/, for example "characters/jinhsi.webp" (D30)
  icon: string | null
}

export type MaterialCategory = 'CREDIT' | 'CHARACTER_EXP' | 'ENEMY_DROP' | 'BOSS_DROP' | 'LOCAL_SPECIALTY'

export interface MaterialSummary {
  id: string
  name: string
  category: MaterialCategory
  // Only for the Resonance Potions: the EXP of one potion
  exp: number | null
  icon: string | null
}

// Rules of the game that the frontend needs (GET /api/game/rules): the same numbers the backend checks with
export interface Regeneration {
  max: number
  regenMinutes: number
}

export interface GameRules {
  waveplate: Regeneration
  waveplateCrystals: Regeneration
  // The levels where an ascension is needed, for example [20, 40] (D34)
  ascensionLevels: number[]
  maxLevel: number
  playTimeOptionsMinutes: number[]
  // The day from which the offset of a recurring event is counted (D29), as "2026-10-01"
  demoEventsAnchorDate: string
}
