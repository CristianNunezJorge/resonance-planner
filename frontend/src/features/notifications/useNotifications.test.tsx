import { act, renderHook } from '@testing-library/react'
import type { ReactNode } from 'react'
import { Provider } from 'react-redux'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { setupStore, type AppStore } from '../../app/store'
import { signedIn } from '../../test/utils'
import { sessionEnded } from '../auth/authSlice'
import { getCalendar } from '../calendar/calendarApi'
import { loadCalendar } from '../calendar/calendarSlice'
import { connectNotifications, type ServerNotification } from './notificationSocket'
import { useNotifications } from './useNotifications'

vi.mock('./notificationSocket', () => ({ connectNotifications: vi.fn() }))
vi.mock('../calendar/calendarApi', () => ({ getCalendar: vi.fn() }))

const WEEK = { from: '2026-10-19', to: '2026-10-25' }
const close = vi.fn()

function renderWith(store: AppStore) {
  const wrapper = ({ children }: { children: ReactNode }) => <Provider store={store}>{children}</Provider>
  return renderHook(() => useNotifications(), { wrapper })
}

// What the server sends, through the callback that the hook gave to the connection
function receive(notification: ServerNotification) {
  const { onNotification } = vi.mocked(connectNotifications).mock.calls[0][0]
  act(() => onNotification(notification))
}

function setVisibility(state: DocumentVisibilityState) {
  vi.spyOn(document, 'visibilityState', 'get').mockReturnValue(state)
  act(() => {
    document.dispatchEvent(new Event('visibilitychange'))
  })
}

describe('useNotifications', () => {
  beforeEach(() => {
    vi.stubEnv('VITE_API_BASE_URL', 'https://api.example.com')
    vi.mocked(connectNotifications).mockReturnValue({ close })
  })

  afterEach(() => {
    vi.unstubAllEnvs()
    vi.restoreAllMocks()
    vi.mocked(connectNotifications).mockReset()
    close.mockReset()
  })

  it('opens one connection with the token of a confirmed session', () => {
    renderWith(setupStore(signedIn()))

    expect(connectNotifications).toHaveBeenCalledTimes(1)
    expect(connectNotifications).toHaveBeenCalledWith(
      expect.objectContaining({ url: 'wss://api.example.com/ws', token: 'test.jwt.token' }),
    )
  })

  it('shows every notification as a short notice', () => {
    const store = setupStore(signedIn())
    renderWith(store)

    receive({ type: 'WAVEPLATE_ALMOST_FULL', message: 'Waveplate quasi piene: 232/240, piene alle 18:42.' })

    expect(store.getState().toast.items).toEqual([
      expect.objectContaining({ message: 'Waveplate quasi piene: 232/240, piene alle 18:42.', variant: 'info' }),
    ])
  })

  it('reloads the calendar when the events changed', async () => {
    vi.mocked(getCalendar).mockResolvedValue({ events: [], tasks: [], pendingProposal: null, planOutdated: false })
    const store = setupStore(signedIn())
    await store.dispatch(loadCalendar(WEEK))
    renderWith(store)

    receive({ type: 'EVENTS_CHANGED', message: 'Gli eventi sono cambiati: il calendario è aggiornato.' })

    await vi.waitFor(() => expect(getCalendar).toHaveBeenCalledTimes(2))
    expect(getCalendar).toHaveBeenLastCalledWith('test.jwt.token', WEEK)
  })

  it('does not connect without a session, or before the server confirmed it', () => {
    renderWith(setupStore())
    renderWith(
      setupStore({ auth: { token: 'saved.jwt.token', user: null, sessionStatus: 'restoring', sessionError: null } }),
    )

    expect(connectNotifications).not.toHaveBeenCalled()
  })

  it('does not connect without the address of the backend', () => {
    vi.stubEnv('VITE_API_BASE_URL', '')

    renderWith(setupStore(signedIn()))

    expect(connectNotifications).not.toHaveBeenCalled()
  })

  it('closes the connection while the page is hidden, and opens a new one when it is visible again', () => {
    renderWith(setupStore(signedIn()))

    setVisibility('hidden')
    expect(close).toHaveBeenCalledTimes(1)

    setVisibility('visible')
    expect(connectNotifications).toHaveBeenCalledTimes(2)
  })

  it('closes the connection when the session ends', () => {
    const store = setupStore(signedIn())
    renderWith(store)

    act(() => {
      store.dispatch(sessionEnded())
    })

    expect(close).toHaveBeenCalledTimes(1)
    expect(connectNotifications).toHaveBeenCalledTimes(1)
  })
})
