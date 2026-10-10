import { createSlice, nanoid, type PayloadAction } from '@reduxjs/toolkit'

// Short notifications that confirm an action (D69), shown by any feature
export type ToastVariant = 'success' | 'danger' | 'info'

export interface Toast {
  id: string
  message: string
  variant: ToastVariant
}

interface ToastState {
  items: Toast[]
}

const initialState: ToastState = {
  items: [],
}

const toastSlice = createSlice({
  name: 'toast',
  initialState,
  reducers: {
    toastShown: {
      reducer(state, action: PayloadAction<Toast>) {
        state.items = [...state.items, action.payload]
      },
      prepare(message: string, variant: ToastVariant = 'success') {
        return { payload: { id: nanoid(), message, variant } }
      },
    },
    toastDismissed(state, action: PayloadAction<string>) {
      state.items = state.items.filter((toast) => toast.id !== action.payload)
    },
  },
})

export const { toastShown, toastDismissed } = toastSlice.actions
export default toastSlice.reducer
