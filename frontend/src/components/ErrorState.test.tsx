import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import ErrorState from './ErrorState'

describe('ErrorState', () => {
  it('shows the message and lets the user try again', () => {
    const onRetry = vi.fn()
    render(<ErrorState message="Impossibile contattare il server." onRetry={onRetry} />)

    expect(screen.getByRole('alert')).toHaveTextContent('Impossibile contattare il server.')
    fireEvent.click(screen.getByRole('button', { name: 'Riprova' }))

    expect(onRetry).toHaveBeenCalledOnce()
  })
})
