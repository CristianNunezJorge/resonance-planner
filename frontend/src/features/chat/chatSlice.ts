import { createAsyncThunk, createSlice, type PayloadAction } from '@reduxjs/toolkit'
import { describeError, type RequestFailure } from '../../api/client'
import type { RootState } from '../../app/store'
import { enterAsGuest, login, register, restoreSession, sessionEnded } from '../auth/authSlice'
import { loadCalendar } from '../calendar/calendarSlice'
import { getMessages, getUsage, sendMessage } from './chatApi'
import type { ChatAnswer, ChatMessage, ChatPage, ChatUsage } from './types'

// The chat with the agent on the calendar page (D130): the messages on the screen from the oldest, the usage of today
// (D129), the message that waits for its answer and the text being written. It belongs to the user of the session: it
// is forgotten when the session ends or another user logs in, and the answers that arrive for an ended session are not
// used (D103).

type LoadStatus = 'idle' | 'loading' | 'succeeded' | 'failed'

interface ChatState {
  // The user the chat belongs to: a session restored for the same user (another tab) keeps it
  ownerId: number | null
  status: LoadStatus
  error: string | null
  loadRequestId: string | null
  // From the oldest to the newest
  messages: ChatMessage[]
  hasMore: boolean
  olderStatus: 'idle' | 'loading' | 'failed'
  olderRequestId: string | null
  usage: ChatUsage | null
  // The message that waits for the answer of the agent
  sending: { requestId: string; content: string } | null
  sendError: string | null
  // The text in the field, kept here so it survives the chat closing or moving to the side (D130)
  draft: string
  // The steps of the answers that arrived in this visit, by id of the answer: the server does not save them (D40)
  steps: Record<number, string[]>
}

const initialState: ChatState = {
  ownerId: null,
  status: 'idle',
  error: null,
  loadRequestId: null,
  messages: [],
  hasMore: false,
  olderStatus: 'idle',
  olderRequestId: null,
  usage: null,
  sending: null,
  sendError: null,
  draft: '',
  steps: {},
}

const NO_SESSION_MESSAGE = 'Devi accedere per scrivere all’assistente.'
const NO_OLDER_MESSAGE = 'Non ci sono messaggi precedenti.'
// No answer from the server at all (connection, gateway): it may have saved the answer anyway
const NO_ANSWER_CODES = ['NETWORK_ERROR', 'SERVICE_UNAVAILABLE', 'INTERNAL_ERROR']

export interface SendFailure extends RequestFailure {
  // The answer was found in the chat read again: the message was not lost
  recovered: boolean
}

type LoadConfig = { state: RootState; rejectValue: string }
type SendConfig = { state: RootState; rejectValue: SendFailure }

export const loadChat = createAsyncThunk<{ page: ChatPage; usage: ChatUsage; userId: number | null }, void, LoadConfig>(
  'chat/load',
  async (_, { getState, rejectWithValue }) => {
    const { token, user } = getState().auth
    if (!token) {
      return rejectWithValue(NO_SESSION_MESSAGE)
    }
    try {
      const [page, usage] = await Promise.all([getMessages(token), getUsage(token)])
      return { page, usage, userId: user?.id ?? null }
    } catch (error) {
      return rejectWithValue(describeError(error).message)
    }
  },
  // The page and the panel may ask together: one load at a time
  { condition: (_, { getState }) => getState().chat.loadRequestId === null },
)

export const loadOlderMessages = createAsyncThunk<ChatPage, void, LoadConfig>(
  'chat/loadOlder',
  async (_, { getState, rejectWithValue }) => {
    const token = getState().auth.token
    const oldest = getState().chat.messages.find((item) => item.id > 0)
    if (!token) {
      return rejectWithValue(NO_SESSION_MESSAGE)
    }
    if (!oldest) {
      return rejectWithValue(NO_OLDER_MESSAGE)
    }
    try {
      return await getMessages(token, oldest.id)
    } catch (error) {
      return rejectWithValue(describeError(error).message)
    }
  },
)

const refreshUsage = createAsyncThunk<ChatUsage, string, LoadConfig>(
  'chat/refreshUsage',
  async (token, { getState, rejectWithValue }) => {
    try {
      const usage = await getUsage(token)
      return getState().auth.token === token ? usage : rejectWithValue(NO_SESSION_MESSAGE)
    } catch (error) {
      return rejectWithValue(describeError(error).message)
    }
  },
)

// The newest messages read again end with the answer to this message, saved after the last one known before
function endsWithAnswerTo(newestFirst: ChatMessage[], content: string, lastKnownId: number): boolean {
  const [last, question] = newestFirst
  return last?.role === 'ASSISTANT' && question?.role === 'USER' && question.content === content && question.id > lastKnownId
}

/**
 * Sends a message and waits for the answer (D52). With a proposal the calendar on the screen is loaded again, so the
 * bar and the dashed tasks appear (D68). When no answer arrives at all, the chat and the usage are read again: if the
 * server saved the answer anyway, it is on the screen and the failure says so (recovered).
 */
export const sendChatMessage = createAsyncThunk<ChatAnswer, string, SendConfig>(
  'chat/send',
  async (content, { getState, dispatch, rejectWithValue }) => {
    const token = getState().auth.token
    if (!token) {
      return rejectWithValue({ code: 'NO_SESSION', message: NO_SESSION_MESSAGE, status: 0, recovered: false })
    }
    const lastKnownId = Math.max(0, ...getState().chat.messages.map((item) => item.id))
    const reloadCalendar = async () => {
      const { range } = getState().calendar
      if (getState().auth.token === token && range) {
        await dispatch(loadCalendar(range))
      }
    }
    try {
      const answer = await sendMessage(token, content)
      if (answer.proposal) {
        await reloadCalendar()
      }
      return answer
    } catch (error) {
      const failure = describeError(error)
      if (getState().auth.token !== token) {
        return rejectWithValue({ ...failure, recovered: false })
      }
      if (!NO_ANSWER_CODES.includes(failure.code)) {
        await dispatch(refreshUsage(token))
        return rejectWithValue({ ...failure, recovered: false })
      }
      const reload = await dispatch(loadChat())
      const recovered =
        loadChat.fulfilled.match(reload) && endsWithAnswerTo(reload.payload.page.messages, content, lastKnownId)
      if (recovered) {
        await reloadCalendar()
      }
      return rejectWithValue({ ...failure, recovered })
    }
  },
  // Every message costs calls of the free plan of everyone (D129): never two at the same moment, never over a limit
  { condition: (_, { getState }) => selectCanSend(getState()) },
)

/** The user can write now: the chat is loaded, no message waits and no daily limit is over (D129). */
export function selectCanSend(state: RootState): boolean {
  const { status, usage, sending } = state.chat
  return (
    status === 'succeeded' &&
    usage !== null &&
    sending === null &&
    usage.used < usage.limit &&
    !usage.callsLimitReached &&
    !usage.appLimitReached &&
    !usage.guestLimitReached
  )
}

const chatSlice = createSlice({
  name: 'chat',
  initialState,
  reducers: {
    draftChanged(state, action: PayloadAction<string>) {
      state.draft = action.payload
    },
  },
  extraReducers: (builder) => {
    builder
      // Someone else may log in on this browser next: nothing of this user's chat stays (D103)
      .addCase(sessionEnded, () => initialState)
      .addCase(login.fulfilled, () => initialState)
      .addCase(register.fulfilled, () => initialState)
      .addCase(enterAsGuest.fulfilled, () => initialState)
      // A session restored for the same user (a login in another tab) keeps the chat and the message that waits
      .addCase(restoreSession.fulfilled, (state, action) =>
        action.payload.user.id === state.ownerId ? state : initialState,
      )
      .addCase(loadChat.pending, (state, action) => {
        state.loadRequestId = action.meta.requestId
        // Read again after a lost answer: the messages stay on the screen meanwhile
        if (state.status !== 'succeeded') {
          state.status = 'loading'
          state.error = null
        }
      })
      .addCase(loadChat.fulfilled, (state, action) => {
        if (action.meta.requestId !== state.loadRequestId) {
          return
        }
        state.ownerId = action.payload.userId
        state.status = 'succeeded'
        state.error = null
        state.loadRequestId = null
        state.messages = action.payload.page.messages.toReversed()
        state.hasMore = action.payload.page.hasMore
        // An older page still on its way belongs to the list of before: it is not added to this one
        state.olderStatus = 'idle'
        state.olderRequestId = null
        state.usage = action.payload.usage
      })
      .addCase(loadChat.rejected, (state, action) => {
        if (action.meta.requestId !== state.loadRequestId) {
          return
        }
        state.loadRequestId = null
        if (state.status !== 'succeeded') {
          state.status = 'failed'
          state.error = action.payload ?? describeError(action.error).message
        }
      })
      .addCase(loadOlderMessages.pending, (state, action) => {
        state.olderStatus = 'loading'
        state.olderRequestId = action.meta.requestId
      })
      .addCase(loadOlderMessages.fulfilled, (state, action) => {
        if (action.meta.requestId !== state.olderRequestId) {
          return
        }
        state.olderStatus = 'idle'
        state.olderRequestId = null
        const shown = new Set(state.messages.map((item) => item.id))
        const older = action.payload.messages.filter((item) => !shown.has(item.id)).toReversed()
        state.messages = [...older, ...state.messages]
        state.hasMore = action.payload.hasMore
      })
      .addCase(loadOlderMessages.rejected, (state, action) => {
        if (action.meta.requestId === state.olderRequestId) {
          state.olderStatus = 'failed'
          state.olderRequestId = null
        }
      })
      .addCase(refreshUsage.fulfilled, (state, action) => {
        state.usage = action.payload
      })
      .addCase(sendChatMessage.pending, (state, action) => {
        state.sending = { requestId: action.meta.requestId, content: action.meta.arg }
        state.sendError = null
      })
      .addCase(sendChatMessage.fulfilled, (state, action) => {
        if (state.sending?.requestId !== action.meta.requestId) {
          return
        }
        const { message, steps, usage } = action.payload
        // The server answers with its message only: the message of the user is shown with the opposite id, which
        // no saved message has, until the chat is read again
        const question: ChatMessage = {
          id: -message.id,
          role: 'USER',
          content: action.meta.arg,
          proposalId: null,
          createdAt: message.createdAt,
        }
        state.messages.push(question, message)
        state.steps[message.id] = steps
        state.usage = usage
        state.sending = null
      })
      .addCase(sendChatMessage.rejected, (state, action) => {
        if (state.sending?.requestId !== action.meta.requestId) {
          return
        }
        const content = state.sending.content
        state.sending = null
        if (action.payload?.recovered) {
          state.sendError = null
          return
        }
        state.sendError = action.payload?.message ?? describeError(action.error).message
        // The message was not saved: it goes back into the field, unless the user is already writing another one
        if (state.draft === '') {
          state.draft = content
        }
      })
  },
})

export const { draftChanged } = chatSlice.actions

export default chatSlice.reducer
