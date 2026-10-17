import { useId, useRef, type ReactNode, type RefObject } from 'react'
import { createPortal } from 'react-dom'
import { useFocusTrap } from './useFocusTrap'

interface ModalProps {
  open: boolean
  title: string
  onClose: () => void
  // While something is saved Esc does nothing; the buttons decide what they do themselves
  busy?: boolean
  // The element that gets the focus when the window opens; otherwise the first one that can take it
  initialFocusRef?: RefObject<HTMLElement | null>
  // The id of the text that describes the window, read by screen readers after the title
  describedBy?: string
  // 'lg' for a long form
  size?: 'lg'
  footer: ReactNode
  children: ReactNode
}

// A window over the page (D69, D116, D120): the styles of a Bootstrap modal, driven by React instead of the JavaScript
// of Bootstrap, drawn at the end of the page (a portal). The focus stays inside while it is open (useFocusTrap).
function Modal({ open, title, onClose, busy = false, initialFocusRef, describedBy, size, footer, children }: ModalProps) {
  const titleId = useId()
  const dialogRef = useRef<HTMLDivElement>(null)

  useFocusTrap(open, dialogRef, { onClose, busy, initialFocusRef })

  if (!open) {
    return null
  }

  const dialogClass = ['modal-dialog modal-dialog-centered modal-dialog-scrollable', size && `modal-${size}`]
    .filter(Boolean)
    .join(' ')

  return createPortal(
    <>
      <div className="modal-backdrop fade show" />
      <div
        ref={dialogRef}
        className="modal fade show d-block"
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={describedBy}
        aria-busy={busy || undefined}
      >
        <div className={dialogClass}>
          <div className="modal-content">
            <div className="modal-header">
              <h2 id={titleId} className="modal-title h5">
                {title}
              </h2>
            </div>
            <div className="modal-body">{children}</div>
            {footer && <div className="modal-footer">{footer}</div>}
          </div>
        </div>
      </div>
    </>,
    document.body,
  )
}

export default Modal
