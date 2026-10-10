import { combineReducers, configureStore } from '@reduxjs/toolkit'
import toastReducer from './toastSlice'

const rootReducer = combineReducers({
  toast: toastReducer,
})

export type RootState = ReturnType<typeof rootReducer>

// A factory lets each test start from a fresh store
export function setupStore(preloadedState?: Partial<RootState>) {
  return configureStore({ reducer: rootReducer, preloadedState })
}

export const store = setupStore()

export type AppStore = ReturnType<typeof setupStore>
export type AppDispatch = AppStore['dispatch']
