import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client'
import { setupStore } from '../../app/store'
import { rover, signedIn } from '../../test/utils'
import { restoreSession, sessionEnded } from '../auth/authSlice'
import { getCalendar } from '../calendar/calendarApi'
import { loadCalendar } from '../calendar/calendarSlice'
import type { CalendarData, Proposal } from '../calendar/types'
import { getMessages, getUsage, sendMessage } from './chatApi'
import { draftChanged, loadChat, loadOlderMessages, selectCanSend, sendChatMessage } from './chatSlice'
import type { ChatAnswer, ChatMessage, ChatUsage } from './types'

vi.mock('./chatApi', () => ({ getMessages: vi.fn(), getUsage: vi.fn(), sendMessage: vi.fn() }))
vi.mock('../calendar/calendarApi', () => ({
  getCalendar: vi.fn(),
  createTask: vi.fn(),
  updateTask: vi.fn(),
  deleteTask: vi.fn(),
  completeTask: vi.fn(),
  createBasicPlan: vi.fn(),
  confirmProposal: vi.fn(),
  rejectProposal: vi.fn(),
}))

const TOKEN = 'test.jwt.token'
const WEEK = { from: '2026-10-12', to: '2026-10-18' }

function message(id: number, role: ChatMessage['role'], content: string): ChatMessage {
  return { id, role, content, proposalId: null, createdAt: '2026-10-17T12:00:00Z' }
}

const usage: ChatUsage = {
  used: 1,
  limit: 5,
  resetsAt: '2026-10-17T22:00:00Z',
  callsLimitReached: false,
  appLimitReached: false,
  guestLimitReached: false,
}

// From the newest, like the API
const newest = [message(4, 'ASSISTANT', 'Ciao!'), message(3, 'USER', 'Ciao')]

const proposal: Proposal = { id: 9, status: 'PENDING', summary: 'Tre giorni.', createdAt: '2026-10-17T12:01:00Z', tasks: [] }

function answer(overrides: Partial<ChatAnswer> = {}): ChatAnswer {
  return {
    message: message(6, 'ASSISTANT', 'Ecco il piano.'),
    proposal: null,
    steps: ['Ho calcolato il piano'],
    usage: { ...usage, used: 2 },
    ...overrides,
  }
}

const emptyWeek: CalendarData = { events: [], tasks: [], pendingProposal: null, planOutdated: false }

async function loadedStore() {
  const store = setupStore(signedIn())
  await store.dispatch(loadChat())
  return store
}

describe('chatSlice', () => {
  beforeEach(() => {
    vi.mocked(getMessages).mockResolvedValue({ messages: newest, hasMore: true })
    vi.mocked(getUsage).mockResolvedValue(usage)
    vi.mocked(getCalendar).mockResolvedValue(emptyWeek)
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  it('loads the last page of the chat, from the oldest message, with the usage of today', async () => {
    const store = await loadedStore()

    const chat = store.getState().chat
    expect(getMessages).toHaveBeenCalledWith(TOKEN)
    expect(chat.status).toBe('succeeded')
    expect(chat.messages.map((item) => item.id)).toEqual([3, 4])
    expect(chat.hasMore).toBe(true)
    expect(chat.usage).toEqual(usage)
    expect(selectCanSend(store.getState())).toBe(true)
  })

  it('keeps the message of a first load that fails', async () => {
    vi.mocked(getMessages).mockRejectedValue(new ApiError('NETWORK_ERROR', 'Impossibile contattare il server.', 0))
    const store = setupStore(signedIn())

    await store.dispatch(loadChat())

    expect(store.getState().chat.status).toBe('failed')
    expect(store.getState().chat.error).toBe('Impossibile contattare il server.')
    expect(selectCanSend(store.getState())).toBe(false)
  })

  it('adds the older messages before the ones on the screen', async () => {
    const store = await loadedStore()
    vi.mocked(getMessages).mockResolvedValue({ messages: [message(2, 'ASSISTANT', 'Prima'), message(1, 'USER', 'Ehi')], hasMore: false })

    await store.dispatch(loadOlderMessages())

    expect(getMessages).toHaveBeenLastCalledWith(TOKEN, 3)
    expect(store.getState().chat.messages.map((item) => item.id)).toEqual([1, 2, 3, 4])
    expect(store.getState().chat.hasMore).toBe(false)
  })

  it('shows the message that waits for the answer, then adds both with the steps and the new usage', async () => {
    const store = await loadedStore()
    let reply: (value: ChatAnswer) => void = () => {}
    vi.mocked(sendMessage).mockReturnValue(new Promise((resolve) => (reply = resolve)))

    const sending = store.dispatch(sendChatMessage('Organizzami la settimana'))

    expect(store.getState().chat.sending?.content).toBe('Organizzami la settimana')
    expect(selectCanSend(store.getState())).toBe(false)
    reply(answer())
    await sending
    const chat = store.getState().chat
    expect(sendMessage).toHaveBeenCalledWith(TOKEN, 'Organizzami la settimana')
    expect(chat.sending).toBeNull()
    expect(chat.messages.map((item) => [item.role, item.content])).toEqual([
      ['USER', 'Ciao'],
      ['ASSISTANT', 'Ciao!'],
      ['USER', 'Organizzami la settimana'],
      ['ASSISTANT', 'Ecco il piano.'],
    ])
    expect(chat.steps[6]).toEqual(['Ho calcolato il piano'])
    expect(chat.usage?.used).toBe(2)
    expect(getCalendar).not.toHaveBeenCalled()
  })

  it('loads the calendar again when the answer brings a proposal', async () => {
    const store = await loadedStore()
    await store.dispatch(loadCalendar(WEEK))
    vi.mocked(getCalendar).mockClear()
    vi.mocked(sendMessage).mockResolvedValue(answer({ proposal }))

    await store.dispatch(sendChatMessage('Organizzami la settimana'))

    expect(getCalendar).toHaveBeenCalledWith(TOKEN, WEEK)
  })

  it('keeps the message of the server and asks the usage again when the agent fails', async () => {
    const store = await loadedStore()
    vi.mocked(getUsage).mockResolvedValue({ ...usage, callsLimitReached: true })
    vi.mocked(sendMessage).mockRejectedValue(
      new ApiError('AGENT_UNAVAILABLE', 'L’assistente non è disponibile in questo momento.', 502),
    )

    const action = await store.dispatch(sendChatMessage('Ciao'))

    expect(sendChatMessage.rejected.match(action) && action.payload?.recovered).toBe(false)
    const chat = store.getState().chat
    expect(chat.sendError).toBe('L’assistente non è disponibile in questo momento.')
    expect(chat.messages).toHaveLength(2)
    expect(chat.usage?.callsLimitReached).toBe(true)
    expect(getMessages).toHaveBeenCalledTimes(1)
  })

  it('reads the chat again when no answer arrived, and finds the answer the server saved anyway', async () => {
    const store = await loadedStore()
    vi.mocked(sendMessage).mockRejectedValue(new ApiError('NETWORK_ERROR', 'Impossibile contattare il server.', 0))
    vi.mocked(getMessages).mockResolvedValue({
      messages: [message(6, 'ASSISTANT', 'Eccomi.'), message(5, 'USER', 'Cosa faccio oggi?'), ...newest],
      hasMore: false,
    })

    const action = await store.dispatch(sendChatMessage('Cosa faccio oggi?'))

    expect(sendChatMessage.rejected.match(action) && action.payload?.recovered).toBe(true)
    const chat = store.getState().chat
    expect(chat.sendError).toBeNull()
    expect(chat.messages.map((item) => item.id)).toEqual([3, 4, 5, 6])
  })

  it('reads the chat again when no answer arrived, and keeps the error when nothing was saved', async () => {
    const store = await loadedStore()
    vi.mocked(sendMessage).mockRejectedValue(new ApiError('SERVICE_UNAVAILABLE', 'Il server non è ancora pronto.', 503))

    const action = await store.dispatch(sendChatMessage('Ciao'))

    expect(sendChatMessage.rejected.match(action) && action.payload?.recovered).toBe(false)
    expect(getMessages).toHaveBeenCalledTimes(2)
    expect(store.getState().chat.sendError).toBe('Il server non è ancora pronto.')
  })

  it('forgets everything when the session ends, also an answer that arrives later', async () => {
    const store = await loadedStore()
    let reply: (value: ChatAnswer) => void = () => {}
    vi.mocked(sendMessage).mockReturnValue(new Promise((resolve) => (reply = resolve)))
    const sending = store.dispatch(sendChatMessage('Ciao'))

    store.dispatch(sessionEnded())
    reply(answer())
    await sending

    const chat = store.getState().chat
    expect(chat.status).toBe('idle')
    expect(chat.messages).toEqual([])
    expect(chat.sending).toBeNull()
    expect(chat.usage).toBeNull()
  })

  it('keeps the chat when the session of the same user is restored, and forgets it for another user', async () => {
    const store = await loadedStore()
    store.dispatch(draftChanged('Una bozza'))

    store.dispatch(restoreSession.fulfilled({ token: 'new.token', user: rover }, 'restore-1'))
    expect(store.getState().chat.messages).toHaveLength(2)
    expect(store.getState().chat.draft).toBe('Una bozza')

    store.dispatch(restoreSession.fulfilled({ token: 'other.token', user: { ...rover, id: 99 } }, 'restore-2'))
    expect(store.getState().chat.messages).toEqual([])
    expect(store.getState().chat.draft).toBe('')
  })

  it('never sends two messages at the same moment', async () => {
    const store = await loadedStore()
    vi.mocked(sendMessage).mockReturnValue(new Promise(() => {}))

    void store.dispatch(sendChatMessage('Primo'))
    void store.dispatch(sendChatMessage('Secondo'))

    expect(sendMessage).toHaveBeenCalledTimes(1)
    expect(store.getState().chat.sending?.content).toBe('Primo')
  })

  it('asks the chat only once when the page and the panel load it together', async () => {
    const store = setupStore(signedIn())

    await Promise.all([store.dispatch(loadChat()), store.dispatch(loadChat())])

    expect(getMessages).toHaveBeenCalledTimes(1)
    expect(getUsage).toHaveBeenCalledTimes(1)
  })

  it('does not add an older page that arrives after the chat was read again', async () => {
    const store = await loadedStore()
    let older: (value: { messages: ChatMessage[]; hasMore: boolean }) => void = () => {}
    vi.mocked(getMessages).mockReturnValueOnce(new Promise((resolve) => (older = resolve)))
    const loadingOlder = store.dispatch(loadOlderMessages())
    vi.mocked(getMessages).mockResolvedValue({ messages: newest, hasMore: true })

    await store.dispatch(loadChat())
    older({ messages: [message(2, 'ASSISTANT', 'Prima')], hasMore: false })
    await loadingOlder

    expect(store.getState().chat.messages.map((item) => item.id)).toEqual([3, 4])
  })

  it('puts a failed message back into the field, unless the user is already writing another one', async () => {
    const store = await loadedStore()
    vi.mocked(sendMessage).mockRejectedValue(new ApiError('AGENT_UNAVAILABLE', 'Non disponibile.', 502))

    await store.dispatch(sendChatMessage('Prima domanda'))
    expect(store.getState().chat.draft).toBe('Prima domanda')

    store.dispatch(draftChanged(''))
    let fail: (reason: unknown) => void = () => {}
    vi.mocked(sendMessage).mockReturnValue(new Promise((_, reject) => (fail = reject)))
    const sending = store.dispatch(sendChatMessage('Seconda domanda'))
    store.dispatch(draftChanged('Sto scrivendo'))
    fail(new ApiError('AGENT_UNAVAILABLE', 'Non disponibile.', 502))
    await sending

    expect(store.getState().chat.draft).toBe('Sto scrivendo')
  })

  it('does not let the user write when a daily limit is over', async () => {
    vi.mocked(getUsage).mockResolvedValue({ ...usage, used: 5 })
    const store = await loadedStore()

    expect(selectCanSend(store.getState())).toBe(false)
  })

  it('does not let a guest write when the guests used their share of the calls (D134)', async () => {
    vi.mocked(getUsage).mockResolvedValue({ ...usage, limit: 2, guestLimitReached: true })
    const store = await loadedStore()

    expect(selectCanSend(store.getState())).toBe(false)
  })
})
