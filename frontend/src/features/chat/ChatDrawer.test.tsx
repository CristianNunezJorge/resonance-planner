import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import ChatDrawer from './ChatDrawer'

// The chat below 992 px slides from the right (D130), with the focus inside like a window
describe('ChatDrawer', () => {
  it('shows nothing while it is closed', () => {
    render(
      <ChatDrawer open={false} onClose={vi.fn()}>
        <button type="button">Invia</button>
      </ChatDrawer>,
    )

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('opens as a dialog with the focus inside, and closes with Esc, the backdrop or its button', () => {
    const onClose = vi.fn()
    render(
      <ChatDrawer open onClose={onClose}>
        <button type="button">Invia</button>
      </ChatDrawer>,
    )

    const dialog = screen.getByRole('dialog', { name: 'Chat con l’agente' })
    expect(dialog).toHaveAttribute('aria-modal', 'true')
    expect(dialog).toContainElement(document.activeElement as HTMLElement)
    expect(document.body).toHaveClass('modal-open')

    fireEvent.keyDown(document, { key: 'Escape' })
    fireEvent.click(document.querySelector('.offcanvas-backdrop') as HTMLElement)
    fireEvent.click(screen.getByRole('button', { name: 'Chiudi la chat' }))

    expect(onClose).toHaveBeenCalledTimes(3)
  })
})
