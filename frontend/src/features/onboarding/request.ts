import type { CharacterSummary, GameRules } from '../catalog/types'
import { rankOf } from '../progress/levels'
import type { CharacterRequest, DraftState, OnboardingRequest } from './types'

/**
 * The body of POST /api/me/onboarding. The characters go in the order of the catalog, whatever the order they were
 * chosen in; the rank of each level comes from the level and from the ascension checkbox (D104). It is called only
 * after every step was checked, so the numbers are valid and a missing value is a bug, not a user mistake.
 */
export function buildRequest(draft: DraftState, characters: CharacterSummary[], rules: GameRules): OnboardingRequest {
  if (draft.playTimeMinutes === null) {
    throw new Error('The play time is missing: the steps must be checked before the request is built')
  }
  const chosen = characters.filter((character) => draft.selected.includes(character.id))
  return {
    characters: chosen.map((character) => characterRequest(draft, character.id, rules)),
    waveplate: {
      value: Number(draft.waveplate.trim()),
      crystals: draft.crystals.trim() === '' ? 0 : Number(draft.crystals.trim()),
    },
    playTimeMinutes: draft.playTimeMinutes,
  }
}

function characterRequest(draft: DraftState, characterId: string, rules: GameRules): CharacterRequest {
  const levels = draft.drafts[characterId]
  if (!levels) {
    throw new Error(`The levels of ${characterId} are missing: a chosen character always has them`)
  }
  return {
    characterId,
    currentLevel: levels.current.level,
    currentRank: rankOf(levels.current, rules),
    targetLevel: levels.target.level,
    targetRank: rankOf(levels.target, rules),
  }
}
