import { apiRequest } from '../../api/client'
import type { CharacterRequirements, FarmingPlan, InventoryItem, Progress, RosterEntry } from './types'

// The personal area of the roster page (D44): the user is always the one of the token. The identifiers go into the
// path encoded, so a value can never change the address (T17-T27 reminder).

export function getRoster(token: string): Promise<RosterEntry[]> {
  return apiRequest<RosterEntry[]>('/api/me/roster', { token })
}

export function addToRoster(token: string, entry: RosterEntry): Promise<RosterEntry> {
  return apiRequest<RosterEntry>('/api/me/roster', { method: 'POST', body: entry, token })
}

export function updateInRoster(token: string, characterId: string, progress: Progress): Promise<RosterEntry> {
  return apiRequest<RosterEntry>(`/api/me/roster/${encodeURIComponent(characterId)}`, {
    method: 'PUT',
    body: progress,
    token,
  })
}

export function removeFromRoster(token: string, characterId: string): Promise<void> {
  return apiRequest<void>(`/api/me/roster/${encodeURIComponent(characterId)}`, { method: 'DELETE', token })
}

export function getRequirements(token: string, characterId: string): Promise<CharacterRequirements> {
  return apiRequest<CharacterRequirements>(`/api/me/roster/${encodeURIComponent(characterId)}/requirements`, {
    token,
  })
}

export function getFarmingPlan(token: string): Promise<FarmingPlan> {
  return apiRequest<FarmingPlan>('/api/me/farming-plan', { token })
}

export function getInventory(token: string): Promise<InventoryItem[]> {
  return apiRequest<InventoryItem[]>('/api/me/inventory', { token })
}

export function putInventoryQuantity(token: string, materialId: string, quantity: number): Promise<InventoryItem> {
  return apiRequest<InventoryItem>(`/api/me/inventory/${encodeURIComponent(materialId)}`, {
    method: 'PUT',
    body: { quantity },
    token,
  })
}
