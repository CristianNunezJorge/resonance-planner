import { act, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import LoadingState from './LoadingState'

describe('LoadingState', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('announces the loading to screen readers', () => {
    render(<LoadingState />)

    expect(screen.getByRole('status')).toHaveTextContent('Caricamento…')
  })

  it('explains the slow start of the free server after a few seconds', () => {
    render(<LoadingState />)
    expect(screen.queryByText(/Il server si sta avviando/)).not.toBeInTheDocument()

    act(() => {
      vi.advanceTimersByTime(5000)
    })

    expect(screen.getByText('Il server si sta avviando: può volerci fino a tre minuti.')).toBeInTheDocument()
  })
})
