import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client'
import { gameRules } from '../../test/fixtures'
import { renderRoutes, rover, signedIn } from '../../test/utils'
import { getGameRules } from '../catalog/catalogApi'
import { deleteAccount, updateProfile } from './profileApi'
import ProfilePage from './ProfilePage'

vi.mock('../catalog/catalogApi', () => ({ getCharacters: vi.fn(), getGameRules: vi.fn(), getMaterials: vi.fn() }))
vi.mock('./profileApi', () => ({ updateProfile: vi.fn(), deleteAccount: vi.fn() }))

const TOKEN = 'test.jwt.token'
const routes = [
  { path: '/', element: <h1>Presentazione</h1> },
  { path: '/profilo', element: <ProfilePage /> },
]

function renderPage() {
  return renderRoutes(routes, '/profilo', signedIn())
}

function lastToast(store: ReturnType<typeof renderPage>['store']) {
  return store.getState().toast.items.at(-1)
}

async function accountForm() {
  return screen.findByRole('region', { name: 'Il tuo account' })
}

describe('ProfilePage', () => {
  beforeEach(() => {
    vi.mocked(getGameRules).mockResolvedValue(gameRules)
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  it('shows the username, the email that cannot change and the play time', async () => {
    renderPage()

    const form = await accountForm()
    expect(within(form).getByLabelText('Username')).toHaveValue('rover')
    expect(within(form).getByText('rover@example.com')).toBeInTheDocument()
    expect(within(form).getByText('L’email non si può cambiare.')).toBeInTheDocument()
    expect(await within(form).findByRole('radio', { name: '1 ora' })).toBeChecked()
    expect(document.title).toBe('Profilo · Resonance Planner')
  })

  it('sends only what changed and keeps the new values in the session', async () => {
    vi.mocked(updateProfile).mockResolvedValue({ ...rover, username: 'rover.new' })
    const { store } = renderPage()
    const form = await accountForm()

    fireEvent.change(within(form).getByLabelText('Username'), { target: { value: ' rover.new ' } })
    fireEvent.click(within(form).getByRole('button', { name: 'Salva' }))

    await waitFor(() => expect(lastToast(store)?.message).toBe('Profilo aggiornato.'))
    expect(updateProfile).toHaveBeenCalledWith(TOKEN, { username: 'rover.new' })
    expect(store.getState().auth.user?.username).toBe('rover.new')
    // The field shows what was saved, without the spaces
    expect(within(form).getByLabelText('Username')).toHaveValue('rover.new')
  })

  it('says when the choices of the play time could not be loaded, and tries again', async () => {
    vi.mocked(getGameRules).mockRejectedValueOnce(new ApiError('NETWORK_ERROR', 'Impossibile contattare il server.', 0))
    renderPage()
    const form = await accountForm()

    expect(await within(form).findByRole('alert')).toHaveTextContent('Non è stato possibile caricare le scelte del tempo di gioco.')
    fireEvent.click(within(form).getByRole('button', { name: 'Riprova' }))

    expect(await within(form).findByRole('radio', { name: '1 ora' })).toBeChecked()
  })

  it('changes the play time alone', async () => {
    vi.mocked(updateProfile).mockResolvedValue({ ...rover, playTimeMinutes: 120 })
    renderPage()
    const form = await accountForm()

    fireEvent.click(await within(form).findByRole('radio', { name: '2 ore' }))
    fireEvent.click(within(form).getByRole('button', { name: 'Salva' }))

    await waitFor(() => expect(updateProfile).toHaveBeenCalledWith(TOKEN, { playTimeMinutes: 120 }))
  })

  it('sends nothing when nothing changed', async () => {
    const { store } = renderPage()
    const form = await accountForm()

    fireEvent.click(within(form).getByRole('button', { name: 'Salva' }))

    expect(updateProfile).not.toHaveBeenCalled()
    expect(lastToast(store)?.message).toBe('Non ci sono modifiche da salvare.')
  })

  it('shows under the field that the username is taken', async () => {
    vi.mocked(updateProfile).mockRejectedValue(
      new ApiError('CONFLICT', 'Questo username è già usato.', 409, { username: 'Questo username è già usato.' }),
    )
    renderPage()
    const form = await accountForm()
    const input = within(form).getByLabelText('Username')

    fireEvent.change(input, { target: { value: 'other' } })
    fireEvent.click(within(form).getByRole('button', { name: 'Salva' }))

    expect(await within(form).findByText('Questo username è già usato.')).toBeInTheDocument()
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(input).toHaveFocus()
  })

  it('shows under the choice a play time the server refuses', async () => {
    vi.mocked(updateProfile).mockRejectedValue(
      new ApiError('VALIDATION_ERROR', 'Alcuni campi non sono validi.', 400, { playTimeMinutes: 'Scegli un tempo valido.' }),
    )
    renderPage()
    const form = await accountForm()

    fireEvent.click(await within(form).findByRole('radio', { name: '30 minuti' }))
    fireEvent.click(within(form).getByRole('button', { name: 'Salva' }))

    expect(await within(form).findByText('Scegli un tempo valido.')).toBeInTheDocument()
    expect(within(form).getByRole('radio', { name: '30 minuti' })).toHaveFocus()
  })

  it('shows a failure that is not about a field above the form', async () => {
    vi.mocked(updateProfile).mockRejectedValue(new ApiError('NETWORK_ERROR', 'Impossibile contattare il server.', 0))
    renderPage()
    const form = await accountForm()

    fireEvent.change(within(form).getByLabelText('Username'), { target: { value: 'rover.new' } })
    fireEvent.click(within(form).getByRole('button', { name: 'Salva' }))

    expect(await within(form).findByRole('alert')).toHaveTextContent('Impossibile contattare il server.')
  })

  it('logs out and goes back to the presentation (F11)', async () => {
    const { store } = renderPage()

    fireEvent.click(within(await accountForm()).getByRole('button', { name: 'Esci' }))

    expect(await screen.findByRole('heading', { name: 'Presentazione' })).toBeInTheDocument()
    expect(store.getState().auth.token).toBeNull()
    expect(lastToast(store)?.message).toBe('Sei uscito dall\'account.')
  })

  // The guest of "Prova la demo" (D134) has an address that cannot exist and a life of 24 hours
  describe('guest account', () => {
    const guestSession = () =>
      renderRoutes(routes, '/profilo', signedIn({ username: 'ospite-k3x9qz', email: 'abc@ospite.invalid', role: 'GUEST' }))

    it('says it is a guest account that goes away by itself, and never shows its address', async () => {
      guestSession()

      const form = await accountForm()
      expect(within(form).getByText('Account ospite')).toBeInTheDocument()
      expect(within(form).getByText(/si cancellano da soli entro 24 ore/)).toBeInTheDocument()
      expect(screen.queryByText('abc@ospite.invalid')).not.toBeInTheDocument()
      expect(within(form).getByLabelText('Username')).toHaveValue('ospite-k3x9qz')
    })

    it('asks the guest to write its name, which it knows, before it can delete', async () => {
      vi.mocked(deleteAccount).mockResolvedValue(undefined)
      guestSession()
      fireEvent.click(await screen.findByRole('button', { name: 'Elimina account' }))

      const dialog = screen.getByRole('dialog', { name: 'Eliminare l’account?' })
      const confirm = within(dialog).getByRole('button', { name: 'Elimina definitivamente' })
      expect(within(dialog).getByLabelText('Il tuo nome')).toHaveFocus()
      expect(within(dialog).queryByText('abc@ospite.invalid')).not.toBeInTheDocument()
      fireEvent.change(within(dialog).getByLabelText('Il tuo nome'), { target: { value: ' ospite-k3x9qz ' } })
      expect(confirm).not.toHaveAttribute('aria-disabled')
      fireEvent.click(confirm)

      expect(await screen.findByRole('heading', { name: 'Presentazione' })).toBeInTheDocument()
      expect(deleteAccount).toHaveBeenCalledWith(TOKEN)
    })
  })

  describe('deletion of the account (F12)', () => {
    it('asks to write the email before it can delete', async () => {
      renderPage()
      fireEvent.click(await screen.findByRole('button', { name: 'Elimina account' }))

      const dialog = screen.getByRole('dialog', { name: 'Eliminare l’account?' })
      const confirm = within(dialog).getByRole('button', { name: 'Elimina definitivamente' })
      expect(within(dialog).getByLabelText('La tua email')).toHaveFocus()
      expect(confirm).toHaveAttribute('aria-disabled', 'true')

      fireEvent.change(within(dialog).getByLabelText('La tua email'), { target: { value: 'other@example.com' } })
      fireEvent.click(confirm)
      expect(confirm).toHaveAttribute('aria-disabled', 'true')
      expect(deleteAccount).not.toHaveBeenCalled()
      fireEvent.change(within(dialog).getByLabelText('La tua email'), { target: { value: ' Rover@Example.com ' } })
      expect(confirm).not.toHaveAttribute('aria-disabled')
    })

    it('deletes the account, ends the session and goes back to the presentation', async () => {
      vi.mocked(deleteAccount).mockResolvedValue(undefined)
      const { store } = renderPage()
      fireEvent.click(await screen.findByRole('button', { name: 'Elimina account' }))
      const dialog = screen.getByRole('dialog')

      fireEvent.change(within(dialog).getByLabelText('La tua email'), { target: { value: 'rover@example.com' } })
      fireEvent.click(within(dialog).getByRole('button', { name: 'Elimina definitivamente' }))

      expect(await screen.findByRole('heading', { name: 'Presentazione' })).toBeInTheDocument()
      expect(deleteAccount).toHaveBeenCalledWith(TOKEN)
      expect(store.getState().auth.token).toBeNull()
      expect(lastToast(store)?.message).toBe('Account eliminato: i tuoi dati sono stati cancellati.')
    })

    it('stays open with the message when the server fails', async () => {
      vi.mocked(deleteAccount).mockRejectedValue(new ApiError('NETWORK_ERROR', 'Impossibile contattare il server.', 0))
      const { store } = renderPage()
      fireEvent.click(await screen.findByRole('button', { name: 'Elimina account' }))
      const dialog = screen.getByRole('dialog')

      fireEvent.change(within(dialog).getByLabelText('La tua email'), { target: { value: 'rover@example.com' } })
      fireEvent.click(within(dialog).getByRole('button', { name: 'Elimina definitivamente' }))

      expect(await within(dialog).findByRole('alert')).toHaveTextContent('Impossibile contattare il server.')
      expect(store.getState().auth.token).toBe(TOKEN)
    })

    it('closes with Annulla', async () => {
      renderPage()
      fireEvent.click(await screen.findByRole('button', { name: 'Elimina account' }))

      fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Annulla' }))

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })
  })
})
