import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { Provider } from 'react-redux'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client'
import { setupStore } from '../../app/store'
import { catalogCharacters, gameRules } from '../../test/fixtures'
import { rover, signedIn } from '../../test/utils'
import { getProfile } from '../auth/authApi'
import { getCharacters, getGameRules } from '../catalog/catalogApi'
import { postOnboarding } from './onboardingApi'
import OnboardingPage from './OnboardingPage'

vi.mock('../catalog/catalogApi', () => ({ getCharacters: vi.fn(), getGameRules: vi.fn() }))
vi.mock('./onboardingApi', () => ({ postOnboarding: vi.fn() }))
vi.mock('../auth/authApi', () => ({ login: vi.fn(), register: vi.fn(), getProfile: vi.fn() }))

const setupDone = { ...rover, onboardingCompleted: true, playTimeMinutes: 60 }
const LOWER_GOAL = "L'obiettivo non può essere più basso della situazione attuale."

function renderPage() {
  const store = setupStore(signedIn({ onboardingCompleted: false }))
  render(
    <Provider store={store}>
      <MemoryRouter>
        <OnboardingPage />
      </MemoryRouter>
    </Provider>,
  )
  return store
}

// The catalog of the tests is Aalto, Jinhsi, Yinlin, in this order
async function startAtStep1() {
  const store = renderPage()
  await screen.findByRole('heading', { name: /Passo 1 di 3/ })
  return store
}

function choose(name: string) {
  fireEvent.click(screen.getByRole('checkbox', { name: new RegExp(name) }))
}

function next() {
  fireEvent.click(screen.getByRole('button', { name: 'Avanti' }))
}

function character(name: string) {
  return screen.getByRole('group', { name: new RegExp(name) })
}

function pick(select: HTMLElement, value: string) {
  fireEvent.change(select, { target: { value } })
}

async function goToStep2(...names: string[]) {
  await startAtStep1()
  for (const name of names) {
    choose(name)
  }
  next()
  await screen.findByRole('heading', { name: /Passo 2 di 3/ })
}

async function goToStep3() {
  await goToStep2('Aalto', 'Jinhsi')
  next()
  await screen.findByRole('heading', { name: /Passo 3 di 3/ })
}

function fillStep3(waveplate: string, playTime: string) {
  fireEvent.change(screen.getByLabelText('Waveplate attuali'), { target: { value: waveplate } })
  fireEvent.click(screen.getByRole('radio', { name: playTime }))
}

describe('OnboardingPage', () => {
  beforeEach(() => {
    vi.mocked(getCharacters).mockResolvedValue(catalogCharacters)
    vi.mocked(getGameRules).mockResolvedValue(gameRules)
  })

  afterEach(() => {
    vi.mocked(getCharacters).mockReset()
    vi.mocked(getGameRules).mockReset()
    vi.mocked(postOnboarding).mockReset()
    vi.mocked(getProfile).mockReset()
  })

  describe('loading', () => {
    it('has a title, shows that it is loading, then step 1 with the characters to choose', async () => {
      renderPage()

      expect(document.title).toBe('Configurazione guidata · Resonance Planner')
      expect(screen.getByRole('heading', { level: 1, name: 'Configurazione guidata' })).toBeInTheDocument()
      expect(screen.getByRole('status')).toHaveTextContent('Caricamento…')

      expect(await screen.findByRole('heading', { name: /Passo 1 di 3/ })).toBeInTheDocument()
      for (const name of ['Aalto', 'Jinhsi', 'Yinlin']) {
        expect(screen.getByRole('checkbox', { name: new RegExp(name) })).not.toBeChecked()
      }
      expect(screen.queryByRole('button', { name: 'Indietro' })).not.toBeInTheDocument()
    })

    it('shows the progress of the three steps to everyone, not only with the colour', async () => {
      await startAtStep1()

      const progress = screen.getByRole('progressbar', { name: /Avanzamento/ })
      expect(progress).toHaveAttribute('value', '1')
      expect(progress).toHaveAttribute('max', '3')
      expect(screen.getByRole('heading', { name: 'Passo 1 di 3 · Scegli i personaggi' })).toBeInTheDocument()
    })

    it('shows the error with a "Riprova" button when the characters cannot be loaded, and goes on after it', async () => {
      vi.mocked(getCharacters)
        .mockRejectedValueOnce(new ApiError('NETWORK_ERROR', 'Impossibile contattare il server.', 0))
        .mockResolvedValueOnce(catalogCharacters)
      renderPage()

      expect(await screen.findByRole('alert')).toHaveTextContent('Impossibile contattare il server.')
      fireEvent.click(screen.getByRole('button', { name: 'Riprova' }))

      expect(await screen.findByRole('heading', { name: /Passo 1 di 3/ })).toBeInTheDocument()
    })

    it('shows the error when the rules of the game cannot be loaded', async () => {
      vi.mocked(getGameRules).mockRejectedValue(new ApiError('NETWORK_ERROR', 'Le regole non arrivano.', 0))
      renderPage()

      expect(await screen.findByRole('alert')).toHaveTextContent('Le regole non arrivano.')
      expect(screen.queryByRole('heading', { name: /Passo 1 di 3/ })).not.toBeInTheDocument()
    })

    it('says so when the catalog has no character, instead of an empty step', async () => {
      vi.mocked(getCharacters).mockResolvedValue([])
      renderPage()

      expect(await screen.findByText('Nessun personaggio disponibile.')).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: 'Avanti' })).not.toBeInTheDocument()
    })
  })

  describe('step 1 · the characters', () => {
    it('does not go on without a character: it says why and puts the focus on the first choice', async () => {
      await startAtStep1()

      next()

      const first = screen.getByRole('checkbox', { name: /Aalto/ })
      expect(screen.getByText('Scegli almeno un personaggio.')).toBeInTheDocument()
      expect(screen.getByRole('heading', { name: /Passo 1 di 3/ })).toBeInTheDocument()
      expect(first).toBeInvalid()
      expect(first).toHaveAccessibleDescription('Scegli almeno un personaggio.')
      await waitFor(() => expect(first).toHaveFocus())

      choose('Jinhsi')
      expect(screen.queryByText('Scegli almeno un personaggio.')).not.toBeInTheDocument()
    })

    it('lets the user choose and unchoose, and remembers the choices', async () => {
      await startAtStep1()

      choose('Yinlin')
      choose('Aalto')
      choose('Yinlin')

      expect(screen.getByRole('checkbox', { name: /Aalto/ })).toBeChecked()
      expect(screen.getByRole('checkbox', { name: /Yinlin/ })).not.toBeChecked()
    })
  })

  describe('step 2 · levels and goals', () => {
    it('shows the chosen characters only, in the order of the catalog, with the usual levels', async () => {
      await goToStep2('Jinhsi', 'Aalto')

      const titles = screen.getAllByRole('group').map((group) => within(group).getByText(/Aalto|Jinhsi|Yinlin/).textContent)
      expect(titles).toEqual(['Aalto', 'Jinhsi'])
      const aalto = within(character('Aalto'))
      expect(aalto.getByLabelText('Livello attuale')).toHaveValue('1')
      expect(aalto.getByLabelText('Obiettivo')).toHaveValue('40')
      // From level 1 there is nothing to say about an ascension; the goal is the maximum, ascended
      expect(aalto.queryByLabelText(/Ho già fatto l'ascensione/)).not.toBeInTheDocument()
      expect(aalto.getByLabelText("Voglio fare l'ascensione al 40")).toBeChecked()
      expect(screen.getByRole('progressbar')).toHaveAttribute('value', '2')
      expect(screen.queryByRole('group', { name: /Yinlin/ })).not.toBeInTheDocument()
    })

    it('moves the focus to the title of the step, so a keyboard user starts from there', async () => {
      await goToStep2('Aalto')

      await waitFor(() => expect(screen.getByRole('heading', { name: /Passo 2 di 3/ })).toHaveFocus())
    })

    it('offers the ascension only at the levels where it is needed, and starts it as "not done" every time', async () => {
      await goToStep2('Jinhsi')
      const jinhsi = within(character('Jinhsi'))
      const level = jinhsi.getByLabelText('Livello attuale')

      pick(level, '20')
      const done = jinhsi.getByLabelText("Ho già fatto l'ascensione al 20")
      expect(done).not.toBeChecked()
      fireEvent.click(done)
      expect(jinhsi.getByLabelText("Ho già fatto l'ascensione al 20")).toBeChecked()

      pick(level, '25')
      expect(jinhsi.queryByLabelText(/Ho già fatto l'ascensione/)).not.toBeInTheDocument()
      pick(level, '20')
      expect(jinhsi.getByLabelText("Ho già fatto l'ascensione al 20")).not.toBeChecked()
    })

    it('does not go on when a goal is lower than the present, and says it under that goal', async () => {
      await goToStep2('Aalto', 'Jinhsi')
      const jinhsi = within(character('Jinhsi'))
      pick(jinhsi.getByLabelText('Livello attuale'), '30')
      pick(jinhsi.getByLabelText('Obiettivo'), '25')

      next()

      const goal = jinhsi.getByLabelText('Obiettivo')
      expect(jinhsi.getByText(LOWER_GOAL)).toBeInTheDocument()
      expect(within(character('Aalto')).queryByText(LOWER_GOAL)).not.toBeInTheDocument()
      expect(goal).toBeInvalid()
      expect(goal).toHaveAccessibleDescription(LOWER_GOAL)
      expect(screen.getByRole('heading', { name: /Passo 2 di 3/ })).toBeInTheDocument()
      await waitFor(() => expect(goal).toHaveFocus())

      pick(goal, '35')
      expect(jinhsi.queryByText(LOWER_GOAL)).not.toBeInTheDocument()
      next()
      expect(await screen.findByRole('heading', { name: /Passo 3 di 3/ })).toBeInTheDocument()
    })

    it('counts the ascension: the same level before and after it is a goal that goes down', async () => {
      await goToStep2('Jinhsi')
      const jinhsi = within(character('Jinhsi'))
      pick(jinhsi.getByLabelText('Livello attuale'), '20')
      fireEvent.click(jinhsi.getByLabelText("Ho già fatto l'ascensione al 20"))
      pick(jinhsi.getByLabelText('Obiettivo'), '20')

      next()

      expect(jinhsi.getByText(LOWER_GOAL)).toBeInTheDocument()
    })

    it('goes back without losing the choices of the steps', async () => {
      await goToStep2('Aalto', 'Jinhsi')
      pick(within(character('Jinhsi')).getByLabelText('Livello attuale'), '12')

      fireEvent.click(screen.getByRole('button', { name: 'Indietro' }))
      expect(await screen.findByRole('heading', { name: /Passo 1 di 3/ })).toBeInTheDocument()
      expect(screen.getByRole('checkbox', { name: /Aalto/ })).toBeChecked()
      expect(screen.getByRole('checkbox', { name: /Jinhsi/ })).toBeChecked()
      next()

      await screen.findByRole('heading', { name: /Passo 2 di 3/ })
      expect(within(character('Jinhsi')).getByLabelText('Livello attuale')).toHaveValue('12')
    })
  })

  describe('step 3 · Waveplate and play time', () => {
    it('asks for the Waveplate with their help, the Crystals (zero) and the play time with words', async () => {
      await goToStep3()

      const waveplate = screen.getByLabelText('Waveplate attuali')
      expect(waveplate).toHaveAttribute('inputmode', 'numeric')
      expect(waveplate).toBeRequired()
      expect(waveplate).toHaveValue('')
      expect(waveplate).toHaveAccessibleDescription('Il valore che vedi nel gioco, da 0 a 240.')
      expect(screen.getByLabelText('Waveplate Crystals attuali')).toHaveValue('0')
      const options = screen.getByRole('group', { name: 'Quanto giochi al giorno?' })
      expect(within(options).getAllByRole('radio').map((radio) => radio.getAttribute('value'))).toEqual(['30', '60', '120'])
      for (const label of ['30 minuti', '1 ora', '2 ore']) {
        expect(within(options).getByRole('radio', { name: label })).not.toBeChecked()
      }
      expect(screen.getByRole('button', { name: 'Completa la configurazione' })).toBeEnabled()
      expect(screen.getByRole('progressbar')).toHaveAttribute('value', '3')
    })

    it('does not send an empty step: it says what is missing and puts the focus on the first wrong field', async () => {
      await goToStep3()

      fireEvent.click(screen.getByRole('button', { name: 'Completa la configurazione' }))

      const waveplate = screen.getByLabelText('Waveplate attuali')
      expect(screen.getByText('Inserisci le Waveplate che vedi nel gioco.')).toBeInTheDocument()
      expect(screen.getByText('Scegli quanto giochi al giorno.')).toBeInTheDocument()
      expect(waveplate).toBeInvalid()
      expect(screen.getByRole('radio', { name: '30 minuti' })).toBeInvalid()
      expect(postOnboarding).not.toHaveBeenCalled()
      await waitFor(() => expect(waveplate).toHaveFocus())
    })

    it('refuses numbers that cannot be in the game, with the limits in the message', async () => {
      await goToStep3()
      fillStep3('241', '1 ora')
      fireEvent.change(screen.getByLabelText('Waveplate Crystals attuali'), { target: { value: '481' } })

      fireEvent.click(screen.getByRole('button', { name: 'Completa la configurazione' }))

      expect(screen.getByText('Inserisci un numero intero da 0 a 240.')).toBeInTheDocument()
      expect(screen.getByText('Inserisci un numero intero da 0 a 480.')).toBeInTheDocument()
      expect(postOnboarding).not.toHaveBeenCalled()
    })

    it('can go back to the levels', async () => {
      await goToStep3()

      fireEvent.click(screen.getByRole('button', { name: 'Indietro' }))

      expect(await screen.findByRole('heading', { name: /Passo 2 di 3/ })).toBeInTheDocument()
    })
  })

  describe('sending', () => {
    it('sends the three steps together and keeps the updated user', async () => {
      vi.mocked(postOnboarding).mockResolvedValue(setupDone)
      const store = renderPage()
      await screen.findByRole('heading', { name: /Passo 1 di 3/ })
      choose('Aalto')
      choose('Jinhsi')
      next()
      await screen.findByRole('heading', { name: /Passo 2 di 3/ })
      const jinhsi = within(character('Jinhsi'))
      pick(jinhsi.getByLabelText('Livello attuale'), '20')
      fireEvent.click(jinhsi.getByLabelText("Ho già fatto l'ascensione al 20"))
      // The goal starts at 40 with the ascension done: the user takes the tick away, so the goal is 40 without it
      fireEvent.click(jinhsi.getByLabelText("Voglio fare l'ascensione al 40"))
      next()
      await screen.findByRole('heading', { name: /Passo 3 di 3/ })
      fillStep3('180', '1 ora')

      fireEvent.click(screen.getByRole('button', { name: 'Completa la configurazione' }))

      await waitFor(() => expect(store.getState().auth.user?.onboardingCompleted).toBe(true))
      expect(postOnboarding).toHaveBeenCalledWith('test.jwt.token', {
        characters: [
          { characterId: 'aalto', currentLevel: 1, currentRank: 0, targetLevel: 40, targetRank: 2 },
          // Level 20 with the ascension done is rank 1; the goal 40 without the ascension is rank 1 too
          { characterId: 'jinhsi', currentLevel: 20, currentRank: 1, targetLevel: 40, targetRank: 1 },
        ],
        waveplate: { value: 180, crystals: 0 },
        playTimeMinutes: 60,
      })
    })

    it('shows that it is saving and cannot be sent twice', async () => {
      vi.mocked(postOnboarding).mockReturnValue(new Promise(() => {}))
      await goToStep3()
      fillStep3('180', '1 ora')

      fireEvent.click(screen.getByRole('button', { name: 'Completa la configurazione' }))

      const saving = await screen.findByRole('button', { name: 'Salvataggio in corso…' })
      expect(saving).toBeDisabled()
      expect(screen.getByRole('button', { name: 'Indietro' })).toBeDisabled()
      fireEvent.click(saving)
      // Enter in a field sends the form again: it still goes once
      fireEvent.submit(document.querySelector('form')!)
      expect(postOnboarding).toHaveBeenCalledTimes(1)
    })

    it('goes back to the step of a field that the server refuses, and shows the message under it', async () => {
      vi.mocked(postOnboarding).mockRejectedValue(
        new ApiError('VALIDATION_ERROR', 'Alcuni campi non sono validi.', 400, {
          // Jinhsi is the second character of the request (Aalto comes first in the catalog)
          'characters[1].currentLevel': 'Con il grado 1 il livello va da 20 a 40.',
        }),
      )
      await goToStep3()
      fillStep3('180', '1 ora')

      fireEvent.click(screen.getByRole('button', { name: 'Completa la configurazione' }))

      expect(await screen.findByRole('heading', { name: /Passo 2 di 3/ })).toBeInTheDocument()
      const level = within(character('Jinhsi')).getByLabelText('Livello attuale')
      expect(within(character('Jinhsi')).getByText('Con il grado 1 il livello va da 20 a 40.')).toBeInTheDocument()
      expect(level).toBeInvalid()
      await waitFor(() => expect(level).toHaveFocus())
      expect(screen.getByRole('button', { name: 'Avanti' })).toBeEnabled()
    })

    it('shows the message of the server under the Waveplate when that field is refused', async () => {
      vi.mocked(postOnboarding).mockRejectedValue(
        new ApiError('VALIDATION_ERROR', 'Alcuni campi non sono validi.', 400, { 'waveplate.value': 'Le Waveplate vanno da 0 a 240.' }),
      )
      await goToStep3()
      fillStep3('180', '1 ora')

      fireEvent.click(screen.getByRole('button', { name: 'Completa la configurazione' }))

      expect(await screen.findByText('Le Waveplate vanno da 0 a 240.')).toBeInTheDocument()
      expect(screen.getByRole('heading', { name: /Passo 3 di 3/ })).toBeInTheDocument()
      expect(screen.getByLabelText('Waveplate attuali')).toBeInvalid()
      await waitFor(() => expect(screen.getByLabelText('Waveplate attuali')).toHaveFocus())
    })

    it('shows an alert with the message when no field is to blame, and lets the user send again', async () => {
      vi.mocked(postOnboarding).mockRejectedValueOnce(
        new ApiError('NETWORK_ERROR', 'Impossibile contattare il server. Controlla la connessione e riprova.', 0),
      )
      vi.mocked(postOnboarding).mockResolvedValueOnce(setupDone)
      const store = await startAtStep1().then(async (created) => {
        choose('Aalto')
        next()
        await screen.findByRole('heading', { name: /Passo 2 di 3/ })
        next()
        await screen.findByRole('heading', { name: /Passo 3 di 3/ })
        fillStep3('180', '30 minuti')
        return created
      })

      fireEvent.click(screen.getByRole('button', { name: 'Completa la configurazione' }))

      const alert = await screen.findByRole('alert')
      expect(alert).toHaveTextContent('Impossibile contattare il server. Controlla la connessione e riprova.')
      await waitFor(() => expect(alert).toHaveFocus())
      expect(screen.getByRole('button', { name: 'Completa la configurazione' })).toBeEnabled()

      fireEvent.click(screen.getByRole('button', { name: 'Completa la configurazione' }))
      await waitFor(() => expect(store.getState().auth.user?.onboardingCompleted).toBe(true))
    })

    it('takes the user on when the setup turns out to be already completed elsewhere', async () => {
      vi.mocked(postOnboarding).mockRejectedValue(new ApiError('CONFLICT', 'La configurazione è già stata completata.', 409))
      vi.mocked(getProfile).mockResolvedValue(setupDone)
      const store = renderPage()
      await screen.findByRole('heading', { name: /Passo 1 di 3/ })
      choose('Aalto')
      next()
      await screen.findByRole('heading', { name: /Passo 2 di 3/ })
      next()
      await screen.findByRole('heading', { name: /Passo 3 di 3/ })
      fillStep3('180', '30 minuti')

      fireEvent.click(screen.getByRole('button', { name: 'Completa la configurazione' }))

      await waitFor(() => expect(store.getState().auth.user?.onboardingCompleted).toBe(true))
    })
  })
})
