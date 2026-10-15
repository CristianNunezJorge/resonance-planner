import { useEffect, useId, useRef, type KeyboardEvent, type MouseEvent } from 'react'
import { createPortal } from 'react-dom'

interface ConfirmModalProps {
  open: boolean
  title: string
  message: string
  confirmLabel: string
  // While the action runs both choices do nothing and Esc does nothing; they stay focusable, so the focus is not lost
  busy?: boolean
  onConfirm: () => void
  onCancel: () => void
}

// A confirmation for the actions that cannot be undone (D69, D116): the styles of a Bootstrap modal, driven by React
// instead of the JavaScript of Bootstrap, drawn at the end of the page (a portal) so its title is not inside the
// card that opened it. The focus starts on "Annulla", the safe choice, stays inside the window while it is open
// (WCAG 2.1.2) and goes back to the button that opened it when it closes, if that button is still there (WCAG 2.4.3).
function ConfirmModal({ open, title, message, confirmLabel, busy = false, onConfirm, onCancel }: ConfirmModalProps) {
  const titleId = useId()
  const messageId = useId()
  const cancelRef = useRef<HTMLButtonElement>(null)
  const confirmRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    if (!open) {
      return
    }
    const opener = document.activeElement instanceof HTMLElement ? document.activeElement : null
    cancelRef.current?.focus()
    return () => {
      if (opener?.isConnected) {
        opener.focus()
      }
    }
  }, [open])

  if (!open) {
    return null
  }

  const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (event.key === 'Escape' && !busy) {
      event.stopPropagation()
      onCancel()
      return
    }
    if (event.key === 'Tab') {
      // Two buttons: Tab and Shift+Tab move between them and never leave the window
      event.preventDefault()
      const next = document.activeElement === cancelRef.current ? confirmRef.current : cancelRef.current
      next?.focus()
    }
  }

  // aria-disabled and not disabled: a disabled button loses the focus, and with it the keys of the window
  const unlessBusy = (action: () => void) => (event: MouseEvent<HTMLButtonElement>) => {
    if (busy) {
      event.preventDefault()
      return
    }
    action()
  }

  return createPortal(
    <>
      <div className="modal-backdrop fade show" />
      <div
        className="modal fade show d-block"
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={messageId}
        aria-busy={busy || undefined}
        onKeyDown={handleKeyDown}
      >
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header">
              <h2 id={titleId} className="modal-title h5">
                {title}
              </h2>
            </div>
            <div className="modal-body">
              <p id={messageId} className="mb-0">
                {message}
              </p>
            </div>
            <div className="modal-footer">
              <button
                ref={cancelRef}
                type="button"
                className="btn btn-outline-light"
                onClick={unlessBusy(onCancel)}
                aria-disabled={busy || undefined}
              >
                Annulla
              </button>
              <button
                ref={confirmRef}
                type="button"
                className="btn btn-danger"
                onClick={unlessBusy(onConfirm)}
                aria-disabled={busy || undefined}
              >
                {confirmLabel}
              </button>
            </div>
          </div>
        </div>
      </div>
    </>,
    document.body,
  )
}

export default ConfirmModal
