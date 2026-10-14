import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiRequest } from '../../api/client'
import { catalogCharacters, gameRules } from '../../test/fixtures'
import { getCharacters, getGameRules } from './catalogApi'

vi.mock('../../api/client', () => ({ apiRequest: vi.fn() }))

// The public catalog (D49): no token is sent
describe('catalogApi', () => {
  afterEach(() => {
    vi.mocked(apiRequest).mockReset()
  })

  it('reads the characters from the public path, without a token', async () => {
    vi.mocked(apiRequest).mockResolvedValue(catalogCharacters)
    const controller = new AbortController()

    const characters = await getCharacters(controller.signal)

    expect(apiRequest).toHaveBeenCalledWith('/api/game/characters', { signal: controller.signal })
    expect(characters).toEqual(catalogCharacters)
  })

  it('reads the rules of the game from the public path, without a token', async () => {
    vi.mocked(apiRequest).mockResolvedValue(gameRules)

    const rules = await getGameRules()

    expect(apiRequest).toHaveBeenCalledWith('/api/game/rules', { signal: undefined })
    expect(rules).toEqual(gameRules)
  })
})
