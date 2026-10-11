import { apiRequest } from '../../api/client'
import type { CharacterSummary } from './types'

// Public catalog: no token needed (D49)
export function getCharacters(signal?: AbortSignal): Promise<CharacterSummary[]> {
  return apiRequest<CharacterSummary[]>('/api/game/characters', { signal })
}
