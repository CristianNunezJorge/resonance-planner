import { useEffect, useEffectEvent, useId, useRef, type ReactNode, type RefObject } from 'react'
import { createPortal } from 'react-dom'

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

const FOCUSABLE = 'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'

// A window over the page (D69, D116, D120): the styles of a Bootstrap modal, driven by React instead of the JavaScript
// of Bootstrap, drawn at the end of the page (a portal). The focus goes inside when it opens, stays inside while it
// is open (WCAG 2.1.2) and goes back to the element that opened it when it closes, if it is still there (WCAG 2.4.3).
// The keys are heard on the whole document: a click on the backdrop moves the focus to the body, and Esc and Tab
// must still work. While it is open the page behind does not scroll (modal-open of Bootstrap).
function Modal({ open, title, onClose, busy = false, initialFocusRef, describedBy, size, footer, children }: ModalProps) {
  const titleId = useId()
  const dialogRef = useRef<HTMLDivElement>(null)

  const focusableInside = () => [...(dialogRef.current?.querySelectorAll<HTMLElement>(FOCUSABLE) ?? [])]

  // Effect events read the latest props without being dependencies: the effect below runs only when the window opens
  const focusInside = useEffectEvent(() => {
    const first = initialFocusRef?.current ?? focusableInside()[0]
    first?.focus()
  })

  const handleKeyDown = useEffectEvent((event: KeyboardEvent) => {
    if (event.key === 'Escape') {
      event.stopPropagation()
      if (!busy) {
        onClose()
      }
      return
    }
    if (event.key !== 'Tab') {
      return
    }
    const focusable = focusableInside()
    if (focusable.length === 0) {
      event.preventDefault()
      return
    }
    const first = focusable[0]
    const last = focusable[focusable.length - 1]
    const active = document.activeElement
    // From outside the window, from the last element or from the first with Shift: the focus goes round inside
    if (!dialogRef.current?.contains(active)) {
      event.preventDefault()
      ;(event.shiftKey ? last : first).focus()
    } else if (!event.shiftKey && active === last) {
      event.preventDefault()
      first.focus()
    } else if (event.shiftKey && active === first) {
      event.preventDefault()
      last.focus()
    }
  })

  useEffect(() => {
    if (!open) {
      return
    }
    const opener = document.activeElement instanceof HTMLElement ? document.activeElement : null
    focusInside()
    const onKeyDown = (event: KeyboardEvent) => handleKeyDown(event)
    document.addEventListener('keydown', onKeyDown)
    document.body.classList.add('modal-open')
    return () => {
      document.removeEventListener('keydown', onKeyDown)
      document.body.classList.remove('modal-open')
      // A list drawn again after a change may have replaced the element: then the focus goes to the content of the
      // page (the main of RootLayout can take it), never to the top of the document
      if (opener && opener !== document.body && opener.isConnected) {
        opener.focus()
      } else {
        document.querySelector<HTMLElement>('main')?.focus()
      }
    }
  }, [open])

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
