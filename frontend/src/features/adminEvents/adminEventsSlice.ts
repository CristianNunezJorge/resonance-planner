import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { describeError, type RequestFailure } from '../../api/client'
import type { RootState } from '../../app/store'
import { enterAsGuest, login, register, restoreSession, sessionEnded } from '../auth/authSlice'
import { createEvent, deleteEvent, listEvents, updateEvent } from './adminEventsApi'
import type { AdminEvent, EventRequest } from './types'

// The events of the admin page (D122): only loadAdminEvents writes them, and every change asks the server again for
// the whole list, as on the roster and calendar pages (D115, D120). They are forgotten when the session ends or
// another user logs in (D103).

type LoadStatus = 'idle' | 'loading' | 'succeeded' | 'failed'

interface AdminEventsState {
  status: LoadStatus
  error: string | null
  refreshing: boolean
  // The load that is running: the answer of any older one is not used
  loadRequestId: string | null
  events: AdminEvent[]
}

const initialState: AdminEventsState = {
  status: 'idle',
  error: null,
  refreshing: false,
  loadRequestId: null,
  events: [],
}

const NO_SESSION_MESSAGE = 'Devi accedere come admin per gestire gli eventi.'
const NO_SESSION: RequestFailure = { code: 'NO_SESSION', message: NO_SESSION_MESSAGE, status: 0 }

type LoadConfig = { state: RootState; rejectValue: string }
type ChangeConfig = { state: RootState; rejectValue: RequestFailure }

export const loadAdminEvents = createAsyncThunk<AdminEvent[], void, LoadConfig>(
  'adminEvents/load',
  async (_, { getState, rejectWithValue }) => {
    const token = getState().auth.token
    if (!token) {
      return rejectWithValue(NO_SESSION_MESSAGE)
    }
    try {
      return await listEvents(token)
    } catch (error) {
      return rejectWithValue(describeError(error).message)
    }
  },
)

/** Runs a change and, if it worked for the same session, reloads the list before it answers (as calendarSlice). */
async function change<T>(
  getState: () => RootState,
  dispatch: (action: ReturnType<typeof loadAdminEvents>) => Promise<unknown>,
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
  if (getState().auth.token === token) {
    await dispatch(loadAdminEvents())
  }
  return { result }
}

export const createAdminEvent = createAsyncThunk<AdminEvent, EventRequest, ChangeConfig>(
  'adminEvents/create',
  async (event, { getState, dispatch, rejectWithValue }) => {
    const outcome = await change(getState, dispatch, (token) => createEvent(token, event))
    return 'failure' in outcome ? rejectWithValue(outcome.failure) : outcome.result
  },
)

export const updateAdminEvent = createAsyncThunk<AdminEvent, { id: number; event: EventRequest }, ChangeConfig>(
  'adminEvents/update',
  async ({ id, event }, { getState, dispatch, rejectWithValue }) => {
    const outcome = await change(getState, dispatch, (token) => updateEvent(token, id, event))
    return 'failure' in outcome ? rejectWithValue(outcome.failure) : outcome.result
  },
)

export const deleteAdminEvent = createAsyncThunk<number, number, ChangeConfig>(
  'adminEvents/delete',
  async (id, { getState, dispatch, rejectWithValue }) => {
    const outcome = await change(getState, dispatch, (token) => deleteEvent(token, id))
    return 'failure' in outcome ? rejectWithValue(outcome.failure) : id
  },
)

const adminEventsSlice = createSlice({
  name: 'adminEvents',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(sessionEnded, () => initialState)
      .addCase(login.fulfilled, () => initialState)
      .addCase(register.fulfilled, () => initialState)
      .addCase(enterAsGuest.fulfilled, () => initialState)
      .addCase(restoreSession.fulfilled, () => initialState)
      .addCase(loadAdminEvents.pending, (state, action) => {
        state.loadRequestId = action.meta.requestId
        if (state.status === 'succeeded') {
          state.refreshing = true
        } else {
          state.status = 'loading'
          state.error = null
        }
      })
      .addCase(loadAdminEvents.fulfilled, (state, action) => {
        if (action.meta.requestId !== state.loadRequestId) {
          return
        }
        state.status = 'succeeded'
        state.error = null
        state.refreshing = false
        state.loadRequestId = null
        state.events = action.payload
      })
      .addCase(loadAdminEvents.rejected, (state, action) => {
        if (action.meta.requestId !== state.loadRequestId) {
          return
        }
        state.status = state.status === 'succeeded' ? 'succeeded' : 'failed'
        state.error = action.payload ?? describeError(action.error).message
        state.refreshing = false
        state.loadRequestId = null
      })
  },
})

export default adminEventsSlice.reducer
