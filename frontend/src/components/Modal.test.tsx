import { fireEvent, render, screen } from '@testing-library/react'
import { useRef, useState } from 'react'
import { describe, expect, it, vi } from 'vitest'
import Modal from './Modal'

function FormWindow({ busy = false, onClose = vi.fn() }: { busy?: boolean; onClose?: () => void }) {
  const [open, setOpen] = useState(false)
  const nameRef = useRef<HTMLInputElement>(null)
  return (
    <>
      <button type="button" onClick={() => setOpen(true)}>
        Apri
      </button>
      <Modal
        open={open}
        title="Nuova attività"
        busy={busy}
        initialFocusRef={nameRef}
        onClose={() => {
          onClose()
          setOpen(false)
        }}
        footer={<button type="button">Salva</button>}
      >
        <label htmlFor="name">Titolo</label>
        <input id="name" ref={nameRef} />
        <button type="button" disabled>
          Spento
        </button>
        <a href="#nota">Nota</a>
      </Modal>
    </>
  )
}

describe('Modal', () => {
  it('shows nothing while it is closed', () => {
    render(
      <Modal open={false} title="Titolo" onClose={vi.fn()} footer={null}>
        Contenuto
      </Modal>,
    )

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('opens as a dialog named by its title, with the focus on the chosen element', () => {
    render(<FormWindow />)
    fireEvent.click(screen.getByRole('button', { name: 'Apri' }))

    const dialog = screen.getByRole('dialog', { name: 'Nuova attività' })
    expect(dialog).toHaveAttribute('aria-modal', 'true')
    expect(screen.getByLabelText('Titolo')).toHaveFocus()
  })

  it('puts the focus on the first element that can take it when none is chosen', () => {
    render(
      <Modal open title="Dettaglio" onClose={vi.fn()} footer={<button type="button">Chiudi</button>}>
        <p>Solo testo</p>
      </Modal>,
    )

    expect(screen.getByRole('button', { name: 'Chiudi' })).toHaveFocus()
  })

  it('keeps Tab and Shift+Tab inside the window, skipping what is disabled', () => {
    render(<FormWindow />)
    fireEvent.click(screen.getByRole('button', { name: 'Apri' }))
    const dialog = screen.getByRole('dialog')
    const save = screen.getByRole('button', { name: 'Salva' })

    save.focus()
    fireEvent.keyDown(dialog, { key: 'Tab' })
    expect(screen.getByLabelText('Titolo')).toHaveFocus()

    fireEvent.keyDown(dialog, { key: 'Tab', shiftKey: true })
    expect(save).toHaveFocus()

    // Inside the window Tab moves as usual
    screen.getByLabelText('Titolo').focus()
    fireEvent.keyDown(dialog, { key: 'Tab' })
    expect(screen.getByLabelText('Titolo')).toHaveFocus()
  })

  it('closes with Esc and gives the focus back to the button that opened it', () => {
    const onClose = vi.fn()
    render(<FormWindow onClose={onClose} />)
    const opener = screen.getByRole('button', { name: 'Apri' })
    opener.focus()
    fireEvent.click(opener)

    fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Escape' })

    expect(onClose).toHaveBeenCalledOnce()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(opener).toHaveFocus()
  })

  it('gives the focus to the content of the page when the element that opened it is gone', () => {
    function Page() {
      const [open, setOpen] = useState(false)
      const [showOpener, setShowOpener] = useState(true)
      return (
        <main tabIndex={-1} aria-label="Contenuto">
          {showOpener && (
            <button type="button" onClick={() => setOpen(true)}>
              Elemento del calendario
            </button>
          )}
          <Modal
            open={open}
            title="Dettaglio"
            onClose={() => {
              // Like a calendar drawn again after a change: the element that opened the window is replaced
              setShowOpener(false)
              setOpen(false)
            }}
            footer={<button type="button">Chiudi</button>}
          >
            Testo
          </Modal>
        </main>
      )
    }
    render(<Page />)
    const opener = screen.getByRole('button', { name: 'Elemento del calendario' })
    opener.focus()
    fireEvent.click(opener)

    fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Escape' })

    expect(screen.getByRole('main', { name: 'Contenuto' })).toHaveFocus()
  })

  it('keeps Esc and Tab working when a click on the backdrop moved the focus out of the window', () => {
    const onClose = vi.fn()
    render(<FormWindow onClose={onClose} />)
    fireEvent.click(screen.getByRole('button', { name: 'Apri' }))
    expect(document.body).toHaveClass('modal-open')

    // The focus is on the page, not inside the window: Tab brings it back inside
    ;(document.activeElement as HTMLElement).blur()
    fireEvent.keyDown(document.body, { key: 'Tab' })
    expect(screen.getByLabelText('Titolo')).toHaveFocus()

    ;(document.activeElement as HTMLElement).blur()
    fireEvent.keyDown(document.body, { key: 'Tab', shiftKey: true })
    expect(screen.getByRole('button', { name: 'Salva' })).toHaveFocus()

    ;(document.activeElement as HTMLElement).blur()
    fireEvent.keyDown(document.body, { key: 'Escape' })
    expect(onClose).toHaveBeenCalledOnce()
    expect(document.body).not.toHaveClass('modal-open')
  })

  it('does not close with Esc while something is being saved', () => {
    const onClose = vi.fn()
    render(<FormWindow busy onClose={onClose} />)
    fireEvent.click(screen.getByRole('button', { name: 'Apri' }))

    fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Escape' })

    expect(onClose).not.toHaveBeenCalled()
    expect(screen.getByRole('dialog')).toHaveAttribute('aria-busy', 'true')
  })

  it('is drawn at the end of the page', () => {
    const { container } = render(
      <div>
        <Modal open title="Fuori" onClose={vi.fn()} footer={null}>
          Testo
        </Modal>
      </div>,
    )

    expect(container).not.toContainElement(screen.getByRole('dialog'))
  })
})
