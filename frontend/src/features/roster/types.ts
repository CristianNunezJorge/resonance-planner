import type { MaterialCategory } from '../catalog/types'

// Shapes of the roster, inventory and farming APIs (docs/05-api.md, D104, D108, D112)

export interface Progress {
  currentLevel: number
  currentRank: number
  targetLevel: number
  targetRank: number
}

export interface RosterEntry extends Progress {
  characterId: string
}

// A material that is needed; the EXP is the line "character-exp", in points (D112)
export interface MaterialNeed {
  materialId: string
  name: string
  category: MaterialCategory
  required: number
  owned: number
  missing: number
}

export interface SourceRuns {
  farmingSourceId: string
  name: string
  runs: number
  waveplates: number
  minutes: number
}

export interface FarmingTotals {
  waveplates: number
  minutes: number
  // null when the user has not chosen a daily play time yet
  estimatedDays: number | null
}

export interface FarmingPlan {
  materials: MaterialNeed[]
  sources: SourceRuns[]
  totals: FarmingTotals
  containsEstimates: boolean
}

// The same calculation for one character, as if the whole inventory were for it (D112, point 7)
export interface CharacterRequirements extends FarmingPlan {
  characterId: string
  completionPercent: number
}

export interface InventoryItem {
  materialId: string
  quantity: number
}
