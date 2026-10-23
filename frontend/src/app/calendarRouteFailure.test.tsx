import { screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { renderRoutes, signedIn } from '../test/utils'
import { routes } from './routes'

// The file of the calendar page cannot be downloaded: offline, or a new deploy removed the old file (D137)
vi.mock('../features/calendar/CalendarPage', () => {
  throw new Error('Failed to fetch dynamically imported module')
})

describe('the calendar route when its file does not load', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('shows the error page with the way back, instead of a blank page', async () => {
    // React logs the caught error: silence it to keep the test output clean
    vi.spyOn(console, 'error').mockImplementation(() => {})

    renderRoutes(routes, '/calendario', signedIn())

    expect(await screen.findByRole('heading', { name: 'Qualcosa è andato storto' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Torna alla presentazione' })).toHaveAttribute('href', '/')
  })
})
