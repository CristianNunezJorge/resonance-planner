import { useEffect, useRef } from 'react'
import { Navigate } from 'react-router'
import { useAppDispatch } from '../app/hooks'
import { toastShown, type ToastVariant } from '../app/toastSlice'

interface RedirectWithToastProps {
  to: string
  message: string
  variant?: ToastVariant
}

// Redirects and tells the user why. The toast is shown once, even when React runs the effect twice (StrictMode).
function RedirectWithToast({ to, message, variant = 'info' }: RedirectWithToastProps) {
  const dispatch = useAppDispatch()
  const shown = useRef(false)

  useEffect(() => {
    if (!shown.current) {
      shown.current = true
      dispatch(toastShown(message, variant))
    }
  }, [dispatch, message, variant])

  return <Navigate to={to} replace />
}

export default RedirectWithToast
