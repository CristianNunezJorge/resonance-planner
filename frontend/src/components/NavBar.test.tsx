import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { Provider } from 'react-redux'
import { createMemoryRouter, Outlet } from 'react-router'
import { RouterProvider } from 'react-router/dom'
import { afterEach, describe, expect, it } from 'vitest'
import { setupStore, type RootState } from '../app/store'
import { saveToken, readToken } from '../features/auth/tokenStorage'
import { signedIn } from '../test/utils'
import NavBar from './NavBar'

// The navbar above a few pages, to see the links work
function renderNav(path: string, preloadedState?: Partial<RootState>) {
  const store = setupStore(preloadedState)
  const router = createMemoryRouter(
    [
      {
        element: (
          <>
            <NavBar />
            <Outlet />
          </>
        ),
        children: [
          { path: '/', element: <h1>Presentazione</h1> },
          { path: '/calendario', element: <h1>Calendario</h1> },
          { path: '/profilo', element: <h1>Profilo</h1> },
          { path: '/login', element: <h1>Accedi</h1> },
        ],
      },
    ],
    { initialEntries: [path] },
  )
  render(
    <Provider store={store}>
      <RouterProvider router={router} />
    </Provider>,
  )
  return { store, router }
}

describe('NavBar', () => {
  afterEach(() => {
    localStorage.clear()
  })

  it('offers login and registration to a visitor, and nothing of the private area', () => {
    renderNav('/')

    expect(screen.getByRole('link', { name: 'Resonance Planner' })).toHaveAttribute('href', '/')
    expect(screen.getByRole('link', { name: 'Accedi' })).toHaveAttribute('href', '/login')
    expect(screen.getByRole('link', { name: 'Registrati' })).toHaveAttribute('href', '/registrati')
    expect(screen.queryByRole('link', { name: 'Calendario' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Esci' })).not.toBeInTheDocument()
  })

  it('shows the pages of the account, the username and the logout button to a logged user', () => {
    renderNav('/calendario', signedIn())

    expect(screen.getByRole('link', { name: 'Calendario' })).toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('link', { name: 'Roster' })).toHaveAttribute('href', '/roster')
    expect(screen.getByRole('link', { name: 'Profilo' })).toHaveAttribute('href', '/profilo')
    expect(screen.getByText('rover')).toBeInTheDocument()
    expect(screen.getByText('Account:')).toHaveClass('visually-hidden')
    expect(screen.getByRole('button', { name: 'Esci' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Accedi' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Eventi' })).not.toBeInTheDocument()
  })

  it('says that the account is a guest of the demo (D134)', () => {
    renderNav('/calendario', signedIn({ username: 'ospite-k3x9qz', role: 'GUEST' }))

    expect(screen.getByText('ospite-k3x9qz')).toBeInTheDocument()
    expect(screen.getByText('(ospite)')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Eventi' })).not.toBeInTheDocument()
  })

  it('points to the guided setup instead of the calendar while the setup is still to do', () => {
    renderNav('/', signedIn({ onboardingCompleted: false }))

    expect(screen.getByRole('link', { name: 'Configurazione' })).toHaveAttribute('href', '/configurazione')
    expect(screen.queryByRole('link', { name: 'Calendario' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Roster' })).not.toBeInTheDocument()
  })

  it('shows the events management only to the admin', () => {
    renderNav('/', signedIn({ role: 'ADMIN' }))

    expect(screen.getByRole('link', { name: 'Eventi' })).toHaveAttribute('href', '/admin/eventi')
  })

  it('shows neither the visitor links nor the private ones while the saved session is being checked', () => {
    renderNav('/', { auth: { token: 't', user: null, sessionStatus: 'restoring', sessionError: null } })

    expect(screen.queryByRole('link', { name: 'Accedi' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Calendario' })).not.toBeInTheDocument()
  })

  it('logs out: back to the presentation, token gone from the browser, and a notice', async () => {
    saveToken('test.jwt.token')
    const { store, router } = renderNav('/profilo', signedIn())

    fireEvent.click(screen.getByRole('button', { name: 'Esci' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/'))
    await waitFor(() => expect(store.getState().auth.user).toBeNull())
    expect(readToken()).toBeNull()
    expect(screen.getByRole('link', { name: 'Accedi' })).toBeInTheDocument()
    expect(store.getState().toast.items).toEqual([expect.objectContaining({ message: 'Sei uscito dall\'account.' })])
  })

  it('labels the menu button according to what it does', () => {
    renderNav('/', signedIn())

    fireEvent.click(screen.getByRole('button', { name: 'Apri il menu' }))

    expect(screen.getByRole('button', { name: 'Chiudi il menu' })).toHaveAttribute('aria-expanded', 'true')
  })

  it('closes the menu when the link of the page already open is tapped', () => {
    renderNav('/profilo', signedIn())
    fireEvent.click(screen.getByRole('button', { name: 'Apri il menu' }))

    fireEvent.click(screen.getByRole('link', { name: 'Profilo' }))

    expect(screen.getByRole('button', { name: 'Apri il menu' })).toHaveAttribute('aria-expanded', 'false')
  })

  it('does not open the menu again when the user comes back to a page with the Back button', async () => {
    const { router } = renderNav('/calendario', signedIn())
    fireEvent.click(screen.getByRole('button', { name: 'Apri il menu' }))
    // The page changes without a tap on a link of the menu (a redirect, a link in the page)
    await act(() => router.navigate('/profilo'))
    expect(router.state.location.pathname).toBe('/profilo')

    await act(() => router.navigate(-1))

    await waitFor(() => expect(router.state.location.pathname).toBe('/calendario'))
    expect(screen.getByRole('button', { name: 'Apri il menu' })).toHaveAttribute('aria-expanded', 'false')
  })

  it('opens and closes the menu of the small screens, and closes it when the page changes', () => {
    const { router } = renderNav('/', signedIn())
    const toggler = screen.getByRole('button', { name: 'Apri il menu' })
    expect(toggler).toHaveAttribute('aria-expanded', 'false')

    fireEvent.click(toggler)
    expect(toggler).toHaveAttribute('aria-expanded', 'true')
    expect(document.getElementById('main-menu')).toHaveClass('show')

    fireEvent.click(toggler)
    expect(toggler).toHaveAttribute('aria-expanded', 'false')

    fireEvent.click(toggler)
    fireEvent.click(screen.getByRole('link', { name: 'Profilo' }))
    expect(router.state.location.pathname).toBe('/profilo')
    expect(screen.getByRole('button', { name: 'Apri il menu' })).toHaveAttribute('aria-expanded', 'false')
  })
})
