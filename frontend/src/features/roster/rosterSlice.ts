import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { describeError, type RequestFailure } from '../../api/client'
import type { RootState } from '../../app/store'
import { login, register, restoreSession, sessionEnded } from '../auth/authSlice'
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
import type { CharacterRequirements, FarmingPlan, InventoryItem, Progress, RosterEntry } from './types'

// The data of the roster page (D115): roster, inventory, the plan of the whole roster and the requirements of every
// character. Only loadRoster writes them; a change asks the server again, because the numbers are always computed
// in Java (D3). They belong to the user of the session: they are forgotten when it ends or another user logs in (D103).

type LoadStatus = 'idle' | 'loading' | 'succeeded' | 'failed'

interface RosterData {
  entries: RosterEntry[]
  requirements: Record<string, CharacterRequirements>
  plan: FarmingPlan
  inventory: Record<string, number>
}

interface RosterState {
  // The first load; after it the data stay on the page while a reload runs
  status: LoadStatus
  // The message of the last load that failed, the first one or a reload
  error: string | null
  refreshing: boolean
  // The load that is running: the answer of any older one is not used
  loadRequestId: string | null
  entries: RosterEntry[]
  requirements: Record<string, CharacterRequirements>
  plan: FarmingPlan | null
  inventory: Record<string, number>
}

const initialState: RosterState = {
  status: 'idle',
  error: null,
  refreshing: false,
  loadRequestId: null,
  entries: [],
  requirements: {},
  plan: null,
  inventory: {},
}

const NO_SESSION_MESSAGE = 'Devi accedere per vedere il tuo roster.'
const NO_SESSION: RequestFailure = { code: 'NO_SESSION', message: NO_SESSION_MESSAGE, status: 0 }

type LoadConfig = { state: RootState; rejectValue: string }
type ChangeConfig = { state: RootState; rejectValue: RequestFailure }

/** Roster, inventory and plan together, then the requirements of every character of the roster (D115). */
export const loadRoster = createAsyncThunk<RosterData, void, LoadConfig>(
  'roster/load',
  async (_, { getState, rejectWithValue }) => {
    const token = getState().auth.token
    if (!token) {
      return rejectWithValue(NO_SESSION_MESSAGE)
    }
    try {
      const [entries, items, plan] = await Promise.all([getRoster(token), getInventory(token), getFarmingPlan(token)])
      const requirements = await Promise.all(entries.map((entry) => getRequirements(token, entry.characterId)))
      return {
        entries,
        plan,
        // fromEntries defines own keys: an id can never reach the prototype of the object
        requirements: Object.fromEntries(requirements.map((item) => [item.characterId, item])),
        inventory: Object.fromEntries(items.map((item: InventoryItem) => [item.materialId, item.quantity])),
      }
    } catch (error) {
      return rejectWithValue(describeError(error).message)
    }
  },
)

/**
 * Runs a change with the token of the session and, if it worked and the session is the same, reloads the page data
 * before it answers: the form closes, the focus moves and the toast appears when the numbers are already the new
 * ones. The failure is returned as it is, so the form can show the message of every field under it.
 */
async function change<T>(
  getState: () => RootState,
  dispatch: (action: ReturnType<typeof loadRoster>) => Promise<unknown>,
  request: (token: string) => Promise<T>,
): Promise<{ result: T } | { failure: RequestFailure }> {
  const token = getState().auth.token
  if (!token) {
    return { failure: NO_SESSION }
  }
  let result: T
  try {
    result = await request(token)
  } catch (error) {
    return { failure: describeError(error) }
  }
  // The answer may arrive when this session is over: then nothing is reloaded for the next user. A reload that
  // fails does not undo the change: the page keeps the data of before and says that they are not up to date
  if (getState().auth.token === token) {
    await dispatch(loadRoster())
  }
  return { result }
}

export const addCharacter = createAsyncThunk<RosterEntry, RosterEntry, ChangeConfig>(
  'roster/add',
  async (entry, { getState, dispatch, rejectWithValue }) => {
    const outcome = await change(getState, dispatch, (token) => addToRoster(token, entry))
    return 'failure' in outcome ? rejectWithValue(outcome.failure) : outcome.result
  },
)

export const updateCharacter = createAsyncThunk<RosterEntry, { characterId: string; progress: Progress }, ChangeConfig>(
  'roster/update',
  async ({ characterId, progress }, { getState, dispatch, rejectWithValue }) => {
    const outcome = await change(getState, dispatch, (token) => updateInRoster(token, characterId, progress))
    return 'failure' in outcome ? rejectWithValue(outcome.failure) : outcome.result
  },
)

export const removeCharacter = createAsyncThunk<string, string, ChangeConfig>(
  'roster/remove',
  async (characterId, { getState, dispatch, rejectWithValue }) => {
    const outcome = await change(getState, dispatch, (token) => removeFromRoster(token, characterId))
    return 'failure' in outcome ? rejectWithValue(outcome.failure) : characterId
  },
)

export const saveQuantity = createAsyncThunk<InventoryItem, InventoryItem, ChangeConfig>(
  'roster/saveQuantity',
  async ({ materialId, quantity }, { getState, dispatch, rejectWithValue }) => {
    const outcome = await change(getState, dispatch, (token) => putInventoryQuantity(token, materialId, quantity))
    return 'failure' in outcome ? rejectWithValue(outcome.failure) : outcome.result
  },
)

/** The requirements of a character, if they were loaded; an id is never read from the prototype of the object. */
export function requirementsOf(state: RootState, characterId: string): CharacterRequirements | undefined {
  const { requirements } = state.roster
  return Object.hasOwn(requirements, characterId) ? requirements[characterId] : undefined
}

const rosterSlice = createSlice({
  name: 'roster',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      // Someone else may log in on this browser next: nothing of this user's data stays (D103)
      .addCase(sessionEnded, () => initialState)
      .addCase(login.fulfilled, () => initialState)
      .addCase(register.fulfilled, () => initialState)
      .addCase(restoreSession.fulfilled, () => initialState)
      .addCase(loadRoster.pending, (state, action) => {
        state.loadRequestId = action.meta.requestId
        if (state.status === 'succeeded') {
          state.refreshing = true
        } else {
          state.status = 'loading'
          state.error = null
        }
      })
      .addCase(loadRoster.fulfilled, (state, action) => {
        if (action.meta.requestId !== state.loadRequestId) {
          return
        }
        state.status = 'succeeded'
        state.error = null
        state.refreshing = false
        state.loadRequestId = null
        state.entries = action.payload.entries
        state.requirements = action.payload.requirements
        state.plan = action.payload.plan
        state.inventory = action.payload.inventory
      })
      .addCase(loadRoster.rejected, (state, action) => {
        if (action.meta.requestId !== state.loadRequestId) {
          return
        }
        // A reload that fails keeps the data of before on the page, with the message and a way to try again
        state.status = state.status === 'succeeded' ? 'succeeded' : 'failed'
        state.error = action.payload ?? describeError(action.error).message
        state.refreshing = false
        state.loadRequestId = null
      })
  },
})

export default rosterSlice.reducer
