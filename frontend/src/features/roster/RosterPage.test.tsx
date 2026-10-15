import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { Provider } from 'react-redux'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client'
import { setupStore } from '../../app/store'
import {
  aaltoEntry,
  aaltoRequirements,
  catalogCharacters,
  catalogMaterials,
  gameRules,
  jinhsiEntry,
  jinhsiPlan,
  jinhsiRequirements,
} from '../../test/fixtures'
import { rover, signedIn } from '../../test/utils'
import { restoreSession } from '../auth/authSlice'
import { getCharacters, getGameRules, getMaterials } from '../catalog/catalogApi'
import {
  addToRoster,
  getFarmingPlan,
  getInventory,
  getRequirements,
  getRoster,
  putInventoryQuantity,
  removeFromRoster,
  updateInRoster,
} from './rosterApi'
import RosterPage from './RosterPage'

vi.mock('../catalog/catalogApi', () => ({ getCharacters: vi.fn(), getGameRules: vi.fn(), getMaterials: vi.fn() }))
vi.mock('./rosterApi', () => ({
  getRoster: vi.fn(),
  getInventory: vi.fn(),
  getFarmingPlan: vi.fn(),
  getRequirements: vi.fn(),
  addToRoster: vi.fn(),
  updateInRoster: vi.fn(),
  removeFromRoster: vi.fn(),
  putInventoryQuantity: vi.fn(),
}))

const TOKEN = 'test.jwt.token'

function renderPage() {
  const store = setupStore(signedIn())
  render(
    <Provider store={store}>
      <MemoryRouter>
        <RosterPage />
      </MemoryRouter>
    </Provider>,
  )
  return store
}

function lastToast(store: ReturnType<typeof setupStore>) {
  return store.getState().toast.items.at(-1)
}

async function cardOf(name: string) {
  return screen.findByRole('article', { name })
}

describe('RosterPage', () => {
  beforeEach(() => {
    vi.mocked(getCharacters).mockResolvedValue(catalogCharacters)
    vi.mocked(getGameRules).mockResolvedValue(gameRules)
    vi.mocked(getMaterials).mockResolvedValue(catalogMaterials)
    vi.mocked(getRoster).mockResolvedValue([jinhsiEntry, aaltoEntry])
    vi.mocked(getInventory).mockResolvedValue([{ materialId: 'lf-howler-core', quantity: 4 }])
    vi.mocked(getFarmingPlan).mockResolvedValue(jinhsiPlan)
    vi.mocked(getRequirements).mockImplementation(async (_, id) =>
      id === 'jinhsi' ? jinhsiRequirements : aaltoRequirements,
    )
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  describe('characters', () => {
    it('has a title, shows that it is loading, then every character with its goal and what is missing', async () => {
      // Aalto without an icon: the card shows its initials (the criterion of T24)
      vi.mocked(getCharacters).mockResolvedValue(
        catalogCharacters.map((character) => (character.id === 'aalto' ? { ...character, icon: null } : character)),
      )
      renderPage()

      expect(document.title).toContain('Roster e inventario')
      expect(screen.getByRole('status')).toHaveTextContent('Caricamento')

      const jinhsi = await cardOf('Jinhsi')
      expect(within(jinhsi).getByText('Livello 1 → 40, con l’ascensione al 40')).toBeInTheDocument()
      expect(within(jinhsi).getByLabelText('Materiali')).toHaveAttribute('value', '25')
      expect(within(jinhsi).getByText('25%')).toBeInTheDocument()
      // The LF cores are all owned: only what is still missing is listed, with the EXP in points
      expect(within(jinhsi).getByText(/3 Elegy Tacet Core, 208\.800 EXP, 88\.080 Shell Credit/)).toBeInTheDocument()
      expect(within(jinhsi).getByText('stima')).toBeInTheDocument()

      const aalto = await cardOf('Aalto')
      expect(within(aalto).getByText('Obiettivo raggiunto: non serve altro.')).toBeInTheDocument()
      expect(within(aalto).getByText('AA')).toBeInTheDocument()
    })

    it('shows the estimate of the whole roster with its runs', async () => {
      renderPage()

      const summary = await screen.findByRole('region', { name: 'Totale stimato per tutto il roster' })
      expect(within(summary).getByText('~280 Waveplate · ~50 minuti · ~2 giorni')).toBeInTheDocument()
      expect(within(summary).getByText(/Boss Challenge: Mourning Aix: 2 run · 120 Waveplate · 8 minuti/)).toBeInTheDocument()
    })

    it('says when the roster is empty', async () => {
      vi.mocked(getRoster).mockResolvedValue([])
      renderPage()

      expect(await screen.findByText('Nessun personaggio: aggiungine uno per iniziare.')).toBeInTheDocument()
      expect(screen.queryByRole('region', { name: /Totale stimato/ })).not.toBeInTheDocument()
    })

    it('loads again when the session is restored while the page is open', async () => {
      const store = renderPage()
      await cardOf('Jinhsi')

      // A login in another tab restores the session: the data of the user are forgotten, then loaded again
      store.dispatch(restoreSession.fulfilled({ token: TOKEN, user: rover }, 'other-tab'))

      await waitFor(() => expect(getRoster).toHaveBeenCalledTimes(2))
      expect(await cardOf('Jinhsi')).toBeInTheDocument()
    })

    it('shows the error of the server and loads again with Riprova', async () => {
      vi.mocked(getRoster).mockRejectedValueOnce(new ApiError('SERVICE_UNAVAILABLE', 'Il server non è ancora pronto.', 503))
      renderPage()

      expect(await screen.findByRole('alert')).toHaveTextContent('Il server non è ancora pronto.')
      fireEvent.click(screen.getByRole('button', { name: 'Riprova' }))

      expect(await cardOf('Jinhsi')).toBeInTheDocument()
    })

    it('adds a character with the usual goal, then loads the page again', async () => {
      vi.mocked(addToRoster).mockResolvedValue({ ...jinhsiEntry, characterId: 'yinlin' })
      const store = renderPage()
      await cardOf('Jinhsi')

      fireEvent.click(screen.getByRole('button', { name: 'Aggiungi un personaggio' }))
      // Only the characters that are not in the roster can be chosen
      const select = screen.getByLabelText('Personaggio')
      expect(within(select).queryByRole('option', { name: 'Jinhsi' })).not.toBeInTheDocument()
      fireEvent.change(select, { target: { value: 'yinlin' } })
      fireEvent.click(screen.getByRole('button', { name: 'Aggiungi' }))

      await waitFor(() =>
        expect(addToRoster).toHaveBeenCalledWith(TOKEN, {
          characterId: 'yinlin',
          currentLevel: 1,
          currentRank: 0,
          targetLevel: 40,
          targetRank: 2,
        }),
      )
      await waitFor(() => expect(getRoster).toHaveBeenCalledTimes(2))
      expect(lastToast(store)?.message).toBe('Yinlin è entrato nel roster.')
      expect(screen.queryByLabelText('Personaggio')).not.toBeInTheDocument()
    })

    it('says that every character is in the roster after the last one is added, and gives the focus to the title', async () => {
      vi.mocked(addToRoster).mockResolvedValue({ ...jinhsiEntry, characterId: 'yinlin' })
      vi.mocked(getRoster)
        .mockResolvedValueOnce([jinhsiEntry, aaltoEntry])
        .mockResolvedValue([jinhsiEntry, aaltoEntry, { ...jinhsiEntry, characterId: 'yinlin' }])
      renderPage()
      await cardOf('Jinhsi')

      fireEvent.click(screen.getByRole('button', { name: 'Aggiungi un personaggio' }))
      fireEvent.change(screen.getByLabelText('Personaggio'), { target: { value: 'yinlin' } })
      fireEvent.click(screen.getByRole('button', { name: 'Aggiungi' }))

      expect(await screen.findByText('Tutti i personaggi del catalogo sono nel tuo roster.')).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: 'Aggiungi un personaggio' })).not.toBeInTheDocument()
      await waitFor(() => expect(screen.getByRole('heading', { name: 'I tuoi personaggi' })).toHaveFocus())
    })

    it('asks to choose a character and a goal that is not lower, before sending', async () => {
      renderPage()
      await cardOf('Jinhsi')
      fireEvent.click(screen.getByRole('button', { name: 'Aggiungi un personaggio' }))

      fireEvent.change(screen.getByLabelText('Obiettivo'), { target: { value: '1' } })
      fireEvent.change(screen.getByLabelText('Livello attuale'), { target: { value: '10' } })
      fireEvent.click(screen.getByRole('button', { name: 'Aggiungi' }))

      expect(await screen.findByText('Scegli un personaggio.')).toBeInTheDocument()
      expect(screen.getByText("L'obiettivo non può essere più basso della situazione attuale.")).toBeInTheDocument()
      expect(screen.getByLabelText('Personaggio')).toHaveFocus()
      expect(addToRoster).not.toHaveBeenCalled()
    })

    it('shows the message of the server under the field it is about, or above the form', async () => {
      vi.mocked(addToRoster)
        .mockRejectedValueOnce(
          new ApiError('CONFLICT', 'Questo personaggio è già nel tuo roster.', 409, {
            characterId: 'Questo personaggio è già nel tuo roster.',
          }),
        )
        .mockRejectedValueOnce(new ApiError('NETWORK_ERROR', 'Impossibile contattare il server.', 0))
      renderPage()
      await cardOf('Jinhsi')
      fireEvent.click(screen.getByRole('button', { name: 'Aggiungi un personaggio' }))
      fireEvent.change(screen.getByLabelText('Personaggio'), { target: { value: 'yinlin' } })

      fireEvent.click(screen.getByRole('button', { name: 'Aggiungi' }))
      expect(await screen.findByText('Questo personaggio è già nel tuo roster.')).toBeInTheDocument()
      expect(screen.getByLabelText('Personaggio')).toHaveAttribute('aria-invalid', 'true')

      fireEvent.click(screen.getByRole('button', { name: 'Aggiungi' }))
      expect(await screen.findByRole('alert')).toHaveTextContent('Impossibile contattare il server.')
    })

    it('changes the levels of a character', async () => {
      vi.mocked(updateInRoster).mockResolvedValue({ ...jinhsiEntry, currentLevel: 20, currentRank: 1 })
      const store = renderPage()
      const jinhsi = await cardOf('Jinhsi')

      fireEvent.click(within(jinhsi).getByRole('button', { name: 'Modifica Jinhsi' }))
      fireEvent.change(within(jinhsi).getByLabelText('Livello attuale'), { target: { value: '20' } })
      fireEvent.click(within(jinhsi).getByLabelText("Ho già fatto l'ascensione al 20"))
      fireEvent.click(within(jinhsi).getByRole('button', { name: 'Salva' }))

      await waitFor(() =>
        expect(updateInRoster).toHaveBeenCalledWith(TOKEN, 'jinhsi', {
          currentLevel: 20,
          currentRank: 1,
          targetLevel: 40,
          targetRank: 2,
        }),
      )
      expect(lastToast(store)?.message).toBe('Jinhsi: livelli aggiornati.')
      await waitFor(() => expect(within(jinhsi).getByRole('button', { name: 'Modifica Jinhsi' })).toHaveFocus())
    })

    it('starts the change from the saved levels, with the ascension already done', async () => {
      renderPage()
      const aalto = await cardOf('Aalto')

      fireEvent.click(within(aalto).getByRole('button', { name: 'Modifica Aalto' }))

      expect(within(aalto).getByLabelText('Livello attuale')).toHaveValue('40')
      expect(within(aalto).getByLabelText("Ho già fatto l'ascensione al 40")).not.toBeChecked()
      expect(within(aalto).getByLabelText("Voglio fare l'ascensione al 40")).toBeChecked()
      fireEvent.click(within(aalto).getByRole('button', { name: 'Annulla' }))
      expect(within(aalto).queryByLabelText('Livello attuale')).not.toBeInTheDocument()
    })

    it('shows a level refused by the server under its picker', async () => {
      vi.mocked(updateInRoster).mockRejectedValue(
        new ApiError('VALIDATION_ERROR', 'Alcuni campi non sono validi.', 400, {
          currentLevel: 'Con il grado 1 il livello va da 20 a 40.',
        }),
      )
      renderPage()
      const jinhsi = await cardOf('Jinhsi')
      fireEvent.click(within(jinhsi).getByRole('button', { name: 'Modifica Jinhsi' }))

      fireEvent.click(within(jinhsi).getByRole('button', { name: 'Salva' }))

      expect(await within(jinhsi).findByText('Con il grado 1 il livello va da 20 a 40.')).toBeInTheDocument()
      expect(within(jinhsi).getByLabelText('Livello attuale')).toHaveFocus()
    })

    it('removes a character only after the confirmation', async () => {
      vi.mocked(removeFromRoster).mockResolvedValue(undefined)
      const store = renderPage()
      const aalto = await cardOf('Aalto')

      fireEvent.click(within(aalto).getByRole('button', { name: 'Rimuovi Aalto' }))
      const dialog = screen.getByRole('dialog', { name: 'Rimuovere Aalto dal roster?' })
      fireEvent.click(within(dialog).getByRole('button', { name: 'Annulla' }))
      expect(removeFromRoster).not.toHaveBeenCalled()

      fireEvent.click(within(aalto).getByRole('button', { name: 'Rimuovi Aalto' }))
      fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Rimuovi' }))

      await waitFor(() => expect(removeFromRoster).toHaveBeenCalledWith(TOKEN, 'aalto'))
      expect(lastToast(store)?.message).toBe('Aalto è uscito dal roster.')
      expect(screen.getByRole('heading', { name: 'I tuoi personaggi' })).toHaveFocus()
    })

    it('tells when a removal fails', async () => {
      vi.mocked(removeFromRoster).mockRejectedValue(new ApiError('NOT_FOUND', 'Questo personaggio non è nel tuo roster.', 404))
      const store = renderPage()
      const aalto = await cardOf('Aalto')

      fireEvent.click(within(aalto).getByRole('button', { name: 'Rimuovi Aalto' }))
      fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Rimuovi' }))

      await waitFor(() => expect(lastToast(store)).toMatchObject({ variant: 'danger' }))
      expect(lastToast(store)?.message).toBe('Questo personaggio non è nel tuo roster.')
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })
  })

  describe('tabs', () => {
    it('moves between the tabs with a click and with the arrows', async () => {
      renderPage()
      await cardOf('Jinhsi')
      const characters = screen.getByRole('tab', { name: 'Personaggi' })
      const inventory = screen.getByRole('tab', { name: 'Inventario' })
      expect(characters).toHaveAttribute('aria-selected', 'true')

      fireEvent.click(inventory)
      expect(inventory).toHaveAttribute('aria-selected', 'true')
      expect(await screen.findByRole('heading', { name: 'I tuoi materiali' })).toBeInTheDocument()

      fireEvent.keyDown(inventory, { key: 'ArrowRight' })
      expect(characters).toHaveAttribute('aria-selected', 'true')
      expect(characters).toHaveFocus()
      expect(characters).toHaveAttribute('tabindex', '0')
      expect(inventory).toHaveAttribute('tabindex', '-1')

      fireEvent.keyDown(characters, { key: 'End' })
      expect(inventory).toHaveFocus()
      fireEvent.keyDown(inventory, { key: 'Home' })
      expect(characters).toHaveFocus()
      expect(screen.getByRole('tablist', { name: 'Sezioni del roster' })).toBeInTheDocument()
    })
  })

  describe('inventory', () => {
    async function openInventory() {
      const store = renderPage()
      await cardOf('Jinhsi')
      fireEvent.click(screen.getByRole('tab', { name: 'Inventario' }))
      await screen.findByRole('heading', { name: 'I tuoi materiali' })
      return store
    }

    it('lists every material by group, with what is owned and 0 for the others', async () => {
      await openInventory()

      expect(screen.getByRole('table', { name: 'Drop dei nemici comuni' })).toBeInTheDocument()
      expect(screen.getByLabelText('LF Howler Core')).toHaveValue('4')
      expect(screen.getByLabelText('Elegy Tacet Core')).toHaveValue('0')
      expect(screen.getByText("20.000 EXP l'una")).toBeInTheDocument()
    })

    it('saves a new quantity when the field is left, then says it is saved', async () => {
      vi.mocked(putInventoryQuantity).mockResolvedValue({ materialId: 'elegy-tacet-core', quantity: 3 })
      await openInventory()
      const field = screen.getByLabelText('Elegy Tacet Core')

      fireEvent.change(field, { target: { value: '3' } })
      fireEvent.blur(field)

      await waitFor(() => expect(putInventoryQuantity).toHaveBeenCalledWith(TOKEN, 'elegy-tacet-core', 3))
      expect(await screen.findByText('Salvato')).toBeInTheDocument()
      expect(screen.getByText('Elegy Tacet Core: 3 salvato.')).toBeInTheDocument()
      await waitFor(() => expect(getRoster).toHaveBeenCalledTimes(2))
    })

    it('keeps the field read-only while it is saved, so two saves never overlap', async () => {
      let answer: (item: { materialId: string; quantity: number }) => void = () => {}
      vi.mocked(putInventoryQuantity).mockReturnValue(new Promise((resolve) => (answer = resolve)))
      await openInventory()
      const field = screen.getByLabelText('Elegy Tacet Core')

      fireEvent.change(field, { target: { value: ' 007 ' } })
      fireEvent.blur(field)

      expect(await screen.findByText('Salvataggio…')).toBeInTheDocument()
      expect(field).toHaveAttribute('readonly')
      expect(field).toHaveValue('7')
      fireEvent.blur(field)
      expect(putInventoryQuantity).toHaveBeenCalledOnce()
      answer({ materialId: 'elegy-tacet-core', quantity: 7 })
      expect(await screen.findByText('Salvato')).toBeInTheDocument()
      expect(field).not.toHaveAttribute('readonly')
    })

    it('keeps the saved quantity when the tab changes and comes back', async () => {
      vi.mocked(putInventoryQuantity).mockResolvedValue({ materialId: 'elegy-tacet-core', quantity: 3 })
      // The reload after the save fails: the store keeps the old inventory
      vi.mocked(getInventory)
        .mockResolvedValueOnce([{ materialId: 'lf-howler-core', quantity: 4 }])
        .mockRejectedValue(new ApiError('NETWORK_ERROR', 'Impossibile contattare il server.', 0))
      await openInventory()
      const field = screen.getByLabelText('Elegy Tacet Core')
      fireEvent.change(field, { target: { value: '3' } })
      fireEvent.blur(field)
      expect(await screen.findByText('Salvato')).toBeInTheDocument()

      fireEvent.click(screen.getByRole('tab', { name: 'Personaggi' }))
      fireEvent.click(screen.getByRole('tab', { name: 'Inventario' }))

      expect(screen.getByLabelText('Elegy Tacet Core')).toHaveValue('3')
    })

    it('sends nothing for a value that did not change or is not a whole number', async () => {
      await openInventory()
      const field = screen.getByLabelText('LF Howler Core')

      fireEvent.blur(field)
      fireEvent.change(field, { target: { value: '4,5' } })
      fireEvent.blur(field)

      // Announced at once, even though the focus has already left the field
      expect(await screen.findByRole('alert')).toHaveTextContent('Inserisci un numero intero da 0 a 999.999.999.')
      expect(field).toHaveAttribute('aria-invalid', 'true')
      fireEvent.change(field, { target: { value: '1000000000' } })
      fireEvent.blur(field)
      expect(putInventoryQuantity).not.toHaveBeenCalled()
    })

    it('shows the message of the server under the field', async () => {
      vi.mocked(putInventoryQuantity).mockRejectedValue(
        new ApiError('VALIDATION_ERROR', 'Alcuni campi non sono validi.', 400, { quantity: 'Quantità non valida.' }),
      )
      await openInventory()
      const field = screen.getByLabelText('Shell Credit')

      fireEvent.change(field, { target: { value: '500' } })
      fireEvent.blur(field)

      expect(await screen.findByText('Quantità non valida.')).toBeInTheDocument()
      expect(screen.queryByText('Salvato')).not.toBeInTheDocument()
    })

    it('shows the error of the catalog of materials and tries again', async () => {
      vi.mocked(getMaterials).mockRejectedValueOnce(new ApiError('NETWORK_ERROR', 'Impossibile contattare il server.', 0))
      renderPage()
      await cardOf('Jinhsi')
      fireEvent.click(screen.getByRole('tab', { name: 'Inventario' }))

      expect(await screen.findByText('Impossibile contattare il server.')).toBeInTheDocument()
      fireEvent.click(screen.getByRole('button', { name: 'Riprova' }))
      expect(await screen.findByRole('heading', { name: 'I tuoi materiali' })).toBeInTheDocument()
    })
  })
})
