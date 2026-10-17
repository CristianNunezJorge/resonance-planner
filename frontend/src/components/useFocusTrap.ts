import { useEffect, useEffectEvent, type RefObject } from 'react'

interface FocusTrapOptions {
  onClose: () => void
  // While something is saved Esc does nothing
  busy?: boolean
  // The element that gets the focus when it opens; otherwise the first one that can take it
  initialFocusRef?: RefObject<HTMLElement | null>
}

const FOCUSABLE = 'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'

/**
 * The focus of a window over the page (D69, D120, D130), used by Modal and by the chat that slides from the side: the
 * focus goes inside when it opens, stays inside while it is open (WCAG 2.1.2) and goes back to the element that
 * opened it when it closes, if it is still there (WCAG 2.4.3). The keys are heard on the whole document: a click on
 * the backdrop moves the focus to the body, and Esc and Tab must still work. While it is open the page behind does
 * not scroll (modal-open of Bootstrap).
 */
export function useFocusTrap(open: boolean, containerRef: RefObject<HTMLElement | null>, options: FocusTrapOptions) {
  const { onClose, busy = false, initialFocusRef } = options

  const focusableInside = () => [...(containerRef.current?.querySelectorAll<HTMLElement>(FOCUSABLE) ?? [])]

  // Effect events read the latest options without being dependencies: the effect below runs only when it opens
  const focusInside = useEffectEvent(() => {
    const first = initialFocusRef?.current ?? focusableInside()[0]
    first?.focus()
  })

  const handleKeyDown = useEffectEvent((event: KeyboardEvent) => {
    // Esc that only ends a composition of characters (IME) does not close the window
    if (event.key === 'Escape' && !event.isComposing) {
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
    if (!containerRef.current?.contains(active)) {
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
}
