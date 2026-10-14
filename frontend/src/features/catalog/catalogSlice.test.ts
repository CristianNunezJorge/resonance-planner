import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client'
import { setupStore } from '../../app/store'
import { gameRules } from '../../test/fixtures'
import { getCharacters, getGameRules } from './catalogApi'
import { fetchCharacters, fetchRules } from './catalogSlice'
import type { CharacterSummary } from './types'

vi.mock('./catalogApi', () => ({ getCharacters: vi.fn(), getGameRules: vi.fn() }))

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
    vi.mocked(getGameRules).mockReset()
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

describe('catalogSlice · rules of the game', () => {
  afterEach(() => {
    vi.mocked(getGameRules).mockReset()
  })

  it('starts without rules', () => {
    const state = setupStore().getState().catalog

    expect(state.rules).toBeNull()
    expect(state.rulesStatus).toBe('idle')
  })

  it('is loading while the rules are on their way', () => {
    vi.mocked(getGameRules).mockReturnValue(new Promise(() => {}))
    const store = setupStore()

    void store.dispatch(fetchRules())

    expect(store.getState().catalog.rulesStatus).toBe('loading')
  })

  it('stores the rules when the request succeeds', async () => {
    vi.mocked(getGameRules).mockResolvedValue(gameRules)
    const store = setupStore()

    await store.dispatch(fetchRules())

    expect(store.getState().catalog.rulesStatus).toBe('succeeded')
    expect(store.getState().catalog.rules).toEqual(gameRules)
  })

  it('keeps the message of the error when the request fails, and tries again when asked', async () => {
    vi.mocked(getGameRules)
      .mockRejectedValueOnce(new ApiError('NETWORK_ERROR', 'Impossibile contattare il server.', 0))
      .mockResolvedValueOnce(gameRules)
    const store = setupStore()

    await store.dispatch(fetchRules())
    expect(store.getState().catalog.rulesStatus).toBe('failed')
    expect(store.getState().catalog.rulesError).toBe('Impossibile contattare il server.')

    await store.dispatch(fetchRules())
    expect(store.getState().catalog.rulesStatus).toBe('succeeded')
    expect(store.getState().catalog.rulesError).toBeNull()
  })

  it('does not ask the server again once the rules are loaded', async () => {
    vi.mocked(getGameRules).mockResolvedValue(gameRules)
    const store = setupStore()

    await store.dispatch(fetchRules())
    await store.dispatch(fetchRules())

    expect(getGameRules).toHaveBeenCalledTimes(1)
  })

  it('does not touch the characters', async () => {
    vi.mocked(getGameRules).mockResolvedValue(gameRules)
    const store = setupStore()

    await store.dispatch(fetchRules())

    expect(store.getState().catalog.charactersStatus).toBe('idle')
  })
})
