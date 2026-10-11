import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client'
import { setupStore } from '../../app/store'
import { getCharacters } from './catalogApi'
import { fetchCharacters } from './catalogSlice'
import type { CharacterSummary } from './types'

vi.mock('./catalogApi', () => ({ getCharacters: vi.fn() }))

const jinhsi: CharacterSummary = {
  id: 'jinhsi',
  name: 'Jinhsi',
  element: 'SPECTRO',
  weaponType: 'BROADBLADE',
  rarity: 5,
  role: 'Main Damage Dealer',
  icon: 'characters/jinhsi.webp',
}

describe('catalogSlice', () => {
  afterEach(() => {
    vi.mocked(getCharacters).mockReset()
  })

  it('is loading while the characters are on their way', () => {
    vi.mocked(getCharacters).mockReturnValue(new Promise(() => {}))
    const store = setupStore()

    void store.dispatch(fetchCharacters())

    expect(store.getState().catalog.charactersStatus).toBe('loading')
  })

  it('stores the characters when the request succeeds', async () => {
    vi.mocked(getCharacters).mockResolvedValue([jinhsi])
    const store = setupStore()

    await store.dispatch(fetchCharacters())

    expect(store.getState().catalog.charactersStatus).toBe('succeeded')
    expect(store.getState().catalog.characters).toEqual([jinhsi])
  })

  it('keeps the message of the error when the request fails', async () => {
    vi.mocked(getCharacters).mockRejectedValue(
      new ApiError('NETWORK_ERROR', 'Impossibile contattare il server. Controlla la connessione e riprova.', 0),
    )
    const store = setupStore()

    await store.dispatch(fetchCharacters())

    expect(store.getState().catalog.charactersStatus).toBe('failed')
    expect(store.getState().catalog.charactersError).toBe(
      'Impossibile contattare il server. Controlla la connessione e riprova.',
    )
  })

  it('does not ask the server again once the characters are loaded', async () => {
    vi.mocked(getCharacters).mockResolvedValue([jinhsi])
    const store = setupStore()

    await store.dispatch(fetchCharacters())
    await store.dispatch(fetchCharacters())

    expect(getCharacters).toHaveBeenCalledTimes(1)
  })
})
