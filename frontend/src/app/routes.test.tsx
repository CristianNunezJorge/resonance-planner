import { render, screen } from '@testing-library/react'
import { Provider } from 'react-redux'
import { createMemoryRouter } from 'react-router'
import { RouterProvider } from 'react-router/dom'
import { describe, expect, it } from 'vitest'
import { routes } from './routes'
import { setupStore } from './store'

function renderAt(path: string) {
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  render(
    <Provider store={setupStore()}>
      <RouterProvider router={router} />
    </Provider>,
  )
}

describe('routes', () => {
  it('shows the landing page at the root path', async () => {
    renderAt('/')

    expect(await screen.findByRole('heading', { name: 'Resonance Planner' })).toBeInTheDocument()
  })

  it('shows the not found page with a link home for an unknown path', async () => {
    renderAt('/pagina-inesistente')

    expect(await screen.findByRole('heading', { name: 'Pagina non trovata' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Torna alla presentazione' })).toHaveAttribute('href', '/')
  })
})
