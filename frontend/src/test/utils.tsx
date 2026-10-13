import { render } from '@testing-library/react'
import { Provider } from 'react-redux'
import { createMemoryRouter, type RouteObject } from 'react-router'
import { RouterProvider } from 'react-router/dom'
import { setupStore, type AppStore, type RootState } from '../app/store'
import type { User } from '../features/auth/types'

export const rover: User = {
  id: 7,
  username: 'rover',
  email: 'rover@example.com',
  role: 'USER',
  playTimeMinutes: 60,
  onboardingCompleted: true,
}

/** The store state of someone who is logged in. */
export function signedIn(overrides: Partial<User> = {}): Partial<RootState> {
  return {
    auth: {
      token: 'test.jwt.token',
      user: { ...rover, ...overrides },
      sessionStatus: 'ready',
      sessionError: null,
    },
  }
}

/**
 * Renders a list of routes at a path, with a fresh store, and returns the store and the router.
 * `beforeRender` runs on the store before the first render, like main.tsx does with the saved session.
 */
export function renderRoutes(
  routes: RouteObject[],
  path: string,
  preloadedState?: Partial<RootState>,
  beforeRender?: (store: AppStore) => void,
) {
  const store = setupStore(preloadedState)
  beforeRender?.(store)
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  render(
    <Provider store={store}>
      <RouterProvider router={router} />
    </Provider>,
  )
  return { store, router }
}
