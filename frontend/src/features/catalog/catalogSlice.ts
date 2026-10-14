import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { ApiError } from '../../api/client'
import type { RootState } from '../../app/store'
import { getCharacters, getGameRules } from './catalogApi'
import type { CharacterSummary, GameRules } from './types'

// Catalog data shared by several pages (presentation, guided setup, roster): loaded once, kept in the store

type LoadStatus = 'idle' | 'loading' | 'succeeded' | 'failed'

interface CatalogState {
  characters: CharacterSummary[]
  charactersStatus: LoadStatus
  charactersError: string | null
  // The rules of the game (levels of the ascension, limits of the Waveplate, play time options)
  rules: GameRules | null
  rulesStatus: LoadStatus
  rulesError: string | null
}

const initialState: CatalogState = {
  characters: [],
  charactersStatus: 'idle',
  charactersError: null,
  rules: null,
  rulesStatus: 'idle',
  rulesError: null,
}

const UNEXPECTED_ERROR_MESSAGE = 'Si è verificato un errore imprevisto. Riprova più tardi.'

export const fetchCharacters = createAsyncThunk<
  CharacterSummary[],
  void,
  { state: RootState; rejectValue: string }
>(
  'catalog/fetchCharacters',
  async (_, { rejectWithValue }) => {
    try {
      return await getCharacters()
    } catch (error) {
      return rejectWithValue(error instanceof ApiError ? error.message : UNEXPECTED_ERROR_MESSAGE)
    }
  },
  {
    // No second request while one is running or after the data has arrived
    condition: (_, { getState }) => {
      const status = getState().catalog.charactersStatus
      return status !== 'loading' && status !== 'succeeded'
    },
  },
)

export const fetchRules = createAsyncThunk<GameRules, void, { state: RootState; rejectValue: string }>(
  'catalog/fetchRules',
  async (_, { rejectWithValue }) => {
    try {
      return await getGameRules()
    } catch (error) {
      return rejectWithValue(error instanceof ApiError ? error.message : UNEXPECTED_ERROR_MESSAGE)
    }
  },
  {
    // No second request while one is running or after the data has arrived
    condition: (_, { getState }) => {
      const status = getState().catalog.rulesStatus
      return status !== 'loading' && status !== 'succeeded'
    },
  },
)

const catalogSlice = createSlice({
  name: 'catalog',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchCharacters.pending, (state) => {
        state.charactersStatus = 'loading'
        state.charactersError = null
      })
      .addCase(fetchCharacters.fulfilled, (state, action) => {
        state.charactersStatus = 'succeeded'
        state.characters = action.payload
      })
      .addCase(fetchCharacters.rejected, (state, action) => {
        state.charactersStatus = 'failed'
        state.charactersError = action.payload ?? UNEXPECTED_ERROR_MESSAGE
      })
      .addCase(fetchRules.pending, (state) => {
        state.rulesStatus = 'loading'
        state.rulesError = null
      })
      .addCase(fetchRules.fulfilled, (state, action) => {
        state.rulesStatus = 'succeeded'
        state.rules = action.payload
      })
      .addCase(fetchRules.rejected, (state, action) => {
        state.rulesStatus = 'failed'
        state.rulesError = action.payload ?? UNEXPECTED_ERROR_MESSAGE
      })
  },
})

export default catalogSlice.reducer
