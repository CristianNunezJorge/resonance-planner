import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiRequest } from '../../api/client'
import { deleteAccount, updateProfile } from './profileApi'

vi.mock('../../api/client', () => ({ apiRequest: vi.fn() }))

const TOKEN = 'some.jwt.token'

describe('profileApi', () => {
  afterEach(() => {
    vi.mocked(apiRequest).mockReset()
  })

  it('changes and deletes the account of the token', async () => {
    vi.mocked(apiRequest).mockResolvedValue(undefined)

    await updateProfile(TOKEN, { playTimeMinutes: 120 })
    await deleteAccount(TOKEN)

    expect(apiRequest).toHaveBeenNthCalledWith(1, '/api/me', { method: 'PATCH', body: { playTimeMinutes: 120 }, token: TOKEN })
    expect(apiRequest).toHaveBeenNthCalledWith(2, '/api/me', { method: 'DELETE', token: TOKEN })
  })
})
