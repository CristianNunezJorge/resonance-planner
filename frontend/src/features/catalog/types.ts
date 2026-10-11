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
