import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiRequest } from '../../api/client'
import { getWaveplate, saveWaveplate } from './waveplateApi'

vi.mock('../../api/client', () => ({ apiRequest: vi.fn() }))

const TOKEN = 'some.jwt.token'

describe('waveplateApi', () => {
  afterEach(() => {
    vi.mocked(apiRequest).mockReset()
  })

  it('reads and saves the Waveplate of the user of the token', async () => {
    vi.mocked(apiRequest).mockResolvedValue(undefined)

    await getWaveplate(TOKEN)
    await saveWaveplate(TOKEN, { value: 180, crystals: 0 })

    expect(apiRequest).toHaveBeenNthCalledWith(1, '/api/me/waveplate', { token: TOKEN })
    expect(apiRequest).toHaveBeenNthCalledWith(2, '/api/me/waveplate', {
      method: 'PUT',
      body: { value: 180, crystals: 0 },
      token: TOKEN,
    })
  })
})
