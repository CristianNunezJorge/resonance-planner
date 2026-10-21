import { createAsyncThunk, createSlice, type PayloadAction } from '@reduxjs/toolkit'
import { describeError, type RequestFailure } from '../../api/client'
import { toastShown } from '../../app/toastSlice'
import type { AppThunk, RootState } from '../../app/store'
import { getProfile, login as loginRequest, register as registerRequest, startGuest } from './authApi'
import { clearToken, readToken, saveToken } from './tokenStorage'
import type { AuthResponse, LoginRequest, RegisterRequest, User } from './types'

// Who is logged in (D45). The token is also kept in the localStorage; the user is read again from the
// server at every start, because the server is the only one that knows if the account still exists.

// idle: nothing was asked yet · restoring: asking the server for the saved session ·
// ready: we know who is logged in (or that nobody is) · failed: the server could not be reached
type SessionStatus = 'idle' | 'restoring' | 'ready' | 'failed'

interface AuthState {
  token: string | null
  user: User | null
  sessionStatus: SessionStatus
  sessionError: string | null
}

const initialState: AuthState = {
  token: null,
  user: null,
  sessionStatus: 'idle',
  sessionError: null,
}

const UNAUTHORIZED = 401
// The saved token disappeared (another tab logged out) before the request started
const NO_SESSION = 'NO_SESSION'
const SESSION_EXPIRED_MESSAGE = 'La sessione è scaduta: accedi di nuovo.'

type AuthThunkConfig = { rejectValue: RequestFailure }

export const login = createAsyncThunk<AuthResponse, LoginRequest, AuthThunkConfig>(
  'auth/login',
  async (credentials, { rejectWithValue }) => {
    try {
      const response = await loginRequest(credentials)
      saveToken(response.token)
      return response
    } catch (error) {
      return rejectWithValue(describeError(error))
    }
  },
)

export const register = createAsyncThunk<AuthResponse, RegisterRequest, AuthThunkConfig>(
  'auth/register',
  async (data, { rejectWithValue }) => {
    try {
      const response = await registerRequest(data)
      saveToken(response.token)
      return response
    } catch (error) {
      return rejectWithValue(describeError(error))
    }
  },
)

// "Prova la demo" (D134): the server creates a temporary account, and the session opens like after a login
export const enterAsGuest = createAsyncThunk<AuthResponse, void, AuthThunkConfig>(
  'auth/enterAsGuest',
  async (_, { rejectWithValue }) => {
    try {
      const response = await startGuest()
      saveToken(response.token)
      return response
    } catch (error) {
      return rejectWithValue(describeError(error))
    }
  },
)

// At every start: if a token was saved, ask the server who it belongs to
export const restoreSession = createAsyncThunk<
  AuthResponse,
  void,
  { state: RootState; rejectValue: RequestFailure }
>(
  'auth/restoreSession',
  async (_, { dispatch, rejectWithValue }) => {
    const token = readToken()
    if (!token) {
      return rejectWithValue({ code: NO_SESSION, message: '', status: 0 })
    }
    try {
      return { token, user: await getProfile(token) }
    } catch (error) {
      const failure = describeError(error)
      if (failure.status === UNAUTHORIZED) {
        // The saved token expired, or its account was deleted since the last visit. Only this token is removed:
        // another tab may have saved a new one in the meantime
        clearToken(token)
        dispatch(toastShown(SESSION_EXPIRED_MESSAGE, 'info'))
      }
      return rejectWithValue(failure)
    }
  },
  {
    // Without a saved token there is nothing to do, and no second request while one is running
    condition: (_, { getState }) => readToken() !== null && getState().auth.sessionStatus !== 'restoring',
  },
)

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    sessionEnded(state) {
      state.token = null
      state.user = null
      state.sessionStatus = 'ready'
      state.sessionError = null
    },
    // New data of the user who is logged in (after the guided setup). An update for someone else is a late answer of
    // a session that already ended: it must not bring that user back
    userUpdated(state, action: PayloadAction<User>) {
      if (state.user?.id === action.payload.id) {
        state.user = action.payload
      }
    },
  },
  extraReducers: (builder) => {
    const signIn = (state: AuthState, action: PayloadAction<AuthResponse>) => {
      state.token = action.payload.token
      state.user = action.payload.user
      state.sessionStatus = 'ready'
      state.sessionError = null
    }
    builder
      .addCase(login.fulfilled, signIn)
      .addCase(register.fulfilled, signIn)
      .addCase(enterAsGuest.fulfilled, signIn)
      .addCase(restoreSession.pending, (state) => {
        state.sessionStatus = 'restoring'
        state.sessionError = null
      })
      .addCase(restoreSession.fulfilled, signIn)
      .addCase(restoreSession.rejected, (state, action) => {
        if (action.payload?.status === UNAUTHORIZED || action.payload?.code === NO_SESSION) {
          // The token expired, its account is gone, or it disappeared meanwhile: nobody is logged in
          state.token = null
          state.sessionStatus = 'ready'
          return
        }
        // The server could not be reached: the token stays, the user can try again
        state.sessionStatus = 'failed'
        state.sessionError = action.payload?.message ?? 'Impossibile ripristinare la sessione.'
      })
  },
})

export const { sessionEnded, userUpdated } = authSlice.actions

/** Logout (F11): the token disappears from the browser and from the memory. */
export const logout = (): AppThunk => (dispatch) => {
  clearToken()
  dispatch(sessionEnded())
}

/** Try again to restore the saved session; if the token is gone meanwhile (another tab logged out), nobody is logged in. */
export const retrySession = (): AppThunk => (dispatch) => {
  if (readToken() === null) {
    dispatch(sessionEnded())
    return
  }
  void dispatch(restoreSession())
}

/**
 * The server refused the token (401): end the session and tell the user why. It acts only if the refused token is
 * still the one of this session: a late answer for a token of a session that already ended must not log out the next one.
 */
export const expireSession =
  (refusedToken: string): AppThunk =>
  (dispatch, getState) => {
    if (getState().auth.token !== refusedToken) {
      return
    }
    clearToken(refusedToken)
    dispatch(sessionEnded())
    dispatch(toastShown(SESSION_EXPIRED_MESSAGE, 'info'))
  }

export default authSlice.reducer
