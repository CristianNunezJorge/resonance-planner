import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiRequest } from '../../api/client'
import { rover } from '../../test/utils'
import { postOnboarding } from './onboardingApi'
import type { OnboardingRequest } from './types'

vi.mock('../../api/client', () => ({ apiRequest: vi.fn() }))

const request: OnboardingRequest = {
  characters: [{ characterId: 'jinhsi', currentLevel: 1, currentRank: 0, targetLevel: 40, targetRank: 2 }],
  waveplate: { value: 180, crystals: 0 },
  playTimeMinutes: 60,
}

describe('postOnboarding', () => {
  afterEach(() => {
    vi.mocked(apiRequest).mockReset()
  })

  it('posts the setup to the onboarding path with the token, and answers with the updated user', async () => {
    const updated = { ...rover, onboardingCompleted: true, playTimeMinutes: 60 }
    vi.mocked(apiRequest).mockResolvedValue(updated)

    const user = await postOnboarding('some.jwt.token', request)

    expect(apiRequest).toHaveBeenCalledWith('/api/me/onboarding', { method: 'POST', body: request, token: 'some.jwt.token' })
    expect(user).toEqual(updated)
  })
})
