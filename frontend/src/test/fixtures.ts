import type { CharacterSummary, GameRules, MaterialSummary } from '../features/catalog/types'
import type { CharacterRequirements, FarmingPlan, RosterEntry } from '../features/roster/types'

// The values of game-rules.json, as GET /api/game/rules answers them
export const gameRules: GameRules = {
  waveplate: { max: 240, regenMinutes: 6 },
  waveplateCrystals: { max: 480, regenMinutes: 12 },
  ascensionLevels: [20, 40],
  maxLevel: 40,
  playTimeOptionsMinutes: [30, 60, 120],
  demoEventsAnchorDate: '2026-10-01',
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

// The roster of the tests: Jinhsi from the start to the cap, Aalto already at 40 before the second ascension
export const jinhsiEntry: RosterEntry = { characterId: 'jinhsi', currentLevel: 1, currentRank: 0, targetLevel: 40, targetRank: 2 }
export const aaltoEntry: RosterEntry = { characterId: 'aalto', currentLevel: 40, currentRank: 1, targetLevel: 40, targetRank: 2 }

// The numbers of the example of D112: Jinhsi from level 1 to 40 with an empty inventory
export const jinhsiPlan: FarmingPlan = {
  materials: [
    { materialId: 'elegy-tacet-core', name: 'Elegy Tacet Core', category: 'BOSS_DROP', required: 3, owned: 0, missing: 3 },
    { materialId: 'lf-howler-core', name: 'LF Howler Core', category: 'ENEMY_DROP', required: 4, owned: 4, missing: 0 },
    { materialId: 'character-exp', name: 'EXP personaggio', category: 'CHARACTER_EXP', required: 208800, owned: 0, missing: 208800 },
    { materialId: 'shell-credit', name: 'Shell Credit', category: 'CREDIT', required: 88080, owned: 0, missing: 88080 },
  ],
  sources: [
    { farmingSourceId: 'boss-mourning-aix', name: 'Boss Challenge: Mourning Aix', runs: 2, waveplates: 120, minutes: 8 },
    { farmingSourceId: 'sim-resonance-potion', name: 'Simulation Training: Resonance Potion', runs: 3, waveplates: 120, minutes: 9 },
  ],
  totals: { waveplates: 280, minutes: 50, estimatedDays: 2 },
  containsEstimates: true,
}

export const jinhsiRequirements: CharacterRequirements = { ...jinhsiPlan, characterId: 'jinhsi', completionPercent: 25 }

export const aaltoRequirements: CharacterRequirements = {
  characterId: 'aalto',
  completionPercent: 100,
  materials: [],
  sources: [],
  totals: { waveplates: 0, minutes: 0, estimatedDays: 0 },
  containsEstimates: false,
}

// A few materials of materials.json, in the order of the catalog (category, then name)
export const catalogMaterials: MaterialSummary[] = [
  { id: 'elegy-tacet-core', name: 'Elegy Tacet Core', category: 'BOSS_DROP', exp: null, icon: null },
  { id: 'premium-resonance-potion', name: 'Premium Resonance Potion', category: 'CHARACTER_EXP', exp: 20000, icon: null },
  { id: 'shell-credit', name: 'Shell Credit', category: 'CREDIT', exp: null, icon: null },
  { id: 'lf-howler-core', name: 'LF Howler Core', category: 'ENEMY_DROP', exp: null, icon: null },
]
