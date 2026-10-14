import type { CharacterSummary, GameRules } from '../features/catalog/types'

// The values of game-rules.json, as GET /api/game/rules answers them
export const gameRules: GameRules = {
  waveplate: { max: 240, regenMinutes: 6 },
  waveplateCrystals: { max: 480, regenMinutes: 12 },
  ascensionLevels: [20, 40],
  maxLevel: 40,
  playTimeOptionsMinutes: [30, 60, 120],
}

export const jinhsi: CharacterSummary = {
  id: 'jinhsi',
  name: 'Jinhsi',
  element: 'SPECTRO',
  weaponType: 'BROADBLADE',
  rarity: 5,
  role: 'Main Damage Dealer',
  icon: 'characters/jinhsi.webp',
}

export const aalto: CharacterSummary = {
  id: 'aalto',
  name: 'Aalto',
  element: 'AERO',
  weaponType: 'PISTOLS',
  rarity: 4,
  role: 'Concerto Efficiency',
  icon: 'characters/aalto.webp',
}

export const yinlin: CharacterSummary = {
  id: 'yinlin',
  name: 'Yinlin',
  element: 'ELECTRO',
  weaponType: 'RECTIFIER',
  rarity: 5,
  role: 'Concerto Efficiency',
  icon: 'characters/yinlin.webp',
}

// In the order of the catalog (alphabetical by name, like the backend answers)
export const catalogCharacters: CharacterSummary[] = [aalto, jinhsi, yinlin]
