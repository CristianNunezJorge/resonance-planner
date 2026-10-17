import { useRef, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { useFocusTrap } from '../../components/useFocusTrap'

interface ChatDrawerProps {
  open: boolean
  onClose: () => void
  children: ReactNode
}

// The chat below 992 px (D66, D130): a panel that slides from the right, with the styles of the Offcanvas of
// Bootstrap driven by React like Modal, drawn at the end of the page. The focus stays inside while it is open.
function ChatDrawer({ open, onClose, children }: ChatDrawerProps) {
  const panelRef = useRef<HTMLDivElement>(null)

  useFocusTrap(open, panelRef, { onClose })

  if (!open) {
    return null
  }

  return createPortal(
    <>
      <div className="offcanvas-backdrop fade show" onClick={onClose} />
      <div
        ref={panelRef}
        className="offcanvas offcanvas-end show chat-drawer"
        role="dialog"
        aria-modal="true"
        aria-label="Chat con l’agente"
      >
        <div className="offcanvas-header justify-content-end pb-0">
          <button type="button" className="btn-close" aria-label="Chiudi la chat" onClick={onClose} />
        </div>
        <div className="offcanvas-body">{children}</div>
      </div>
    </>,
    document.body,
  )
}

export default ChatDrawer
