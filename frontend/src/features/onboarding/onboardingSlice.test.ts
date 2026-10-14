import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client'
import { setupStore } from '../../app/store'
import { catalogCharacters, gameRules } from '../../test/fixtures'
import { rover, signedIn } from '../../test/utils'
import { getProfile } from '../auth/authApi'
import { login, sessionEnded } from '../auth/authSlice'
import { login as loginRequest } from '../auth/authApi'
import { postOnboarding } from './onboardingApi'
import {
  advance,
  ascensionChanged,
  characterToggled,
  crystalsTyped,
  levelChanged,
  playTimeChosen,
  stepBack,
  submit,
  waveplateTyped,
} from './onboardingSlice'

vi.mock('./onboardingApi', () => ({ postOnboarding: vi.fn() }))
vi.mock('../auth/authApi', () => ({ login: vi.fn(), register: vi.fn(), getProfile: vi.fn() }))

const MAX_LEVEL = gameRules.maxLevel
const setupDone = { ...rover, onboardingCompleted: true, playTimeMinutes: 60 }

// A store of someone who is logged in and has the setup still to do
function newStore() {
  return setupStore(signedIn({ onboardingCompleted: false }))
}

type Store = ReturnType<typeof newStore>

// A draft that is complete and valid, up to step 3
function fillEverything(store: Store) {
  store.dispatch(characterToggled('jinhsi', MAX_LEVEL))
  store.dispatch(characterToggled('aalto', MAX_LEVEL))
  store.dispatch(waveplateTyped('180'))
  store.dispatch(playTimeChosen(60))
  store.dispatch(advance(gameRules))
  store.dispatch(advance(gameRules))
}

describe('onboardingSlice · the draft', () => {
  it('starts at step 1 with nothing chosen', () => {
    const state = newStore().getState().onboarding

    expect(state).toMatchObject({
      step: 1,
      selected: [],
      drafts: {},
      waveplate: '',
      crystals: '0',
      playTimeMinutes: null,
      errors: {},
      generalError: null,
      submitStatus: 'idle',
      failedAttempts: 0,
    })
  })

  it('creates the default levels when a character is chosen: from level 1 to the maximum, ascended', () => {
    const store = newStore()

    store.dispatch(characterToggled('jinhsi', MAX_LEVEL))

    expect(store.getState().onboarding.selected).toEqual(['jinhsi'])
    expect(store.getState().onboarding.drafts.jinhsi).toEqual({
      current: { level: 1, ascended: false },
      target: { level: 40, ascended: true },
    })
  })

  it('unchooses a character but remembers its levels for when it is chosen again', () => {
    const store = newStore()
    store.dispatch(characterToggled('jinhsi', MAX_LEVEL))
    store.dispatch(levelChanged({ id: 'jinhsi', side: 'current', level: 25 }))

    store.dispatch(characterToggled('jinhsi', MAX_LEVEL))
    expect(store.getState().onboarding.selected).toEqual([])
    store.dispatch(characterToggled('jinhsi', MAX_LEVEL))

    expect(store.getState().onboarding.selected).toEqual(['jinhsi'])
    expect(store.getState().onboarding.drafts.jinhsi.current.level).toBe(25)
  })

  it('puts the ascension back to "not done" when the level changes, and ignores a level that is not a number', () => {
    const store = newStore()
    store.dispatch(characterToggled('jinhsi', MAX_LEVEL))
    store.dispatch(levelChanged({ id: 'jinhsi', side: 'current', level: 20 }))
    store.dispatch(ascensionChanged({ id: 'jinhsi', side: 'current', ascended: true }))
    expect(store.getState().onboarding.drafts.jinhsi.current).toEqual({ level: 20, ascended: true })

    store.dispatch(levelChanged({ id: 'jinhsi', side: 'current', level: 25 }))

    expect(store.getState().onboarding.drafts.jinhsi.current).toEqual({ level: 25, ascended: false })
    // The goal was not touched
    expect(store.getState().onboarding.drafts.jinhsi.target).toEqual({ level: 40, ascended: true })
  })

  it('keeps the text of the Waveplate and the Crystals as typed, and the play time as chosen', () => {
    const store = newStore()

    store.dispatch(waveplateTyped('18'))
    store.dispatch(crystalsTyped('1 '))
    store.dispatch(playTimeChosen(120))

    expect(store.getState().onboarding).toMatchObject({ waveplate: '18', crystals: '1 ', playTimeMinutes: 120 })
  })
})

describe('onboardingSlice · moving between the steps', () => {
  it('stays in step 1 with a message when no character is chosen, and counts the failed attempt', () => {
    const store = newStore()

    store.dispatch(advance(gameRules))

    expect(store.getState().onboarding.step).toBe(1)
    expect(store.getState().onboarding.errors).toEqual({ characters: 'Scegli almeno un personaggio.' })
    expect(store.getState().onboarding.failedAttempts).toBe(1)
  })

  it('goes to step 2 when a character is chosen, and forgets the old messages', () => {
    const store = newStore()
    store.dispatch(advance(gameRules))
    store.dispatch(characterToggled('jinhsi', MAX_LEVEL))

    store.dispatch(advance(gameRules))

    expect(store.getState().onboarding.step).toBe(2)
    expect(store.getState().onboarding.errors).toEqual({})
  })

  it('clears the message of the characters as soon as one is chosen', () => {
    const store = newStore()
    store.dispatch(advance(gameRules))

    store.dispatch(characterToggled('jinhsi', MAX_LEVEL))

    expect(store.getState().onboarding.errors).toEqual({})
  })

  it('stays in step 2 when a goal is lower than the present, and clears the message when the goal is changed', () => {
    const store = newStore()
    store.dispatch(characterToggled('jinhsi', MAX_LEVEL))
    store.dispatch(advance(gameRules))
    store.dispatch(levelChanged({ id: 'jinhsi', side: 'current', level: 30 }))
    store.dispatch(levelChanged({ id: 'jinhsi', side: 'target', level: 25 }))

    store.dispatch(advance(gameRules))

    expect(store.getState().onboarding.step).toBe(2)
    expect(store.getState().onboarding.errors).toEqual({
      'progress.jinhsi.target': "L'obiettivo non può essere più basso della situazione attuale.",
    })

    store.dispatch(levelChanged({ id: 'jinhsi', side: 'target', level: 35 }))

    expect(store.getState().onboarding.errors).toEqual({})
  })

  it('goes back without checking anything, and never below step 1', () => {
    const store = newStore()
    store.dispatch(characterToggled('jinhsi', MAX_LEVEL))
    store.dispatch(advance(gameRules))

    store.dispatch(stepBack())
    store.dispatch(stepBack())

    expect(store.getState().onboarding.step).toBe(1)
    expect(store.getState().onboarding.selected).toEqual(['jinhsi'])
  })

  it('does not go past step 3 with advance: the last step is sent', () => {
    const store = newStore()
    fillEverything(store)
    expect(store.getState().onboarding.step).toBe(3)

    store.dispatch(advance(gameRules))

    expect(store.getState().onboarding.step).toBe(3)
  })
})

describe('onboardingSlice · sending the setup', () => {
  afterEach(() => {
    vi.mocked(postOnboarding).mockReset()
    vi.mocked(getProfile).mockReset()
  })

  it('does not call the server when the last step is not valid', async () => {
    const store = newStore()
    fillEverything(store)
    store.dispatch(waveplateTyped(''))

    await store.dispatch(submit(gameRules, catalogCharacters))

    expect(postOnboarding).not.toHaveBeenCalled()
    expect(store.getState().onboarding.errors).toEqual({ waveplate: 'Inserisci le Waveplate che vedi nel gioco.' })
    expect(store.getState().onboarding.failedAttempts).toBe(1)
  })

  it('sends the whole setup with the token, then stores the updated user and forgets the draft', async () => {
    vi.mocked(postOnboarding).mockResolvedValue(setupDone)
    const store = newStore()
    fillEverything(store)

    await store.dispatch(submit(gameRules, catalogCharacters))

    expect(postOnboarding).toHaveBeenCalledWith('test.jwt.token', {
      characters: [
        { characterId: 'aalto', currentLevel: 1, currentRank: 0, targetLevel: 40, targetRank: 2 },
        { characterId: 'jinhsi', currentLevel: 1, currentRank: 0, targetLevel: 40, targetRank: 2 },
      ],
      waveplate: { value: 180, crystals: 0 },
      playTimeMinutes: 60,
    })
    expect(store.getState().auth.user).toEqual(setupDone)
    expect(store.getState().onboarding.selected).toEqual([])
    expect(store.getState().onboarding.step).toBe(1)
    expect(store.getState().toast.items.at(-1)).toMatchObject({ message: 'Configurazione completata.', variant: 'success' })
  })

  it('sends only once when it is called again while the first request is still running', async () => {
    let finish: (user: typeof setupDone) => void = () => {}
    vi.mocked(postOnboarding).mockReturnValue(new Promise((resolve) => (finish = resolve)))
    const store = newStore()
    fillEverything(store)

    const first = store.dispatch(submit(gameRules, catalogCharacters))
    expect(store.getState().onboarding.submitStatus).toBe('submitting')
    await store.dispatch(submit(gameRules, catalogCharacters))
    finish(setupDone)
    await first

    expect(postOnboarding).toHaveBeenCalledTimes(1)
  })

  it('goes back to the step of the first wrong field and shows the message of the server under it', async () => {
    vi.mocked(postOnboarding).mockRejectedValue(
      new ApiError('VALIDATION_ERROR', 'Alcuni campi non sono validi.', 400, {
        'characters[1].currentLevel': 'Con il grado 1 il livello va da 20 a 40.',
        'waveplate.value': 'Le Waveplate vanno da 0 a 240.',
      }),
    )
    const store = newStore()
    fillEverything(store)

    await store.dispatch(submit(gameRules, catalogCharacters))

    expect(store.getState().onboarding.step).toBe(2)
    expect(store.getState().onboarding.errors).toEqual({
      'progress.jinhsi.current': 'Con il grado 1 il livello va da 20 a 40.',
      waveplate: 'Le Waveplate vanno da 0 a 240.',
    })
    expect(store.getState().onboarding.generalError).toBeNull()
    expect(store.getState().onboarding.submitStatus).toBe('idle')
    expect(store.getState().onboarding.failedAttempts).toBe(1)
    // The choices are still there: the user fixes the field and sends again
    expect(store.getState().onboarding.selected).toEqual(['jinhsi', 'aalto'])
    expect(store.getState().auth.user?.onboardingCompleted).toBe(false)
  })

  it('keeps the message of a failure that names no field for the alert of the page', async () => {
    vi.mocked(postOnboarding).mockRejectedValue(
      new ApiError('NETWORK_ERROR', 'Impossibile contattare il server. Controlla la connessione e riprova.', 0),
    )
    const store = newStore()
    fillEverything(store)

    await store.dispatch(submit(gameRules, catalogCharacters))

    expect(store.getState().onboarding.step).toBe(3)
    expect(store.getState().onboarding.errors).toEqual({})
    expect(store.getState().onboarding.generalError).toBe('Impossibile contattare il server. Controlla la connessione e riprova.')
    expect(store.getState().onboarding.submitStatus).toBe('idle')
  })

  it('takes a conflict as success when the setup was completed elsewhere, and says so', async () => {
    vi.mocked(postOnboarding).mockRejectedValue(
      new ApiError('CONFLICT', 'La configurazione è già stata completata.', 409),
    )
    vi.mocked(getProfile).mockResolvedValue(setupDone)
    const store = newStore()
    fillEverything(store)

    await store.dispatch(submit(gameRules, catalogCharacters))

    expect(getProfile).toHaveBeenCalledWith('test.jwt.token')
    expect(store.getState().auth.user).toEqual(setupDone)
    expect(store.getState().onboarding.selected).toEqual([])
    expect(store.getState().toast.items.at(-1)).toMatchObject({
      message: 'La configurazione era già stata completata.',
      variant: 'info',
    })
  })

  it('shows the message of a conflict when the setup is still to do (the roster changed meanwhile)', async () => {
    vi.mocked(postOnboarding).mockRejectedValue(
      new ApiError('CONFLICT', 'Il tuo roster è cambiato mentre salvavi la configurazione: riprova.', 409),
    )
    vi.mocked(getProfile).mockResolvedValue({ ...rover, onboardingCompleted: false })
    const store = newStore()
    fillEverything(store)

    await store.dispatch(submit(gameRules, catalogCharacters))

    expect(store.getState().onboarding.generalError).toBe(
      'Il tuo roster è cambiato mentre salvavi la configurazione: riprova.',
    )
    expect(store.getState().auth.user?.onboardingCompleted).toBe(false)
    expect(store.getState().onboarding.selected).toEqual(['jinhsi', 'aalto'])
  })

  it('keeps the original message of a conflict when the profile cannot be read', async () => {
    vi.mocked(postOnboarding).mockRejectedValue(
      new ApiError('CONFLICT', 'La configurazione è già stata completata.', 409),
    )
    vi.mocked(getProfile).mockRejectedValue(new ApiError('NETWORK_ERROR', 'Impossibile contattare il server.', 0))
    const store = newStore()
    fillEverything(store)

    await store.dispatch(submit(gameRules, catalogCharacters))

    expect(store.getState().onboarding.generalError).toBe('La configurazione è già stata completata.')
  })

  it('does not call the server without a session', async () => {
    const store = setupStore()
    fillEverything(store)

    await store.dispatch(submit(gameRules, catalogCharacters))

    expect(postOnboarding).not.toHaveBeenCalled()
    expect(store.getState().onboarding.generalError).not.toBeNull()
  })

  it('ignores a late answer of a session that ended: it must not touch the draft of the next user', async () => {
    let fail: (error: Error) => void = () => {}
    vi.mocked(postOnboarding).mockReturnValue(new Promise((_, reject) => (fail = reject)))
    const store = newStore()
    fillEverything(store)
    const sending = store.dispatch(submit(gameRules, catalogCharacters))

    // Alice leaves while her request is still on its way, and Bob logs in and starts his own draft
    store.dispatch(sessionEnded())
    vi.mocked(loginRequest).mockResolvedValue({ token: 'bob.jwt.token', user: { ...rover, id: 8, username: 'bob', onboardingCompleted: false } })
    await store.dispatch(login({ email: 'bob@example.com', password: 'S3cret-pass' }))
    store.dispatch(characterToggled('aalto', MAX_LEVEL))
    fail(new ApiError('VALIDATION_ERROR', 'Alcuni campi non sono validi.', 400, { 'waveplate.value': 'Le Waveplate vanno da 0 a 240.' }))
    await sending

    expect(store.getState().onboarding).toMatchObject({ step: 1, selected: ['aalto'], errors: {}, failedAttempts: 0 })
  })

  it('does not tell a user who is not the sender that the setup is complete, nor update them', async () => {
    let finish: (user: typeof setupDone) => void = () => {}
    vi.mocked(postOnboarding).mockReturnValue(new Promise((resolve) => (finish = resolve)))
    const store = newStore()
    fillEverything(store)
    const sending = store.dispatch(submit(gameRules, catalogCharacters))
    store.dispatch(sessionEnded())

    finish(setupDone)
    await sending

    expect(store.getState().toast.items).toEqual([])
    expect(store.getState().auth.user).toBeNull()
  })

  it('forgets the draft when another user logs in, even without a logout in between (two tabs)', async () => {
    vi.mocked(loginRequest).mockResolvedValue({ token: 'bob.jwt.token', user: { ...rover, id: 8, onboardingCompleted: false } })
    const store = newStore()
    fillEverything(store)

    await store.dispatch(login({ email: 'bob@example.com', password: 'S3cret-pass' }))

    expect(store.getState().onboarding).toMatchObject({ step: 1, selected: [], waveplate: '' })
  })

  it('forgets the draft when the session ends, so the next user does not see it (D103)', () => {
    const store = newStore()
    fillEverything(store)

    store.dispatch(sessionEnded())

    expect(store.getState().onboarding).toMatchObject({ step: 1, selected: [], drafts: {}, waveplate: '', playTimeMinutes: null })
  })
})
