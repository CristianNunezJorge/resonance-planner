import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { setUnauthorizedHandler } from '../../api/client'
import { setupStore } from '../../app/store'
import { signedIn } from '../../test/utils'
import { getProfile } from './authApi'
import { expireSession, restoreSession } from './authSlice'
import { readToken, saveToken } from './tokenStorage'

// Here nothing of the API layer is mocked: the real client, the real handler (wired like main.tsx) and the real
// slice work together, with only the network replaced.

const unauthorized = () =>
  new Response(
    JSON.stringify({ success: false, data: null, error: { code: 'UNAUTHORIZED', message: 'Devi effettuare l\'accesso.' } }),
    { status: 401, headers: { 'Content-Type': 'application/json' } },
  )

describe('a token refused by the server', () => {
  const fetchMock = vi.fn<typeof fetch>()

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
    vi.stubEnv('VITE_API_BASE_URL', 'https://api.example.com')
    localStorage.clear()
  })

  afterEach(() => {
    setUnauthorizedHandler(null)
    fetchMock.mockReset()
    vi.unstubAllGlobals()
    vi.unstubAllEnvs()
  })

  function wire(store: ReturnType<typeof setupStore>) {
    setUnauthorizedHandler((refusedToken) => store.dispatch(expireSession(refusedToken)))
  }

  it('at the start of the app ends in one single notice and no saved token', async () => {
    saveToken('expired.jwt.token')
    fetchMock.mockResolvedValue(unauthorized())
    const store = setupStore()
    wire(store)

    await store.dispatch(restoreSession())

    expect(store.getState().auth).toMatchObject({ user: null, token: null, sessionStatus: 'ready' })
    expect(readToken()).toBeNull()
    expect(store.getState().toast.items).toHaveLength(1)
    expect(store.getState().toast.items[0].message).toBe('La sessione è scaduta: accedi di nuovo.')
  })

  it('while the user works ends the session, with one notice', async () => {
    saveToken('test.jwt.token')
    fetchMock.mockResolvedValue(unauthorized())
    const store = setupStore(signedIn())
    wire(store)

    await getProfile('test.jwt.token').catch(() => undefined)

    expect(store.getState().auth).toMatchObject({ user: null, token: null })
    expect(readToken()).toBeNull()
    expect(store.getState().toast.items).toHaveLength(1)
  })

  it('also ends the session when the answer has no ApiResponse body, like a page of a gateway', async () => {
    fetchMock.mockResolvedValue(new Response('<html>Unauthorized</html>', { status: 401 }))
    const store = setupStore(signedIn())
    wire(store)

    await getProfile('test.jwt.token').catch(() => undefined)

    expect(store.getState().auth.user).toBeNull()
  })

  it('leaves alone a session that started after the request of the refused token', async () => {
    // The slow request carried an old token; meanwhile the user logged in again and has a new token
    fetchMock.mockResolvedValue(unauthorized())
    const store = setupStore(signedIn())
    wire(store)

    await getProfile('old.jwt.token').catch(() => undefined)

    expect(store.getState().auth.user).not.toBeNull()
    expect(store.getState().toast.items).toHaveLength(0)
  })
})
