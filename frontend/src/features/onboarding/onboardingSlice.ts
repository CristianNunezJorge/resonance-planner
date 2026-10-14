import { createAsyncThunk, createSlice, type PayloadAction } from '@reduxjs/toolkit'
import { describeError, type RequestFailure } from '../../api/client'
import { toastShown } from '../../app/toastSlice'
import type { AppThunk, RootState } from '../../app/store'
import { getProfile } from '../auth/authApi'
import { login, register, restoreSession, sessionEnded, userUpdated } from '../auth/authSlice'
import type { User } from '../auth/types'
import type { CharacterSummary, GameRules } from '../catalog/types'
import { mapFailure } from './failureMapping'
import { postOnboarding } from './onboardingApi'
import { buildRequest } from './request'
import type { DraftState, OnboardingRequest, Step } from './types'
import {
  CHARACTERS_KEY,
  CRYSTALS_KEY,
  PLAY_TIME_KEY,
  WAVEPLATE_KEY,
  progressKey,
  validateStep,
  type FieldErrors,
} from './validation'

// The draft of the guided setup (D20, D50): what the user picks and types in the three steps stays here, and the last
// step sends it all in one request. It is forgotten when the setup is saved and when the session ends (D103).

type SubmitStatus = 'idle' | 'submitting'

interface OnboardingState extends DraftState {
  step: Step
  // The messages under the fields of the page, by the keys of validation.ts
  errors: FieldErrors
  // A failure that is about no field, for the alert of the page
  generalError: string | null
  submitStatus: SubmitStatus
  // The request that is running: the answer of any other one (of a session that ended) is not for this draft
  submitRequestId: string | null
  // Counts the attempts that failed: the page moves the keyboard focus to the first wrong field each time it grows
  failedAttempts: number
}

const FIRST_STEP: Step = 1
const LAST_STEP: Step = 3
const CONFLICT = 409
const NO_SESSION_MESSAGE = 'Devi accedere per completare la configurazione.'

const initialState: OnboardingState = {
  step: FIRST_STEP,
  selected: [],
  drafts: {},
  waveplate: '',
  crystals: '0',
  playTimeMinutes: null,
  errors: {},
  generalError: null,
  submitStatus: 'idle',
  submitRequestId: null,
  failedAttempts: 0,
}

type Side = 'current' | 'target'

const onboardingSlice = createSlice({
  name: 'onboarding',
  initialState,
  reducers: {
    // Chooses or unchooses a character. The first time, it starts from level 1 with the maximum level as the goal,
    // ascended (the usual goal); unchosen and chosen again, it remembers the levels the user had set.
    characterToggled: {
      reducer(state, action: PayloadAction<{ id: string; defaultTargetLevel: number }>) {
        const { id, defaultTargetLevel } = action.payload
        if (state.selected.includes(id)) {
          state.selected = state.selected.filter((chosen) => chosen !== id)
        } else {
          state.selected.push(id)
          state.drafts[id] ??= {
            current: { level: 1, ascended: false },
            target: { level: defaultTargetLevel, ascended: true },
          }
        }
        delete state.errors[CHARACTERS_KEY]
      },
      prepare(id: string, defaultTargetLevel: number) {
        return { payload: { id, defaultTargetLevel } }
      },
    },
    // A new level always starts with the ascension "not done": it is shown only at the caps, and a stale tick from a
    // level the user left would be a surprise
    levelChanged(state, action: PayloadAction<{ id: string; side: Side; level: number }>) {
      const { id, side, level } = action.payload
      const draft = state.drafts[id]
      if (draft) {
        draft[side] = { level, ascended: false }
        delete state.errors[progressKey(id, side)]
      }
    },
    ascensionChanged(state, action: PayloadAction<{ id: string; side: Side; ascended: boolean }>) {
      const { id, side, ascended } = action.payload
      const draft = state.drafts[id]
      if (draft) {
        draft[side].ascended = ascended
        delete state.errors[progressKey(id, side)]
      }
    },
    waveplateTyped(state, action: PayloadAction<string>) {
      state.waveplate = action.payload
      delete state.errors[WAVEPLATE_KEY]
    },
    crystalsTyped(state, action: PayloadAction<string>) {
      state.crystals = action.payload
      delete state.errors[CRYSTALS_KEY]
    },
    playTimeChosen(state, action: PayloadAction<number>) {
      state.playTimeMinutes = action.payload
      delete state.errors[PLAY_TIME_KEY]
    },
    stepAdvanced(state) {
      state.step = Math.min(state.step + 1, LAST_STEP) as Step
      state.errors = {}
      state.generalError = null
    },
    // Going back checks nothing: the user may want to change an earlier choice before fixing this step
    stepBack(state) {
      state.step = Math.max(state.step - 1, FIRST_STEP) as Step
      state.errors = {}
      state.generalError = null
    },
    // `step` is given when the earliest wrong field is in a step before the one the user is on
    validationFailed(state, action: PayloadAction<{ errors: FieldErrors; step?: Step }>) {
      state.errors = action.payload.errors
      state.generalError = null
      state.step = action.payload.step ?? state.step
      state.failedAttempts += 1
    },
  },
  extraReducers: (builder) => {
    builder
      // Someone else may log in on this browser next: nothing of this user's draft stays (D103)
      .addCase(sessionEnded, () => initialState)
      // A login that replaces the user without a logout (a login in another tab) is a new user too
      .addCase(login.fulfilled, () => initialState)
      .addCase(register.fulfilled, () => initialState)
      .addCase(restoreSession.fulfilled, () => initialState)
      .addCase(submitOnboarding.pending, (state, action) => {
        state.submitStatus = 'submitting'
        state.submitRequestId = action.meta.requestId
        state.errors = {}
        state.generalError = null
      })
      // Saved: the setup is over, and so is its draft
      .addCase(submitOnboarding.fulfilled, (state, action) =>
        action.meta.requestId === state.submitRequestId ? initialState : state,
      )
      .addCase(submitOnboarding.rejected, (state, action) => {
        // Not sent at all (another submission was running), or the answer of a request that is not the one of this
        // draft anymore (the session ended meanwhile): nothing happened to this draft
        if (action.meta.condition || action.meta.requestId !== state.submitRequestId) {
          return
        }
        const failure = action.payload ?? describeError(undefined)
        const characterIds = action.meta.arg.characters.map((character) => character.characterId)
        const mapped = mapFailure(failure, characterIds)
        state.submitStatus = 'idle'
        state.submitRequestId = null
        state.errors = mapped.errors
        state.generalError = mapped.generalError
        state.step = mapped.step ?? state.step
        state.failedAttempts += 1
      })
  },
})

export const {
  characterToggled,
  levelChanged,
  ascensionChanged,
  waveplateTyped,
  crystalsTyped,
  playTimeChosen,
  stepAdvanced,
  stepBack,
  validationFailed,
} = onboardingSlice.actions

/**
 * Sends the three steps and answers with the user as the server saved it. If the server says the setup was already
 * completed (409), the profile is read: when it is completed, the user simply goes on, as if this request had
 * succeeded; otherwise the conflict is another one (the roster changed meanwhile) and the user sees its message.
 */
export const submitOnboarding = createAsyncThunk<
  User,
  OnboardingRequest,
  { state: RootState; rejectValue: RequestFailure }
>(
  'onboarding/submit',
  async (request, { getState, dispatch, rejectWithValue }) => {
    const token = getState().auth.token
    if (!token) {
      return rejectWithValue({ code: 'NO_SESSION', message: NO_SESSION_MESSAGE, status: 0 })
    }
    // The answer may arrive when this session is over (a slow server): then nobody is told and nothing is updated
    const isSameSession = () => getState().auth.token === token
    let user: User
    try {
      user = await postOnboarding(token, request)
    } catch (error) {
      const failure = describeError(error)
      const completed = failure.status === CONFLICT ? await profileIfCompleted(token) : null
      if (!completed) {
        return rejectWithValue(failure)
      }
      if (isSameSession()) {
        dispatch(userUpdated(completed))
        dispatch(toastShown('La configurazione era già stata completata.', 'info'))
      }
      return completed
    }
    if (isSameSession()) {
      dispatch(userUpdated(user))
      dispatch(toastShown('Configurazione completata.'))
    }
    return user
  },
  {
    // A double click, or Enter pressed twice, sends the setup once
    condition: (_, { getState }) => getState().onboarding.submitStatus !== 'submitting',
  },
)

async function profileIfCompleted(token: string): Promise<User | null> {
  try {
    const profile = await getProfile(token)
    return profile.onboardingCompleted ? profile : null
  } catch {
    // The profile cannot be read: the user sees the message of the conflict, and can try again
    return null
  }
}

/** "Avanti": checks the step the user is on and goes to the next one, or stays and shows what to fix. */
export const advance =
  (rules: GameRules): AppThunk =>
  (dispatch, getState) => {
    const { onboarding } = getState()
    if (onboarding.step >= LAST_STEP) {
      return
    }
    const errors = validateStep(onboarding.step, onboarding, rules)
    dispatch(Object.keys(errors).length > 0 ? validationFailed({ errors }) : stepAdvanced())
  }

/**
 * The last step: checks every step, earliest first, and sends the setup if all are valid. The steps are checked in
 * order on the way, so a wrong one here means the draft was changed some other way: the user is taken to it.
 */
export const submit =
  (rules: GameRules, characters: CharacterSummary[]): AppThunk<Promise<void>> =>
  async (dispatch, getState) => {
    const draft = getState().onboarding
    for (const step of [1, 2, 3] as const) {
      const errors = validateStep(step, draft, rules)
      if (Object.keys(errors).length > 0) {
        dispatch(validationFailed({ errors, step }))
        return
      }
    }
    await dispatch(submitOnboarding(buildRequest(draft, characters, rules)))
  }

export default onboardingSlice.reducer
