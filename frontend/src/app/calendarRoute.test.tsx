import { screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { getCharacters, getGameRules } from '../features/catalog/catalogApi'
import { gameRules } from '../test/fixtures'
import { renderRoutes, signedIn } from '../test/utils'
import { routes } from './routes'

// The calendar page, with FullCalendar, is downloaded only when the calendar opens (D137). Here its module is a small
// page that counts how many times it was loaded
const calendarModule = vi.hoisted(() => ({ loads: 0 }))

vi.mock('../features/calendar/CalendarPage', () => {
  calendarModule.loads += 1
  return { default: () => <h1>Calendario</h1> }
})
vi.mock('../features/catalog/catalogApi', () => ({ getCharacters: vi.fn(), getGameRules: vi.fn() }))

// The order of the tests matters: the module is loaded once per test file, so the first test must run before the
// calendar is ever rendered
describe('the calendar route', () => {
  it('does not load the calendar page together with the other pages', () => {
    // The routes were imported at the top of this file
    expect(routes).not.toHaveLength(0)
    expect(calendarModule.loads).toBe(0)
  })

  it('loads it when the calendar opens, with the loading state and the title meanwhile', async () => {
    vi.mocked(getCharacters).mockResolvedValue([])
    vi.mocked(getGameRules).mockResolvedValue(gameRules)

    renderRoutes(routes, '/calendario', signedIn())

    expect(screen.getByText('Caricamento…')).toBeInTheDocument()
    expect(document.title).toBe('Calendario · Resonance Planner')
    expect(await screen.findByRole('heading', { name: 'Calendario' })).toBeInTheDocument()
    expect(calendarModule.loads).toBe(1)
  })
})
