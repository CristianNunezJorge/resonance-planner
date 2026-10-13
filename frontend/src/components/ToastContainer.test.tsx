import { act, fireEvent, render, screen } from '@testing-library/react'
import { Provider } from 'react-redux'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { setupStore } from '../app/store'
import { toastShown } from '../app/toastSlice'
import ToastContainer from './ToastContainer'

function renderContainer() {
  const store = setupStore()
  render(
    <Provider store={store}>
      <ToastContainer />
    </Provider>,
  )
  return store
}

const advance = (ms: number) =>
  act(() => {
    vi.advanceTimersByTime(ms)
  })

describe('ToastContainer', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('has a polite area from the start, empty: a screen reader announces what is added to it', () => {
    renderContainer()

    const area = screen.getByRole('status')
    expect(area).toBeEmptyDOMElement()
    expect(area).toHaveAttribute('aria-live', 'polite')
    expect(area).toHaveAttribute('aria-atomic', 'false')
  })

  it('shows the notifications of the store inside that area', () => {
    const store = renderContainer()

    act(() => {
      store.dispatch(toastShown('Sei uscito dall\'account.', 'info'))
    })

    expect(screen.getByRole('status')).toHaveTextContent('Sei uscito dall\'account.')
  })

  it('announces an error at once, as an alert of its own, outside the polite area', () => {
    const store = renderContainer()

    act(() => {
      store.dispatch(toastShown('Area riservata.', 'danger'))
    })

    expect(screen.getByRole('alert')).toHaveTextContent('Area riservata.')
    expect(screen.getByRole('status')).not.toHaveTextContent('Area riservata.')
  })

  it('closes a notification with its button', () => {
    const store = renderContainer()
    act(() => {
      store.dispatch(toastShown('Fatto.'))
    })

    fireEvent.click(screen.getByRole('button', { name: 'Chiudi' }))

    expect(screen.queryByText('Fatto.')).not.toBeInTheDocument()
    expect(store.getState().toast.items).toHaveLength(0)
  })

  it('closes a notification by itself after a few seconds', () => {
    const store = renderContainer()
    act(() => {
      store.dispatch(toastShown('Fatto.'))
    })

    advance(6000)

    expect(screen.queryByText('Fatto.')).not.toBeInTheDocument()
    expect(store.getState().toast.items).toHaveLength(0)
  })

  it('keeps an error longer than the other messages', () => {
    const store = renderContainer()
    act(() => {
      store.dispatch(toastShown('Area riservata.', 'danger'))
    })

    advance(6000)
    expect(screen.getByText('Area riservata.')).toBeInTheDocument()

    advance(4000)
    expect(screen.queryByText('Area riservata.')).not.toBeInTheDocument()
  })

  it('waits while the mouse is over the notification, and counts again when it leaves', () => {
    const store = renderContainer()
    act(() => {
      store.dispatch(toastShown('Fatto.'))
    })
    const toast = screen.getByText('Fatto.').closest('.toast') as HTMLElement

    fireEvent.mouseEnter(toast)
    advance(60000)
    expect(screen.getByText('Fatto.')).toBeInTheDocument()

    fireEvent.mouseLeave(toast)
    advance(6000)
    expect(screen.queryByText('Fatto.')).not.toBeInTheDocument()
  })

  it('waits while the keyboard is on its button too', () => {
    const store = renderContainer()
    act(() => {
      store.dispatch(toastShown('Fatto.'))
    })

    fireEvent.focus(screen.getByRole('button', { name: 'Chiudi' }))
    advance(60000)

    expect(screen.getByText('Fatto.')).toBeInTheDocument()
  })

  it('keeps the other notifications when one closes', () => {
    const store = renderContainer()
    act(() => {
      store.dispatch(toastShown('Prima.'))
    })
    advance(3000)
    act(() => {
      store.dispatch(toastShown('Seconda.'))
    })

    advance(3000)

    expect(screen.queryByText('Prima.')).not.toBeInTheDocument()
    expect(screen.getByText('Seconda.')).toBeInTheDocument()
  })

  it('uses the light theme for the info notification, so its close icon is dark on the light blue', () => {
    const store = renderContainer()
    act(() => {
      store.dispatch(toastShown('Informazione.', 'info'))
      store.dispatch(toastShown('Fatto.', 'success'))
    })

    expect(screen.getByText('Informazione.').closest('.toast')).toHaveAttribute('data-bs-theme', 'light')
    expect(screen.getByText('Fatto.').closest('.toast')).toHaveAttribute('data-bs-theme', 'dark')
  })
})
