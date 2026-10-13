import { useEffect, useState } from 'react'
import { useAppDispatch, useAppSelector } from '../app/hooks'
import { toastDismissed, type Toast, type ToastVariant } from '../app/toastSlice'

// A notification stays long enough to be read, then goes away by itself (D69). An error stays longer.
const TOAST_DURATION_MS: Record<ToastVariant, number> = {
  success: 6000,
  info: 6000,
  danger: 10000,
}

function ToastItem({ toast }: { toast: Toast }) {
  const dispatch = useAppDispatch()
  // While the mouse is over a notification, or the keyboard is on its button, it does not close (WCAG 2.2.1)
  const [isPaused, setIsPaused] = useState(false)

  useEffect(() => {
    if (isPaused) {
      return
    }
    const timerId = setTimeout(() => dispatch(toastDismissed(toast.id)), TOAST_DURATION_MS[toast.variant])
    return () => clearTimeout(timerId)
  }, [dispatch, toast.id, toast.variant, isPaused])

  return (
    // The info notification has dark text on a light background: its theme is the light one, so the close icon is dark
    <div
      className={`toast show align-items-center border-0 text-bg-${toast.variant}`}
      data-bs-theme={toast.variant === 'info' ? 'light' : 'dark'}
      // An error is announced at once. The other messages are announced by the polite area around them.
      role={toast.variant === 'danger' ? 'alert' : undefined}
      onMouseEnter={() => setIsPaused(true)}
      onMouseLeave={() => setIsPaused(false)}
      onFocus={() => setIsPaused(true)}
      onBlur={() => setIsPaused(false)}
    >
      <div className="d-flex">
        <div className="toast-body">{toast.message}</div>
        <button
          type="button"
          className="btn-close me-2 m-auto"
          aria-label="Chiudi"
          onClick={() => dispatch(toastDismissed(toast.id))}
        />
      </div>
    </div>
  )
}

// Short notifications of any feature, in the corner of the page (D69)
function ToastContainer() {
  const toasts = useAppSelector((state) => state.toast.items)

  return (
    <div className="toast-container position-fixed bottom-0 end-0 p-3">
      {/* This area is always in the page, empty: a screen reader announces only what is added to it later */}
      <div role="status" aria-live="polite" aria-atomic="false">
        {toasts
          .filter((toast) => toast.variant !== 'danger')
          .map((toast) => (
            <ToastItem key={toast.id} toast={toast} />
          ))}
      </div>
      {toasts
        .filter((toast) => toast.variant === 'danger')
        .map((toast) => (
          <ToastItem key={toast.id} toast={toast} />
        ))}
    </div>
  )
}

export default ToastContainer
