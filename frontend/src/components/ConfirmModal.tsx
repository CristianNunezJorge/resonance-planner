import { useId, useRef, type MouseEvent } from 'react'
import Modal from './Modal'

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

// A confirmation for the actions that cannot be undone (D69, D116): a Modal with the focus on "Annulla", the safe
// choice. Tab and Shift+Tab move between the two buttons and never leave the window.
function ConfirmModal({ open, title, message, confirmLabel, busy = false, onConfirm, onCancel }: ConfirmModalProps) {
  const messageId = useId()
  const cancelRef = useRef<HTMLButtonElement>(null)

  // aria-disabled and not disabled: a disabled button loses the focus, and with it the keys of the window
  const unlessBusy = (action: () => void) => (event: MouseEvent<HTMLButtonElement>) => {
    if (busy) {
      event.preventDefault()
      return
    }
    action()
  }

  return (
    <Modal
      open={open}
      title={title}
      busy={busy}
      onClose={onCancel}
      initialFocusRef={cancelRef}
      describedBy={messageId}
      footer={
        <>
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
            type="button"
            className="btn btn-danger"
            onClick={unlessBusy(onConfirm)}
            aria-disabled={busy || undefined}
          >
            {confirmLabel}
          </button>
        </>
      }
    >
      <p id={messageId} className="mb-0">
        {message}
      </p>
    </Modal>
  )
}

export default ConfirmModal
