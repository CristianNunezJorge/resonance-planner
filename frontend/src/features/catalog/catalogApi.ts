import { apiRequest } from '../../api/client'
import type { CharacterSummary, GameRules } from './types'

// Public catalog: no token needed (D49)
export function getCharacters(signal?: AbortSignal): Promise<CharacterSummary[]> {
  return apiRequest<CharacterSummary[]>('/api/game/characters', { signal })
}

export function getGameRules(signal?: AbortSignal): Promise<GameRules> {
  return apiRequest<GameRules>('/api/game/rules', { signal })
}
