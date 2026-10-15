import { fireEvent, render, screen } from '@testing-library/react'
import { useState } from 'react'
import { describe, expect, it, vi } from 'vitest'
import ConfirmModal from './ConfirmModal'

// A page with the button that opens the window, like the card of the roster
function Harness({ onConfirm, busy = false }: { onConfirm: () => void; busy?: boolean }) {
  const [open, setOpen] = useState(false)
  return (
    <>
      <button type="button" onClick={() => setOpen(true)}>
        Rimuovi Jinhsi
      </button>
      <ConfirmModal
        open={open}
        title="Rimuovere Jinhsi?"
        message="Il personaggio esce dal roster."
        confirmLabel="Rimuovi"
        busy={busy}
        onConfirm={onConfirm}
        onCancel={() => setOpen(false)}
      />
    </>
  )
}

function openWindow() {
  const opener = screen.getByRole('button', { name: 'Rimuovi Jinhsi' })
  opener.focus()
  fireEvent.click(opener)
  return opener
}

describe('ConfirmModal', () => {
  it('shows nothing while it is closed', () => {
    render(<Harness onConfirm={vi.fn()} />)

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('opens as a dialog with its title and message and the focus on the safe choice', () => {
    render(<Harness onConfirm={vi.fn()} />)

    openWindow()

    const dialog = screen.getByRole('dialog', { name: 'Rimuovere Jinhsi?' })
    expect(dialog).toHaveAttribute('aria-modal', 'true')
    expect(dialog).toHaveAccessibleDescription('Il personaggio esce dal roster.')
    expect(screen.getByRole('button', { name: 'Annulla' })).toHaveFocus()
  })

  it('confirms with its button', () => {
    const onConfirm = vi.fn()
    render(<Harness onConfirm={onConfirm} />)
    openWindow()

    fireEvent.click(screen.getByRole('button', { name: 'Rimuovi' }))

    expect(onConfirm).toHaveBeenCalledOnce()
  })

  it('closes with Annulla and with Esc, and gives the focus back to the button that opened it', () => {
    const onConfirm = vi.fn()
    render(<Harness onConfirm={onConfirm} />)

    const opener = openWindow()
    fireEvent.click(screen.getByRole('button', { name: 'Annulla' }))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(opener).toHaveFocus()

    openWindow()
    fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Escape' })
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(opener).toHaveFocus()
    expect(onConfirm).not.toHaveBeenCalled()
  })

  it('keeps Tab inside the window', () => {
    render(<Harness onConfirm={vi.fn()} />)
    openWindow()
    const cancel = screen.getByRole('button', { name: 'Annulla' })
    const confirm = screen.getByRole('button', { name: 'Rimuovi' })

    confirm.focus()
    fireEvent.keyDown(confirm, { key: 'Tab' })
    expect(cancel).toHaveFocus()

    fireEvent.keyDown(cancel, { key: 'Tab', shiftKey: true })
    expect(confirm).toHaveFocus()
  })

  it('does nothing while the action runs, without losing the focus', () => {
    const onConfirm = vi.fn()
    render(<Harness onConfirm={onConfirm} busy />)
    openWindow()
    const cancel = screen.getByRole('button', { name: 'Annulla' })

    expect(cancel).toHaveAttribute('aria-disabled', 'true')
    expect(screen.getByRole('button', { name: 'Rimuovi' })).toHaveAttribute('aria-disabled', 'true')
    expect(screen.getByRole('dialog')).toHaveAttribute('aria-busy', 'true')
    fireEvent.click(screen.getByRole('button', { name: 'Rimuovi' }))
    fireEvent.click(cancel)
    fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Escape' })

    expect(onConfirm).not.toHaveBeenCalled()
    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(cancel).toHaveFocus()
  })

  it('is drawn at the end of the page, outside the element that opened it', () => {
    const { container } = render(<Harness onConfirm={vi.fn()} />)
    openWindow()

    expect(container.querySelector('[role="dialog"]')).toBeNull()
    expect(document.body.querySelector('[role="dialog"]')).not.toBeNull()
  })
})
