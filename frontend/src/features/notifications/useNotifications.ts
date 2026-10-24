import { useEffect, useState } from 'react'
import { apiSocketUrl } from '../../api/client'
import { useAppDispatch, useAppSelector } from '../../app/hooks'
import { toastShown } from '../../app/toastSlice'
import { usePageVisible } from '../../components/usePageVisible'
import { reloadCalendar } from '../calendar/calendarSlice'
import { connectNotifications, type ConnectionStatus } from './notificationSocket'

const SOCKET_PATH = '/ws'

// 'off': nobody is logged in, or the page is hidden (D139: the footer says it)
export type NotificationStatus = 'off' | ConnectionStatus

/**
 * The real-time notifications (D135), for the whole app: one connection while the server has confirmed the session
 * and the page is visible, like the Waveplate of D120. It closes when the tab is hidden, at logout and when the
 * session ends; the free server can then sleep. Every notification becomes a short notice (D69). The state of the
 * connection is returned for the footer.
 */
export function useNotifications(): NotificationStatus {
  const dispatch = useAppDispatch()
  const token = useAppSelector((state) => state.auth.token)
  // A saved token is used only after GET /api/me confirmed it (D103)
  const userId = useAppSelector((state) => state.auth.user?.id)
  const isVisible = usePageVisible()
  const url = apiSocketUrl(SOCKET_PATH)
  // The state belongs to the connection of one token: the one of an old session is never shown
  const [socket, setSocket] = useState<{ token: string; status: ConnectionStatus } | null>(null)
  const isLoggedIn = Boolean(token) && userId !== undefined

  useEffect(() => {
    if (!token || userId === undefined || !isVisible || !url) {
      return
    }
    const connection = connectNotifications({
      url,
      token,
      onNotification: ({ type, message }) => {
        dispatch(toastShown(message, 'info'))
        if (type === 'EVENTS_CHANGED') {
          void dispatch(reloadCalendar())
        }
      },
      onStatus: (status) => setSocket({ token, status }),
    })
    return () => {
      connection.close()
      // The same token comes back when the page is visible again: without this the footer would say "collegate"
      // while the new connection is still opening
      setSocket(null)
    }
  }, [dispatch, token, userId, isVisible, url])

  if (!isLoggedIn || !isVisible) {
    return 'off'
  }
  // Without the address of the backend the app cannot open the connection at all
  if (!url) {
    return 'stopped'
  }
  return socket?.token === token ? socket.status : 'connecting'
}
