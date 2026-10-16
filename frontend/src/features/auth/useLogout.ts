import { useCallback } from 'react'
import { useNavigate } from 'react-router'
import { useAppDispatch } from '../../app/hooks'
import { toastShown } from '../../app/toastSlice'
import { logout } from './authSlice'

/**
 * Ends the session from a page (F11, F12): first back to the presentation, then the token goes away; in the opposite
 * order the page being left would see a visitor and send them to the login. flushSync makes the new page appear
 * before the await ends: without it, a real browser logs out while the old page is still on screen. The token goes
 * away in any case, then the message is shown.
 */
export function useLogout(): (message: string) => Promise<void> {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()

  return useCallback(
    async (message: string) => {
      try {
        await navigate('/', { flushSync: true })
      } finally {
        dispatch(logout())
        dispatch(toastShown(message, 'info'))
      }
    },
    [dispatch, navigate],
  )
}
