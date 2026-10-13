import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { setupStore } from '../../app/store'
import { rover, signedIn } from '../../test/utils'
import { getProfile } from './authApi'
import { syncSessionAcrossTabs } from './crossTabSync'
import { clearToken, saveToken, TOKEN_KEY } from './tokenStorage'

vi.mock('./authApi', () => ({ login: vi.fn(), register: vi.fn(), getProfile: vi.fn() }))

// The browser sends this event to the tabs that did NOT change the storage
const storageEvent = (key: string | null) => new StorageEvent('storage', { key })

describe('syncSessionAcrossTabs', () => {
  let stop: () => void

  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    stop?.()
    vi.mocked(getProfile).mockReset()
  })

  it('logs out this tab when another tab logs out', () => {
    saveToken('test.jwt.token')
    const store = setupStore(signedIn())
    stop = syncSessionAcrossTabs(store.dispatch)

    clearToken()
    window.dispatchEvent(storageEvent(TOKEN_KEY))

    expect(store.getState().auth).toMatchObject({ user: null, token: null })
  })

  it('logs out this tab when the whole storage is cleared', () => {
    const store = setupStore(signedIn())
    stop = syncSessionAcrossTabs(store.dispatch)

    window.dispatchEvent(storageEvent(null))

    expect(store.getState().auth.user).toBeNull()
  })

  it('reads who the new token belongs to when another tab logs in', async () => {
    vi.mocked(getProfile).mockResolvedValue(rover)
    const store = setupStore()
    stop = syncSessionAcrossTabs(store.dispatch)

    saveToken('other.tab.token')
    window.dispatchEvent(storageEvent(TOKEN_KEY))

    await vi.waitFor(() => expect(store.getState().auth.user).toEqual(rover))
    expect(getProfile).toHaveBeenCalledWith('other.tab.token')
  })

  it('ignores the other keys of the storage', () => {
    saveToken('test.jwt.token')
    const store = setupStore(signedIn())
    stop = syncSessionAcrossTabs(store.dispatch)

    window.dispatchEvent(storageEvent('something-else'))

    expect(store.getState().auth.user).not.toBeNull()
    expect(getProfile).not.toHaveBeenCalled()
  })

  it('stops listening when asked to', () => {
    const store = setupStore(signedIn())
    stop = syncSessionAcrossTabs(store.dispatch)
    stop()

    window.dispatchEvent(storageEvent(null))

    expect(store.getState().auth.user).not.toBeNull()
  })
})
