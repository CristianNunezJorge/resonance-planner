import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client'
import { setupStore } from '../../app/store'
import { getProfile, login as loginRequest, register as registerRequest } from './authApi'
import { expireSession, login, logout, register, restoreSession, retrySession } from './authSlice'
import { clearToken, readToken, saveToken } from './tokenStorage'
import type { AuthResponse, User } from './types'

vi.mock('./authApi', () => ({ login: vi.fn(), register: vi.fn(), getProfile: vi.fn() }))

const rover: User = {
  id: 7,
  username: 'rover',
  email: 'rover@example.com',
  role: 'USER',
  playTimeMinutes: null,
  onboardingCompleted: false,
}
const answer: AuthResponse = { token: 'fresh.jwt.token', user: rover }

describe('authSlice', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    vi.mocked(loginRequest).mockReset()
    vi.mocked(registerRequest).mockReset()
    vi.mocked(getProfile).mockReset()
  })

  describe('login and register', () => {
    it('keeps the user and the token after a login, and saves the token for the next visit', async () => {
      vi.mocked(loginRequest).mockResolvedValue(answer)
      const store = setupStore()

      await store.dispatch(login({ email: 'rover@example.com', password: 'S3cret-pass' }))

      expect(loginRequest).toHaveBeenCalledWith({ email: 'rover@example.com', password: 'S3cret-pass' })
      expect(store.getState().auth.user).toEqual(rover)
      expect(store.getState().auth.token).toBe('fresh.jwt.token')
      expect(readToken()).toBe('fresh.jwt.token')
    })

    it('logs in the new user after a registration', async () => {
      vi.mocked(registerRequest).mockResolvedValue(answer)
      const store = setupStore()

      await store.dispatch(register({ username: 'rover', email: 'rover@example.com', password: 'S3cret-pass' }))

      expect(store.getState().auth.user).toEqual(rover)
      expect(readToken()).toBe('fresh.jwt.token')
    })

    it('rejects with the code, the message and the fields, and keeps nothing, when the server refuses', async () => {
      vi.mocked(registerRequest).mockRejectedValue(
        new ApiError('VALIDATION_ERROR', 'Alcuni campi non sono validi.', 400, { email: 'Inserisci un indirizzo email valido.' }),
      )
      const store = setupStore()

      const result = await store.dispatch(register({ username: 'rover', email: 'x', password: 'S3cret-pass' }))

      expect(result.type).toBe('auth/register/rejected')
      expect(result.payload).toEqual({
        code: 'VALIDATION_ERROR',
        message: 'Alcuni campi non sono validi.',
        status: 400,
        fields: { email: 'Inserisci un indirizzo email valido.' },
      })
      expect(store.getState().auth.user).toBeNull()
      expect(readToken()).toBeNull()
    })
  })

  describe('logout', () => {
    it('forgets the user and the token, also in the browser', async () => {
      vi.mocked(loginRequest).mockResolvedValue(answer)
      const store = setupStore()
      await store.dispatch(login({ email: 'rover@example.com', password: 'S3cret-pass' }))

      store.dispatch(logout())

      expect(store.getState().auth).toMatchObject({ user: null, token: null })
      expect(readToken()).toBeNull()
    })
  })

  describe('restoreSession', () => {
    it('does nothing and asks nothing when there is no saved token', async () => {
      const store = setupStore()

      await store.dispatch(restoreSession())

      expect(getProfile).not.toHaveBeenCalled()
      expect(store.getState().auth).toMatchObject({ user: null, sessionStatus: 'idle' })
    })

    it('reads the profile with the saved token and restores the user', async () => {
      saveToken('saved.jwt.token')
      vi.mocked(getProfile).mockResolvedValue(rover)
      const store = setupStore()

      const pending = store.dispatch(restoreSession())
      expect(store.getState().auth.sessionStatus).toBe('restoring')
      await pending

      expect(getProfile).toHaveBeenCalledWith('saved.jwt.token')
      expect(store.getState().auth).toMatchObject({ user: rover, token: 'saved.jwt.token', sessionStatus: 'ready' })
    })

    it('forgets a token that the server does not accept anymore', async () => {
      saveToken('expired.jwt.token')
      vi.mocked(getProfile).mockRejectedValue(new ApiError('UNAUTHORIZED', 'Devi effettuare l\'accesso.', 401))
      const store = setupStore()

      await store.dispatch(restoreSession())

      expect(store.getState().auth).toMatchObject({ user: null, token: null, sessionStatus: 'ready' })
      expect(readToken()).toBeNull()
      // The visitor finds the login page: the notice says why
      expect(store.getState().toast.items).toEqual([
        expect.objectContaining({ message: 'La sessione è scaduta: accedi di nuovo.', variant: 'info' }),
      ])
    })

    it('does not remove a token that another tab saved while the request was running', async () => {
      saveToken('old.jwt.token')
      vi.mocked(getProfile).mockImplementation(() => {
        saveToken('newer.jwt.token')
        return Promise.reject(new ApiError('UNAUTHORIZED', 'Devi effettuare l\'accesso.', 401))
      })
      const store = setupStore()

      await store.dispatch(restoreSession())

      expect(store.getState().auth).toMatchObject({ user: null, token: null, sessionStatus: 'ready' })
      expect(readToken()).toBe('newer.jwt.token')
    })

    it('keeps the token and reports the problem when the server cannot be reached, so it can be retried', async () => {
      saveToken('saved.jwt.token')
      vi.mocked(getProfile)
        .mockRejectedValueOnce(new ApiError('NETWORK_ERROR', 'Impossibile contattare il server.', 0))
        .mockResolvedValueOnce(rover)
      const store = setupStore()

      await store.dispatch(restoreSession())
      expect(store.getState().auth).toMatchObject({
        sessionStatus: 'failed',
        sessionError: 'Impossibile contattare il server.',
        user: null,
      })
      expect(readToken()).toBe('saved.jwt.token')

      await store.dispatch(restoreSession())
      expect(store.getState().auth).toMatchObject({ sessionStatus: 'ready', user: rover, sessionError: null })
    })

    it('does not ask twice while the first request is running', async () => {
      saveToken('saved.jwt.token')
      vi.mocked(getProfile).mockResolvedValue(rover)
      const store = setupStore()

      await Promise.all([store.dispatch(restoreSession()), store.dispatch(restoreSession())])

      expect(getProfile).toHaveBeenCalledOnce()
    })
  })

  describe('retrySession', () => {
    it('asks the server again when the token is still saved', async () => {
      saveToken('saved.jwt.token')
      vi.mocked(getProfile).mockResolvedValue(rover)
      const store = setupStore({
        auth: { token: null, user: null, sessionStatus: 'failed', sessionError: 'Impossibile contattare il server.' },
      })

      store.dispatch(retrySession())

      await vi.waitFor(() => expect(store.getState().auth.user).toEqual(rover))
    })

    it('ends the wait when the token disappeared meanwhile, for example after a logout in another tab', () => {
      const store = setupStore({
        auth: { token: null, user: null, sessionStatus: 'failed', sessionError: 'Impossibile contattare il server.' },
      })
      clearToken()

      store.dispatch(retrySession())

      expect(getProfile).not.toHaveBeenCalled()
      expect(store.getState().auth).toMatchObject({ user: null, sessionStatus: 'ready', sessionError: null })
    })
  })

  describe('expireSession', () => {
    it('ends the session of the refused token and tells the user, once', async () => {
      vi.mocked(loginRequest).mockResolvedValue(answer)
      const store = setupStore()
      await store.dispatch(login({ email: 'rover@example.com', password: 'S3cret-pass' }))

      store.dispatch(expireSession('fresh.jwt.token'))
      store.dispatch(expireSession('fresh.jwt.token'))

      expect(store.getState().auth).toMatchObject({ user: null, token: null })
      expect(readToken()).toBeNull()
      expect(store.getState().toast.items).toHaveLength(1)
      expect(store.getState().toast.items[0]).toMatchObject({
        message: 'La sessione è scaduta: accedi di nuovo.',
        variant: 'info',
      })
    })

    it('ignores a late refusal of a token of a session that already ended', async () => {
      vi.mocked(loginRequest).mockResolvedValue(answer)
      const store = setupStore()
      await store.dispatch(login({ email: 'rover@example.com', password: 'S3cret-pass' }))

      // The slow request of a previous session is answered 401 after this new login
      store.dispatch(expireSession('old.jwt.token'))

      expect(store.getState().auth.user).toEqual(rover)
      expect(readToken()).toBe('fresh.jwt.token')
      expect(store.getState().toast.items).toHaveLength(0)
    })

    it('does not throw away the token that another tab saved in the meantime', async () => {
      vi.mocked(loginRequest).mockResolvedValue(answer)
      const store = setupStore()
      await store.dispatch(login({ email: 'rover@example.com', password: 'S3cret-pass' }))
      saveToken('other.tab.token')

      store.dispatch(expireSession('fresh.jwt.token'))

      expect(store.getState().auth.user).toBeNull()
      expect(readToken()).toBe('other.tab.token')
    })
  })
})
