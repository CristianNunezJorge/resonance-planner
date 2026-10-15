import { useDispatch, useSelector, useStore } from 'react-redux'
import type { AppDispatch, AppStore, RootState } from './store'

// Typed versions of the react-redux hooks, to use instead of the plain ones
export const useAppDispatch = useDispatch.withTypes<AppDispatch>()
export const useAppSelector = useSelector.withTypes<RootState>()
// The store itself, for an effect that must read the state as it is now, not as it was when the page was drawn
export const useAppStore = useStore.withTypes<AppStore>()
