import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { Provider } from 'react-redux'
import { RouterProvider } from 'react-router/dom'
// Global styles first, so the component styles imported later can override Bootstrap
import './styles/main.scss'
import { setUnauthorizedHandler } from './api/client'
import { router } from './app/router'
import { store } from './app/store'
import { expireSession, restoreSession } from './features/auth/authSlice'
import { syncSessionAcrossTabs } from './features/auth/crossTabSync'

// A request with a token that the server refuses (expired, account deleted) ends the session (D45, F2)
setUnauthorizedHandler((refusedToken) => store.dispatch(expireSession(refusedToken)))
// A login or logout in another tab of the same browser applies here too
syncSessionAcrossTabs(store.dispatch)
// If a token was saved in the browser, find out who it belongs to before the pages decide what to show
void store.dispatch(restoreSession())

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Provider store={store}>
      <RouterProvider router={router} />
    </Provider>
  </StrictMode>,
)
