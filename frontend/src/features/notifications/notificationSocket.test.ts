import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { connectNotifications, parseNotification, retryDelayMs, type ServerNotification } from './notificationSocket'

// The browser side of our small protocol (D135): AUTH, then PING every 25 seconds; notifications from the server

class FakeSocket {
  static instances: FakeSocket[] = []
  readonly url: string
  readonly sent: string[] = []
  closedWith: number | null = null
  onopen: (() => void) | null = null
  onmessage: ((event: { data: unknown }) => void) | null = null
  onclose: ((event: { code: number }) => void) | null = null

  constructor(url: string) {
    this.url = url
    FakeSocket.instances.push(this)
  }

  send(data: string) {
    this.sent.push(data)
  }

  close(code?: number) {
    this.closedWith = code ?? 1005
  }

  // What the server does
  open() {
    this.onopen?.()
  }

  receive(data: unknown) {
    this.onmessage?.({ data })
  }

  drop(code: number) {
    this.onclose?.({ code })
  }
}

const URL = 'wss://api.example.com/ws'
const AUTH = '{"type":"AUTH","token":"the.token"}'
const PING = '{"type":"PING"}'

function lastSocket(): FakeSocket {
  return FakeSocket.instances[FakeSocket.instances.length - 1]
}

describe('connectNotifications', () => {
  const received: ServerNotification[] = []

  beforeEach(() => {
    vi.useFakeTimers()
    vi.stubGlobal('WebSocket', FakeSocket)
    FakeSocket.instances = []
    received.length = 0
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  function connect() {
    return connectNotifications({ url: URL, token: 'the.token', onNotification: (n) => received.push(n) })
  }

  it('sends the token as the first message, then a PING every 25 seconds', () => {
    connect()
    const socket = lastSocket()
    expect(socket.url).toBe(URL)
    expect(socket.sent).toEqual([])

    socket.open()
    expect(socket.sent).toEqual([AUTH])

    vi.advanceTimersByTime(25_000)
    vi.advanceTimersByTime(25_000)
    expect(socket.sent).toEqual([AUTH, PING, PING])
  })

  it('passes on the notifications of the server and ignores anything else', () => {
    connect()
    const socket = lastSocket()
    socket.open()

    socket.receive('{"type":"EVENT_ENDING","message":"Ultimo giorno di «Bountiful Crescendo»."}')
    socket.receive('not json')
    socket.receive('{"type":"SOMETHING_ELSE","message":"?"}')

    expect(received).toEqual([{ type: 'EVENT_ENDING', message: 'Ultimo giorno di «Bountiful Crescendo».' }])
  })

  it('opens a new connection after a fall, waiting a little longer every time', () => {
    connect()
    lastSocket().drop(1006)
    expect(FakeSocket.instances).toHaveLength(1)

    vi.advanceTimersByTime(1_000)
    expect(FakeSocket.instances).toHaveLength(2)

    lastSocket().drop(1006)
    vi.advanceTimersByTime(1_999)
    expect(FakeSocket.instances).toHaveLength(2)
    vi.advanceTimersByTime(1)
    expect(FakeSocket.instances).toHaveLength(3)
  })

  it('starts again from the shortest wait after a connection that stayed open', () => {
    connect()
    lastSocket().drop(1006)
    vi.advanceTimersByTime(1_000)
    lastSocket().drop(1006)
    vi.advanceTimersByTime(2_000)

    lastSocket().open()
    vi.advanceTimersByTime(10_000)
    lastSocket().drop(1001)
    vi.advanceTimersByTime(1_000)

    expect(FakeSocket.instances).toHaveLength(4)
  })

  it('keeps waiting longer when the server closes the connection right after the token', () => {
    connect()
    lastSocket().open()
    lastSocket().drop(1008)
    vi.advanceTimersByTime(1_000)
    expect(FakeSocket.instances).toHaveLength(2)

    // Without this, a connection refused after every AUTH would come back every second
    lastSocket().open()
    lastSocket().drop(1008)
    vi.advanceTimersByTime(1_999)
    expect(FakeSocket.instances).toHaveLength(2)
    vi.advanceTimersByTime(1)
    expect(FakeSocket.instances).toHaveLength(3)
  })

  it('ignores what arrives after the page closed the connection', () => {
    const connection = connect()
    const socket = lastSocket()
    socket.open()

    connection.close()
    socket.receive('{"type":"EVENTS_CHANGED","message":"Gli eventi sono cambiati."}')

    expect(received).toEqual([])
  })

  it('stops the PING when the connection falls', () => {
    connect()
    const socket = lastSocket()
    socket.open()
    socket.drop(1006)

    vi.advanceTimersByTime(25_000)

    expect(socket.sent).toEqual([AUTH])
  })

  it('does not try again when the server refuses the token', () => {
    connect()
    lastSocket().open()
    lastSocket().drop(4401)

    vi.advanceTimersByTime(120_000)

    expect(FakeSocket.instances).toHaveLength(1)
  })

  it('closes for good when the page asks', () => {
    const connection = connect()
    const socket = lastSocket()
    socket.open()

    connection.close()
    vi.advanceTimersByTime(120_000)

    expect(socket.closedWith).toBe(1000)
    expect(socket.sent).toEqual([AUTH])
    expect(FakeSocket.instances).toHaveLength(1)
  })

  it('does not open the connection planned after a fall once the page closed it', () => {
    const connection = connect()
    lastSocket().drop(1006)

    connection.close()
    vi.advanceTimersByTime(60_000)

    expect(FakeSocket.instances).toHaveLength(1)
  })
})

describe('retryDelayMs', () => {
  it('doubles the wait from one second up to one minute', () => {
    expect([0, 1, 2, 3, 4, 5, 6, 7, 20].map(retryDelayMs)).toEqual([
      1_000, 2_000, 4_000, 8_000, 16_000, 32_000, 60_000, 60_000, 60_000,
    ])
  })
})

describe('parseNotification', () => {
  it('reads the notifications the server can send', () => {
    for (const type of ['WAVEPLATE_ALMOST_FULL', 'WAVEPLATE_FULL', 'EVENT_ENDING', 'EVENTS_CHANGED']) {
      expect(parseNotification(JSON.stringify({ type, message: 'Testo.' }))).toEqual({ type, message: 'Testo.' })
    }
  })

  it('refuses anything else', () => {
    expect(parseNotification('{"type":"EVENT_ENDING"}')).toBeNull()
    expect(parseNotification('{"type":"EVENT_ENDING","message":""}')).toBeNull()
    expect(parseNotification('{"type":"EVENT_ENDING","message":"   "}')).toBeNull()
    expect(parseNotification('{"type":"EVENT_ENDING","message":42}')).toBeNull()
    expect(parseNotification(JSON.stringify({ type: 'EVENT_ENDING', message: 'x'.repeat(501) }))).toBeNull()
    expect(parseNotification('{"type":"toString","message":"Testo."}')).toBeNull()
    expect(parseNotification('null')).toBeNull()
    expect(parseNotification('[1]')).toBeNull()
    expect(parseNotification('{')).toBeNull()
    expect(parseNotification(new ArrayBuffer(4))).toBeNull()
  })
})
