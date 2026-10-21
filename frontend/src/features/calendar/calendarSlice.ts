import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { describeError, type RequestFailure } from '../../api/client'
import type { RootState } from '../../app/store'
import { enterAsGuest, login, register, restoreSession, sessionEnded } from '../auth/authSlice'
import {
  completeTask,
  confirmProposal,
  createBasicPlan,
  createTask,
  deleteTask,
  getCalendar,
  rejectProposal,
  updateTask,
} from './calendarApi'
import type { CalendarData, CalendarTask, Completion, DateRange, GameEvent, Proposal, TaskForm } from './types'

// The data of the calendar page (D120): the events and the tasks of the range on the screen, the pending proposal
// and whether the last plan is out of date (D68, D125). Only loadCalendar writes
// them; a change asks the server again for the same range, because the server decides what a change means (D3).
// They belong to the user of the session: they are forgotten when it ends or another user logs in (D103).

type LoadStatus = 'idle' | 'loading' | 'succeeded' | 'failed'

interface CalendarState {
  // The first load; after it the data stay on the page while another range or a reload arrives
  status: LoadStatus
  // The message of the last load that failed, the first one or a reload
  error: string | null
  refreshing: boolean
  // The load that is running: the answer of any older one is not used
  loadRequestId: string | null
  // The range on the screen, asked again after every change
  range: DateRange | null
  events: GameEvent[]
  tasks: CalendarTask[]
  pendingProposal: Proposal | null
  planOutdated: boolean
}

const initialState: CalendarState = {
  status: 'idle',
  error: null,
  refreshing: false,
  loadRequestId: null,
  range: null,
  events: [],
  tasks: [],
  pendingProposal: null,
  planOutdated: false,
}

const NO_SESSION_MESSAGE = 'Devi accedere per vedere il tuo calendario.'
const NO_SESSION: RequestFailure = { code: 'NO_SESSION', message: NO_SESSION_MESSAGE, status: 0 }

type LoadConfig = { state: RootState; rejectValue: string }
type ChangeConfig = { state: RootState; rejectValue: RequestFailure }

export const loadCalendar = createAsyncThunk<CalendarData, DateRange, LoadConfig>(
  'calendar/load',
  async (range, { getState, rejectWithValue }) => {
    const token = getState().auth.token
    if (!token) {
      return rejectWithValue(NO_SESSION_MESSAGE)
    }
    try {
      return await getCalendar(token, range)
    } catch (error) {
      return rejectWithValue(describeError(error).message)
    }
  },
)

/**
 * Runs a change with the token of the session and, if it worked and the session is the same, reloads the range on
 * the screen before it answers: the window closes and the toast appears when the calendar is already the new one.
 * The failure is returned as it is, so the form can show the message of every field under it.
 */
async function change<T>(
  getState: () => RootState,
  dispatch: (action: ReturnType<typeof loadCalendar>) => Promise<unknown>,
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
  // A reload that fails does not undo the change: the page keeps the data of before and says that they are old
  const { range } = getState().calendar
  if (getState().auth.token === token && range) {
    await dispatch(loadCalendar(range))
  }
  return { result }
}

export const createCalendarTask = createAsyncThunk<CalendarTask, TaskForm, ChangeConfig>(
  'calendar/createTask',
  async (form, { getState, dispatch, rejectWithValue }) => {
    const outcome = await change(getState, dispatch, (token) => createTask(token, form))
    return 'failure' in outcome ? rejectWithValue(outcome.failure) : outcome.result
  },
)

export const updateCalendarTask = createAsyncThunk<CalendarTask, { id: number; form: TaskForm }, ChangeConfig>(
  'calendar/updateTask',
  async ({ id, form }, { getState, dispatch, rejectWithValue }) => {
    const outcome = await change(getState, dispatch, (token) => updateTask(token, id, form))
    return 'failure' in outcome ? rejectWithValue(outcome.failure) : outcome.result
  },
)

export const deleteCalendarTask = createAsyncThunk<number, number, ChangeConfig>(
  'calendar/deleteTask',
  async (id, { getState, dispatch, rejectWithValue }) => {
    const outcome = await change(getState, dispatch, (token) => deleteTask(token, id))
    return 'failure' in outcome ? rejectWithValue(outcome.failure) : id
  },
)

/** Marks a task as done; a farming task adds its drops to the inventory, so the roster forgets its numbers (D118). */
export const completeCalendarTask = createAsyncThunk<Completion, number, ChangeConfig>(
  'calendar/completeTask',
  async (id, { getState, dispatch, rejectWithValue }) => {
    const outcome = await change(getState, dispatch, (token) => completeTask(token, id))
    return 'failure' in outcome ? rejectWithValue(outcome.failure) : outcome.result
  },
)

/** The basic plan without AI (D64); a plan with nothing to do answers 409 with the warning of the planner. */
export const generateBasicPlan = createAsyncThunk<Proposal, void, ChangeConfig>(
  'calendar/generateBasicPlan',
  async (_, { getState, dispatch, rejectWithValue }) => {
    const outcome = await change(getState, dispatch, (token) => createBasicPlan(token))
    return 'failure' in outcome ? rejectWithValue(outcome.failure) : outcome.result
  },
)

// A proposal that the server refuses was probably closed elsewhere (another tab, a newer plan): the range is asked
// again, so the bar shows what is really pending instead of a proposal that cannot be decided anymore
async function decide(
  getState: () => RootState,
  dispatch: (action: ReturnType<typeof loadCalendar>) => Promise<unknown>,
  request: (token: string) => Promise<Proposal>,
): Promise<{ result: Proposal } | { failure: RequestFailure }> {
  const outcome = await change(getState, dispatch, request)
  const { range } = getState().calendar
  if ('failure' in outcome && outcome.failure.code !== 'NO_SESSION' && range) {
    await dispatch(loadCalendar(range))
  }
  return outcome
}

/** The tasks of the proposal enter the calendar and replace the plan before (D22). */
export const confirmPendingProposal = createAsyncThunk<Proposal, number, ChangeConfig>(
  'calendar/confirmProposal',
  async (id, { getState, dispatch, rejectWithValue }) => {
    const outcome = await decide(getState, dispatch, (token) => confirmProposal(token, id))
    return 'failure' in outcome ? rejectWithValue(outcome.failure) : outcome.result
  },
)

export const rejectPendingProposal = createAsyncThunk<Proposal, number, ChangeConfig>(
  'calendar/rejectProposal',
  async (id, { getState, dispatch, rejectWithValue }) => {
    const outcome = await decide(getState, dispatch, (token) => rejectProposal(token, id))
    return 'failure' in outcome ? rejectWithValue(outcome.failure) : outcome.result
  },
)

const calendarSlice = createSlice({
  name: 'calendar',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      // Someone else may log in on this browser next: nothing of this user's data stays (D103)
      .addCase(sessionEnded, () => initialState)
      .addCase(login.fulfilled, () => initialState)
      .addCase(register.fulfilled, () => initialState)
      .addCase(enterAsGuest.fulfilled, () => initialState)
      .addCase(restoreSession.fulfilled, () => initialState)
      .addCase(loadCalendar.pending, (state, action) => {
        state.loadRequestId = action.meta.requestId
        state.range = action.meta.arg
        if (state.status === 'succeeded') {
          state.refreshing = true
        } else {
          state.status = 'loading'
          state.error = null
        }
      })
      .addCase(loadCalendar.fulfilled, (state, action) => {
        if (action.meta.requestId !== state.loadRequestId) {
          return
        }
        state.status = 'succeeded'
        state.error = null
        state.refreshing = false
        state.loadRequestId = null
        state.events = action.payload.events
        state.tasks = action.payload.tasks
        state.pendingProposal = action.payload.pendingProposal
        state.planOutdated = action.payload.planOutdated
      })
      .addCase(loadCalendar.rejected, (state, action) => {
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

export default calendarSlice.reducer
