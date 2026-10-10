import { describe, expect, it } from 'vitest'
import { setupStore } from './store'
import { toastDismissed, toastShown } from './toastSlice'

describe('toastSlice', () => {
  it('adds a toast with a generated id and the success variant by default', () => {
    const store = setupStore()

    store.dispatch(toastShown('Attività segnata come fatta'))

    const [toast] = store.getState().toast.items
    expect(toast.message).toBe('Attività segnata come fatta')
    expect(toast.variant).toBe('success')
    expect(toast.id).not.toBe('')
  })

  it('removes only the dismissed toast', () => {
    const store = setupStore()
    store.dispatch(toastShown('Prima notifica'))
    store.dispatch(toastShown('Seconda notifica', 'danger'))
    const [first, second] = store.getState().toast.items

    store.dispatch(toastDismissed(first.id))

    expect(store.getState().toast.items).toEqual([second])
  })
})
