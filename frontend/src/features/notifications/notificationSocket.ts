// The browser side of the real-time notifications (D135): a plain WebSocket with our small protocol. The browser
// cannot put the token in a header of the WebSocket, so the first message carries it (AUTH); then a PING every 25
// seconds keeps the connection alive behind Cloudflare, which closes a silent one after 100 seconds.

export type NotificationType = 'WAVEPLATE_ALMOST_FULL' | 'WAVEPLATE_FULL' | 'EVENT_ENDING' | 'EVENTS_CHANGED'

export interface ServerNotification {
  type: NotificationType
  message: string
}

export interface NotificationConnection {
  close: () => void
}

// What the footer shows (D139): the connection is open, is being opened again, or the server refused the token
export type ConnectionStatus = 'connecting' | 'connected' | 'stopped'

interface ConnectOptions {
  url: string
  token: string
  onNotification: (notification: ServerNotification) => void
  onStatus?: (status: ConnectionStatus) => void
}

const TYPES: ReadonlySet<string> = new Set<NotificationType>([
  'WAVEPLATE_ALMOST_FULL',
  'WAVEPLATE_FULL',
  'EVENT_ENDING',
  'EVENTS_CHANGED',
])
// The longest text of the server is under 200 characters (an event name has at most 120)
const MAX_MESSAGE_LENGTH = 500
const PING_EVERY_MS = 25_000
const FIRST_RETRY_MS = 1_000
const LONGEST_RETRY_MS = 60_000
// A connection that stayed open this long worked: after it the waits start again from one second. Not at the opening,
// or a connection that the server closes right after AUTH would come back every second
const STABLE_AFTER_MS = 10_000
// The server refuses a token right after AUTH: a connection still open after two seconds has been accepted (D139)
const ACCEPTED_AFTER_MS = 2_000
// The server refused the token, or the account is gone: the same token would be refused again
const TOKEN_REJECTED = 4401
const NORMAL_CLOSURE = 1000

/** 1, 2, 4… seconds, up to a minute: a sleeping free server takes a few minutes to wake up. */
export function retryDelayMs(attempt: number): number {
  return Math.min(FIRST_RETRY_MS * 2 ** attempt, LONGEST_RETRY_MS)
}

/** A notification of the server, or null for anything that is not one: the text is shown as plain text (D60). */
export function parseNotification(data: unknown): ServerNotification | null {
  if (typeof data !== 'string') {
    return null
  }
  let value: unknown
  try {
    value = JSON.parse(data)
  } catch {
    return null
  }
  if (typeof value !== 'object' || value === null) {
    return null
  }
  const { type, message } = value as Record<string, unknown>
  if (typeof type !== 'string' || !TYPES.has(type) || typeof message !== 'string') {
    return null
  }
  if (message.trim().length === 0 || message.length > MAX_MESSAGE_LENGTH) {
    return null
  }
  return { type: type as NotificationType, message }
}

/**
 * Opens the connection and keeps it open: after a fall it opens a new one, waiting longer every time, without error
 * messages (the notifications are an extra). It stops for good when the server refuses the token or the page calls
 * close().
 */
export function connectNotifications({ url, token, onNotification, onStatus }: ConnectOptions): NotificationConnection {
  let socket: WebSocket | null = null
  let pingTimer: ReturnType<typeof setInterval> | undefined
  let stableTimer: ReturnType<typeof setTimeout> | undefined
  let acceptedTimer: ReturnType<typeof setTimeout> | undefined
  let retryTimer: ReturnType<typeof setTimeout> | undefined
  let attempt = 0
  let stopped = false

  // After close() the page no longer listens: nothing is reported
  const report = (status: ConnectionStatus) => {
    if (!stopped) {
      onStatus?.(status)
    }
  }

  const open = () => {
    const current = new WebSocket(url)
    socket = current
    current.onopen = () => {
      current.send(JSON.stringify({ type: 'AUTH', token }))
      acceptedTimer = setTimeout(() => report('connected'), ACCEPTED_AFTER_MS)
      pingTimer = setInterval(() => current.send(JSON.stringify({ type: 'PING' })), PING_EVERY_MS)
      stableTimer = setTimeout(() => {
        attempt = 0
      }, STABLE_AFTER_MS)
    }
    current.onmessage = (event: MessageEvent) => {
      // A message already on its way when the page closed the connection belongs to a session that is over
      if (stopped) {
        return
      }
      const notification = parseNotification(event.data)
      if (notification) {
        onNotification(notification)
      }
    }
    current.onclose = (event: CloseEvent) => {
      clearInterval(pingTimer)
      clearTimeout(stableTimer)
      clearTimeout(acceptedTimer)
      if (stopped) {
        return
      }
      if (event.code === TOKEN_REJECTED) {
        report('stopped')
        return
      }
      report('connecting')
      retryTimer = setTimeout(open, retryDelayMs(attempt))
      attempt += 1
    }
  }

  open()

  return {
    close: () => {
      stopped = true
      clearInterval(pingTimer)
      clearTimeout(stableTimer)
      clearTimeout(acceptedTimer)
      clearTimeout(retryTimer)
      socket?.close(NORMAL_CLOSURE)
    },
  }
}
