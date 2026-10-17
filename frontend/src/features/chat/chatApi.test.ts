import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiPage, apiRequest } from '../../api/client'
import { getMessages, getUsage, sendMessage } from './chatApi'

vi.mock('../../api/client', () => ({ apiPage: vi.fn(), apiRequest: vi.fn() }))

const TOKEN = 'some.jwt.token'

describe('chatApi', () => {
  afterEach(() => {
    vi.mocked(apiPage).mockReset()
    vi.mocked(apiRequest).mockReset()
  })

  it('reads the newest page of the chat, then the older ones from the oldest message shown', async () => {
    vi.mocked(apiPage).mockResolvedValue({ data: [], meta: { limit: 20, hasMore: true } })

    await expect(getMessages(TOKEN)).resolves.toEqual({ messages: [], hasMore: true })
    await getMessages(TOKEN, 88)

    expect(apiPage).toHaveBeenNthCalledWith(1, '/api/me/agent/messages', { token: TOKEN })
    expect(apiPage).toHaveBeenNthCalledWith(2, '/api/me/agent/messages?before=88', { token: TOKEN })
  })

  it('reads the usage of today and sends a message', async () => {
    vi.mocked(apiRequest).mockResolvedValue(undefined)

    await getUsage(TOKEN)
    await sendMessage(TOKEN, 'Cosa faccio oggi?')

    expect(apiRequest).toHaveBeenNthCalledWith(1, '/api/me/agent/usage', { token: TOKEN })
    expect(apiRequest).toHaveBeenNthCalledWith(2, '/api/me/agent/messages', {
      method: 'POST',
      body: { content: 'Cosa faccio oggi?' },
      token: TOKEN,
    })
  })
})
