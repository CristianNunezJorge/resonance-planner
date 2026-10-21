import { useEffect } from 'react'
import { apiSocketUrl } from '../../api/client'
import { useAppDispatch, useAppSelector } from '../../app/hooks'
import { toastShown } from '../../app/toastSlice'
import { usePageVisible } from '../../components/usePageVisible'
import { reloadCalendar } from '../calendar/calendarSlice'
import { connectNotifications } from './notificationSocket'

const SOCKET_PATH = '/ws'

/**
 * The real-time notifications (D135), for the whole app: one connection while the server has confirmed the session
 * and the page is visible, like the Waveplate of D120. It closes when the tab is hidden, at logout and when the
 * session ends; the free server can then sleep. Every notification becomes a short notice (D69).
 */
export function useNotifications(): void {
  const dispatch = useAppDispatch()
  const token = useAppSelector((state) => state.auth.token)
  // A saved token is used only after GET /api/me confirmed it (D103)
  const userId = useAppSelector((state) => state.auth.user?.id)
  const isVisible = usePageVisible()

  useEffect(() => {
    const url = apiSocketUrl(SOCKET_PATH)
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
    })
    return () => connection.close()
  }, [dispatch, token, userId, isVisible])
}
