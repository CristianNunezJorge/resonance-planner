import { combineReducers, configureStore, type ThunkAction, type UnknownAction } from '@reduxjs/toolkit'
import authReducer from '../features/auth/authSlice'
import calendarReducer from '../features/calendar/calendarSlice'
import catalogReducer from '../features/catalog/catalogSlice'
import onboardingReducer from '../features/onboarding/onboardingSlice'
import rosterReducer from '../features/roster/rosterSlice'
import toastReducer from './toastSlice'

const rootReducer = combineReducers({
  toast: toastReducer,
  catalog: catalogReducer,
  auth: authReducer,
  onboarding: onboardingReducer,
  roster: rosterReducer,
  calendar: calendarReducer,
})

export type RootState = ReturnType<typeof rootReducer>

// A factory lets each test start from a fresh store
export function setupStore(preloadedState?: Partial<RootState>) {
  // No Redux DevTools in production: the actions of login and registration carry the password and the token
  return configureStore({ reducer: rootReducer, preloadedState, devTools: import.meta.env.DEV })
}

export const store = setupStore()

export type AppStore = ReturnType<typeof setupStore>
export type AppDispatch = AppStore['dispatch']
// A function that dispatches several actions, like logout
export type AppThunk<ReturnType = void> = ThunkAction<ReturnType, RootState, unknown, UnknownAction>
