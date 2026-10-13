import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { Provider } from 'react-redux'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client'
import { setupStore } from '../../app/store'
import { rover } from '../../test/utils'
import { login as loginRequest } from './authApi'
import LoginPage from './LoginPage'

vi.mock('./authApi', () => ({ login: vi.fn(), register: vi.fn(), getProfile: vi.fn() }))

function renderPage() {
  const store = setupStore()
  render(
    <Provider store={store}>
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    </Provider>,
  )
  return store
}

function fillAndSubmit(email: string, password: string) {
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: email } })
  fireEvent.change(screen.getByLabelText('Password'), { target: { value: password } })
  fireEvent.click(screen.getByRole('button', { name: 'Accedi' }))
}

describe('LoginPage', () => {
  afterEach(() => {
    vi.mocked(loginRequest).mockReset()
    localStorage.clear()
  })

  it('has a title, the two fields and a link to the registration', () => {
    renderPage()

    expect(document.title).toBe('Accedi · Resonance Planner')
    expect(screen.getByRole('heading', { name: 'Accedi' })).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toHaveAttribute('type', 'email')
    expect(screen.getByLabelText('Password')).toHaveAttribute('type', 'password')
    expect(screen.getByRole('link', { name: 'Registrati' })).toHaveAttribute('href', '/registrati')
  })

  it('helps the password manager and the screen reader: right autocomplete, required fields, post method', () => {
    renderPage()

    expect(screen.getByLabelText('Email')).toHaveAttribute('autocomplete', 'username')
    expect(screen.getByLabelText('Password')).toHaveAttribute('autocomplete', 'current-password')
    expect(screen.getByLabelText('Email')).toBeRequired()
    expect(screen.getByLabelText('Password')).toBeRequired()
    expect(document.querySelector('form')).toHaveAttribute('method', 'post')
  })

  it('sends email and password and keeps the user of the answer', async () => {
    vi.mocked(loginRequest).mockResolvedValue({ token: 'fresh.jwt.token', user: rover })
    const store = renderPage()

    fillAndSubmit('rover@example.com', 'S3cret-pass')

    await waitFor(() => expect(store.getState().auth.user).toEqual(rover))
    expect(loginRequest).toHaveBeenCalledWith({ email: 'rover@example.com', password: 'S3cret-pass' })
  })

  it('disables the button while the request is running, so it cannot be sent twice', async () => {
    let finish: (value: { token: string; user: typeof rover }) => void = () => undefined
    vi.mocked(loginRequest).mockReturnValue(new Promise((resolve) => (finish = resolve)))
    renderPage()

    fillAndSubmit('rover@example.com', 'S3cret-pass')

    const button = await screen.findByRole('button', { name: 'Accesso in corso…' })
    expect(button).toBeDisabled()
    finish({ token: 't', user: rover })
  })

  it('shows one message for a wrong email or password, and moves the focus to it', async () => {
    vi.mocked(loginRequest).mockRejectedValue(
      new ApiError('BAD_CREDENTIALS', 'Email o password non corrette.', 401),
    )
    renderPage()

    fillAndSubmit('rover@example.com', 'wrong-password')

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Email o password non corrette.')
    // The focus moves in an effect, after the text is on screen
    await waitFor(() => expect(alert).toHaveFocus())
    expect(screen.getByRole('button', { name: 'Accedi' })).toBeEnabled()
    expect(screen.getByLabelText('Email')).toBeValid()
  })

  it('tells the user how long to wait when there were too many attempts', async () => {
    vi.mocked(loginRequest).mockRejectedValue(
      new ApiError('RATE_LIMITED', 'Troppe richieste: riprova tra 12 minuti.', 429),
    )
    renderPage()

    fillAndSubmit('rover@example.com', 'wrong-password')

    expect(await screen.findByRole('alert')).toHaveTextContent('Troppe richieste: riprova tra 12 minuti.')
  })

  it('shows each message of the server under its field and moves the focus to the first wrong one', async () => {
    vi.mocked(loginRequest).mockRejectedValue(
      new ApiError('VALIDATION_ERROR', 'Alcuni campi non sono validi.', 400, {
        email: 'Inserisci l\'email.',
        password: 'Inserisci la password.',
      }),
    )
    renderPage()

    fireEvent.click(screen.getByRole('button', { name: 'Accedi' }))

    expect(await screen.findByText('Inserisci l\'email.')).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toBeInvalid()
    expect(screen.getByLabelText('Email')).toHaveAccessibleDescription('Inserisci l\'email.')
    expect(screen.getByLabelText('Password')).toHaveAccessibleDescription('Inserisci la password.')
    await waitFor(() => expect(screen.getByLabelText('Email')).toHaveFocus())
    // The errors are under the fields: no second alert with the same news
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('reports a server that cannot be reached', async () => {
    vi.mocked(loginRequest).mockRejectedValue(
      new ApiError('NETWORK_ERROR', 'Impossibile contattare il server. Controlla la connessione e riprova.', 0),
    )
    renderPage()

    fillAndSubmit('rover@example.com', 'S3cret-pass')

    expect(await screen.findByRole('alert')).toHaveTextContent('Impossibile contattare il server.')
  })

  it('clears the old error when the form is sent again', async () => {
    vi.mocked(loginRequest)
      .mockRejectedValueOnce(new ApiError('BAD_CREDENTIALS', 'Email o password non corrette.', 401))
      .mockReturnValueOnce(new Promise(() => undefined))
    renderPage()

    fillAndSubmit('rover@example.com', 'wrong-password')
    await screen.findByRole('alert')
    fireEvent.click(screen.getByRole('button', { name: 'Accedi' }))

    await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument())
  })
})
