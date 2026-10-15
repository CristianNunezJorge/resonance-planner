import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiRequest } from '../../api/client'
import { jinhsiEntry } from '../../test/fixtures'
import {
  addToRoster,
  getFarmingPlan,
  getInventory,
  getRequirements,
  getRoster,
  putInventoryQuantity,
  removeFromRoster,
  updateInRoster,
} from './rosterApi'

vi.mock('../../api/client', () => ({ apiRequest: vi.fn() }))

const TOKEN = 'some.jwt.token'
const progress = { currentLevel: 20, currentRank: 1, targetLevel: 40, targetRank: 2 }

describe('rosterApi', () => {
  afterEach(() => {
    vi.mocked(apiRequest).mockReset()
  })

  it('reads the roster, the inventory and the plan of the user of the token', async () => {
    vi.mocked(apiRequest).mockResolvedValue([])

    await getRoster(TOKEN)
    await getInventory(TOKEN)
    await getFarmingPlan(TOKEN)

    expect(apiRequest).toHaveBeenNthCalledWith(1, '/api/me/roster', { token: TOKEN })
    expect(apiRequest).toHaveBeenNthCalledWith(2, '/api/me/inventory', { token: TOKEN })
    expect(apiRequest).toHaveBeenNthCalledWith(3, '/api/me/farming-plan', { token: TOKEN })
  })

  it('adds, changes and removes a character', async () => {
    vi.mocked(apiRequest).mockResolvedValue(jinhsiEntry)

    await addToRoster(TOKEN, jinhsiEntry)
    await updateInRoster(TOKEN, 'jinhsi', progress)
    await removeFromRoster(TOKEN, 'jinhsi')

    expect(apiRequest).toHaveBeenNthCalledWith(1, '/api/me/roster', { method: 'POST', body: jinhsiEntry, token: TOKEN })
    expect(apiRequest).toHaveBeenNthCalledWith(2, '/api/me/roster/jinhsi', { method: 'PUT', body: progress, token: TOKEN })
    expect(apiRequest).toHaveBeenNthCalledWith(3, '/api/me/roster/jinhsi', { method: 'DELETE', token: TOKEN })
  })

  it('reads the requirements of a character and saves a quantity', async () => {
    vi.mocked(apiRequest).mockResolvedValue({})

    await getRequirements(TOKEN, 'jinhsi')
    await putInventoryQuantity(TOKEN, 'lf-howler-core', 12)

    expect(apiRequest).toHaveBeenNthCalledWith(1, '/api/me/roster/jinhsi/requirements', { token: TOKEN })
    expect(apiRequest).toHaveBeenNthCalledWith(2, '/api/me/inventory/lf-howler-core', {
      method: 'PUT',
      body: { quantity: 12 },
      token: TOKEN,
    })
  })

  it('encodes the identifiers, so a value can never change the address', async () => {
    vi.mocked(apiRequest).mockResolvedValue({})

    await removeFromRoster(TOKEN, '../admin/events')
    await putInventoryQuantity(TOKEN, 'a?b=1#c', 1)

    expect(apiRequest).toHaveBeenNthCalledWith(1, '/api/me/roster/..%2Fadmin%2Fevents', expect.anything())
    expect(apiRequest).toHaveBeenNthCalledWith(2, '/api/me/inventory/a%3Fb%3D1%23c', expect.anything())
  })
})
