import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// Vitest globals are off, so React Testing Library needs an explicit cleanup
afterEach(() => {
  cleanup()
})

// No test reaches a real server: the WebSocket of the notifications (D135), opened by the layout of every page, never
// connects. Assigned directly and not with vi.stubGlobal, so the tests that put their own get this one back.
class SilentWebSocket {
  onopen = null
  onmessage = null
  onclose = null
  send() {}
  close() {}
}
globalThis.WebSocket = SilentWebSocket as unknown as typeof WebSocket
