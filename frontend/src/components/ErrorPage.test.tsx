import { render, screen } from '@testing-library/react'
import { createMemoryRouter } from 'react-router'
import { RouterProvider } from 'react-router/dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import ErrorPage from './ErrorPage'

function BrokenPage(): never {
  throw new Error('Page crashed')
}

describe('ErrorPage', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('replaces a crashed page with an Italian message and a link home', async () => {
    // React logs the caught error: silence it to keep the test output clean
    vi.spyOn(console, 'error').mockImplementation(() => {})
    const router = createMemoryRouter([
      { path: '/', element: <BrokenPage />, errorElement: <ErrorPage /> },
    ])

    render(<RouterProvider router={router} />)

    expect(await screen.findByRole('heading', { name: 'Qualcosa è andato storto' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Torna alla presentazione' })).toHaveAttribute('href', '/')
    expect(screen.queryByText('Page crashed')).not.toBeInTheDocument()
  })
})
