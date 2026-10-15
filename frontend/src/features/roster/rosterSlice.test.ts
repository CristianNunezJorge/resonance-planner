import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client'
import { setupStore } from '../../app/store'
import {
  aaltoEntry,
  aaltoRequirements,
  jinhsiEntry,
  jinhsiPlan,
  jinhsiRequirements,
} from '../../test/fixtures'
import { rover, signedIn } from '../../test/utils'
import { login as loginRequest } from '../auth/authApi'
import { login, sessionEnded } from '../auth/authSlice'
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
import {
  addCharacter,
  loadRoster,
  removeCharacter,
  requirementsOf,
  saveQuantity,
  updateCharacter,
} from './rosterSlice'

vi.mock('./rosterApi', () => ({
  getRoster: vi.fn(),
  getInventory: vi.fn(),
  getFarmingPlan: vi.fn(),
  getRequirements: vi.fn(),
  addToRoster: vi.fn(),
  updateInRoster: vi.fn(),
  removeFromRoster: vi.fn(),
  putInventoryQuantity: vi.fn(),
}))
vi.mock('../auth/authApi', () => ({ login: vi.fn(), register: vi.fn(), getProfile: vi.fn() }))

const TOKEN = 'test.jwt.token'

function newStore() {
  return setupStore(signedIn())
}

// Lets the reload that a change starts arrive before the test looks at the store
async function settle() {
  await new Promise((resolve) => setTimeout(resolve, 0))
}

describe('rosterSlice', () => {
  beforeEach(() => {
    vi.mocked(getRoster).mockResolvedValue([jinhsiEntry, aaltoEntry])
    vi.mocked(getInventory).mockResolvedValue([{ materialId: 'lf-howler-core', quantity: 4 }])
    vi.mocked(getFarmingPlan).mockResolvedValue(jinhsiPlan)
    vi.mocked(getRequirements).mockImplementation(async (_, id) =>
      id === 'jinhsi' ? jinhsiRequirements : aaltoRequirements,
    )
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  describe('loadRoster', () => {
    it('loads roster, inventory and plan, then the requirements of every character', async () => {
      const store = newStore()

      await store.dispatch(loadRoster())

      const state = store.getState().roster
      expect(state.status).toBe('succeeded')
      expect(state.entries).toEqual([jinhsiEntry, aaltoEntry])
      expect(state.inventory).toEqual({ 'lf-howler-core': 4 })
      expect(state.plan).toEqual(jinhsiPlan)
      expect(requirementsOf(store.getState(), 'jinhsi')).toEqual(jinhsiRequirements)
      expect(requirementsOf(store.getState(), 'aalto')).toEqual(aaltoRequirements)
      expect(getRequirements).toHaveBeenCalledWith(TOKEN, 'jinhsi')
      expect(getRequirements).toHaveBeenCalledWith(TOKEN, 'aalto')
    })

    it('never reads an id from the prototype of the requirements', async () => {
      const store = newStore()
      await store.dispatch(loadRoster())

      expect(requirementsOf(store.getState(), 'constructor')).toBeUndefined()
      expect(requirementsOf(store.getState(), '__proto__')).toBeUndefined()
    })

    it('asks for nothing without a session', async () => {
      const store = setupStore()

      await store.dispatch(loadRoster())

      expect(store.getState().roster.status).toBe('failed')
      expect(store.getState().roster.error).toBe('Devi accedere per vedere il tuo roster.')
      expect(getRoster).not.toHaveBeenCalled()
    })

    it('shows the message of a first load that fails', async () => {
      vi.mocked(getFarmingPlan).mockRejectedValue(new ApiError('SERVICE_UNAVAILABLE', 'Il server non è ancora pronto.', 503))
      const store = newStore()

      await store.dispatch(loadRoster())

      expect(store.getState().roster.status).toBe('failed')
      expect(store.getState().roster.error).toBe('Il server non è ancora pronto.')
    })

    it('keeps the data of before when a reload fails, with its message', async () => {
      const store = newStore()
      await store.dispatch(loadRoster())
      vi.mocked(getRoster).mockRejectedValue(new ApiError('NETWORK_ERROR', 'Impossibile contattare il server.', 0))

      const reload = store.dispatch(loadRoster())
      expect(store.getState().roster.refreshing).toBe(true)
      await reload

      const state = store.getState().roster
      expect(state.status).toBe('succeeded')
      expect(state.refreshing).toBe(false)
      expect(state.error).toBe('Impossibile contattare il server.')
      expect(state.entries).toEqual([jinhsiEntry, aaltoEntry])
    })

    it('uses only the newest of two loads, even when the older one answers last', async () => {
      let answerOld: (entries: (typeof jinhsiEntry)[]) => void = () => {}
      vi.mocked(getRoster)
        .mockReturnValueOnce(new Promise((resolve) => (answerOld = resolve)))
        .mockResolvedValueOnce([aaltoEntry])
      const store = newStore()

      const older = store.dispatch(loadRoster())
      await store.dispatch(loadRoster())
      answerOld([jinhsiEntry, aaltoEntry])
      await older

      expect(store.getState().roster.entries).toEqual([aaltoEntry])
    })

    it('ignores the answer of a load that started before the session ended', async () => {
      let answer: (entries: (typeof jinhsiEntry)[]) => void = () => {}
      vi.mocked(getRoster).mockReturnValue(new Promise((resolve) => (answer = resolve)))
      const store = newStore()

      const load = store.dispatch(loadRoster())
      store.dispatch(sessionEnded())
      answer([jinhsiEntry])
      await load

      expect(store.getState().roster).toMatchObject({ status: 'idle', entries: [], plan: null })
    })

    it('forgets the data of the user when the session ends or another user logs in', async () => {
      const store = newStore()
      await store.dispatch(loadRoster())
      store.dispatch(sessionEnded())
      expect(store.getState().roster.entries).toEqual([])

      await store.dispatch(loadRoster())
      vi.mocked(loginRequest).mockResolvedValue({ token: 'other.jwt.token', user: { ...rover, id: 8 } })
      await store.dispatch(login({ email: 'other@example.com', password: 'secret-password' }))
      expect(store.getState().roster.entries).toEqual([])
    })
  })

  describe('changes', () => {
    it('adds a character with the token, then reloads the data', async () => {
      vi.mocked(addToRoster).mockResolvedValue(jinhsiEntry)
      const store = newStore()

      const result = await store.dispatch(addCharacter(jinhsiEntry))

      // The change answers after the reload: the page shows the new numbers when the form closes
      expect(addCharacter.fulfilled.match(result)).toBe(true)
      expect(addToRoster).toHaveBeenCalledWith(TOKEN, jinhsiEntry)
      expect(getRoster).toHaveBeenCalledOnce()
      expect(store.getState().roster.status).toBe('succeeded')
      expect(store.getState().roster.entries).toEqual([jinhsiEntry, aaltoEntry])
    })

    it('changes, removes and saves with the token', async () => {
      vi.mocked(updateInRoster).mockResolvedValue(jinhsiEntry)
      vi.mocked(removeFromRoster).mockResolvedValue(undefined)
      vi.mocked(putInventoryQuantity).mockResolvedValue({ materialId: 'lf-howler-core', quantity: 9 })
      const store = newStore()
      const progress = { currentLevel: 20, currentRank: 1, targetLevel: 40, targetRank: 2 }

      await store.dispatch(updateCharacter({ characterId: 'jinhsi', progress }))
      await store.dispatch(removeCharacter('aalto'))
      const saved = await store.dispatch(saveQuantity({ materialId: 'lf-howler-core', quantity: 9 }))
      await settle()

      expect(updateInRoster).toHaveBeenCalledWith(TOKEN, 'jinhsi', progress)
      expect(removeFromRoster).toHaveBeenCalledWith(TOKEN, 'aalto')
      expect(putInventoryQuantity).toHaveBeenCalledWith(TOKEN, 'lf-howler-core', 9)
      expect(saved.payload).toEqual({ materialId: 'lf-howler-core', quantity: 9 })
    })

    it('gives the failure to the form, with its fields, and reloads nothing', async () => {
      vi.mocked(addToRoster).mockRejectedValue(
        new ApiError('CONFLICT', 'Questo personaggio è già nel tuo roster.', 409, { characterId: 'Già presente.' }),
      )
      const store = newStore()

      const result = await store.dispatch(addCharacter(jinhsiEntry))
      await settle()

      expect(result.payload).toEqual({
        code: 'CONFLICT',
        message: 'Questo personaggio è già nel tuo roster.',
        status: 409,
        fields: { characterId: 'Già presente.' },
      })
      expect(getRoster).not.toHaveBeenCalled()
    })

    it('reloads nothing when the session ended while the change was running', async () => {
      let answer: (entry: typeof jinhsiEntry) => void = () => {}
      vi.mocked(addToRoster).mockReturnValue(new Promise((resolve) => (answer = resolve)))
      const store = newStore()

      const adding = store.dispatch(addCharacter(jinhsiEntry))
      store.dispatch(sessionEnded())
      answer(jinhsiEntry)
      await adding
      await settle()

      expect(getRoster).not.toHaveBeenCalled()
    })

    it('sends nothing without a session', async () => {
      const store = setupStore()

      const result = await store.dispatch(removeCharacter('jinhsi'))

      expect(removeCharacter.rejected.match(result)).toBe(true)
      expect(removeFromRoster).not.toHaveBeenCalled()
    })
  })
})
