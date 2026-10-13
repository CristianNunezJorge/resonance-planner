import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { Provider } from 'react-redux'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client'
import { setupStore } from '../../app/store'
import { rover } from '../../test/utils'
import { register as registerRequest } from './authApi'
import RegisterPage from './RegisterPage'

vi.mock('./authApi', () => ({ login: vi.fn(), register: vi.fn(), getProfile: vi.fn() }))

function renderPage() {
  const store = setupStore()
  render(
    <Provider store={store}>
      <MemoryRouter>
        <RegisterPage />
      </MemoryRouter>
    </Provider>,
  )
  return store
}

function fillAndSubmit(username: string, email: string, password: string) {
  fireEvent.change(screen.getByLabelText('Username'), { target: { value: username } })
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: email } })
  fireEvent.change(screen.getByLabelText('Password'), { target: { value: password } })
  fireEvent.click(screen.getByRole('button', { name: 'Registrati' }))
}

describe('RegisterPage', () => {
  afterEach(() => {
    vi.mocked(registerRequest).mockReset()
    localStorage.clear()
  })

  it('has a title, the three fields with their help and a link to the login', () => {
    renderPage()

    expect(document.title).toBe('Registrati · Resonance Planner')
    expect(screen.getByRole('heading', { name: 'Registrati' })).toBeInTheDocument()
    expect(screen.getByLabelText('Username')).toHaveAccessibleDescription(
      'Da 3 a 30 caratteri: lettere, numeri, punto, trattino e underscore.',
    )
    expect(screen.getByLabelText('Password')).toHaveAccessibleDescription('Almeno 8 caratteri.')
    expect(screen.getByLabelText('Password')).toHaveAttribute('autocomplete', 'new-password')
    expect(screen.getByRole('link', { name: 'Accedi' })).toHaveAttribute('href', '/login')
  })

  it('marks every field as required and sends the form with the post method', () => {
    renderPage()

    for (const label of ['Username', 'Email', 'Password']) {
      expect(screen.getByLabelText(label)).toBeRequired()
    }
    expect(document.querySelector('form')).toHaveAttribute('method', 'post')
  })

  it('sends the three values and keeps the new user', async () => {
    vi.mocked(registerRequest).mockResolvedValue({ token: 'fresh.jwt.token', user: rover })
    const store = renderPage()

    fillAndSubmit('rover', 'rover@example.com', 'S3cret-pass')

    await waitFor(() => expect(store.getState().auth.user).toEqual(rover))
    expect(registerRequest).toHaveBeenCalledWith({
      username: 'rover',
      email: 'rover@example.com',
      password: 'S3cret-pass',
    })
  })

  it('shows the message for each wrong field under it, with the hint kept, and focuses the first one', async () => {
    vi.mocked(registerRequest).mockRejectedValue(
      new ApiError('VALIDATION_ERROR', 'Alcuni campi non sono validi.', 400, {
        username: 'Usa da 3 a 30 caratteri tra lettere, numeri, punto, trattino e underscore.',
        password: 'Deve avere almeno 8 caratteri.',
      }),
    )
    renderPage()

    fillAndSubmit('ab', 'rover@example.com', 'short')

    expect(await screen.findByText('Deve avere almeno 8 caratteri.')).toBeInTheDocument()
    expect(screen.getByLabelText('Username')).toBeInvalid()
    expect(screen.getByLabelText('Email')).toBeValid()
    expect(screen.getByLabelText('Password')).toHaveAccessibleDescription('Almeno 8 caratteri. Deve avere almeno 8 caratteri.')
    await waitFor(() => expect(screen.getByLabelText('Username')).toHaveFocus())
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('tells that the email is already registered, under the email, with the focus on it', async () => {
    vi.mocked(registerRequest).mockRejectedValue(
      new ApiError('CONFLICT', 'Questa email è già registrata.', 409, { email: 'Questa email è già registrata.' }),
    )
    renderPage()

    fillAndSubmit('rover', 'rover@example.com', 'S3cret-pass')

    expect(await screen.findByText('Questa email è già registrata.')).toHaveClass('invalid-feedback')
    expect(screen.getByLabelText('Email')).toBeInvalid()
    await waitFor(() => expect(screen.getByLabelText('Email')).toHaveFocus())
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('shows in the alert an error that is not about a field', async () => {
    vi.mocked(registerRequest).mockRejectedValue(
      new ApiError('NETWORK_ERROR', 'Impossibile contattare il server. Controlla la connessione e riprova.', 0),
    )
    renderPage()

    fillAndSubmit('rover', 'rover@example.com', 'S3cret-pass')

    expect(await screen.findByRole('alert')).toHaveTextContent('Impossibile contattare il server.')
  })

  it('disables the button while the request is running', async () => {
    vi.mocked(registerRequest).mockReturnValue(new Promise(() => undefined))
    renderPage()

    fillAndSubmit('rover', 'rover@example.com', 'S3cret-pass')

    expect(await screen.findByRole('button', { name: 'Registrazione in corso…' })).toBeDisabled()
  })

  it('tells how long to wait when there were too many attempts', async () => {
    vi.mocked(registerRequest).mockRejectedValue(
      new ApiError('RATE_LIMITED', 'Troppe richieste: riprova tra 9 minuti.', 429),
    )
    renderPage()

    fillAndSubmit('rover', 'rover@example.com', 'S3cret-pass')

    expect(await screen.findByRole('alert')).toHaveTextContent('Troppe richieste: riprova tra 9 minuti.')
  })
})
