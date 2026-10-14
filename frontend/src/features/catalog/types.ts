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
}
