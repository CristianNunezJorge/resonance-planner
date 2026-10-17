import { apiPage, apiRequest } from '../../api/client'
import type { ChatAnswer, ChatMessage, ChatPage, ChatUsage } from './types'

// The chat of the user of the token (D44, D129). The cursor goes into the address encoded, so it can never change it.

/** A page of the history from the newest message; before is the id of the oldest message already shown. */
export async function getMessages(token: string, before?: number): Promise<ChatPage> {
  const query = before === undefined ? '' : `?${new URLSearchParams({ before: String(before) }).toString()}`
  const { data, meta } = await apiPage<ChatMessage[]>(`/api/me/agent/messages${query}`, { token })
  return { messages: data, hasMore: meta.hasMore }
}

export function getUsage(token: string): Promise<ChatUsage> {
  return apiRequest<ChatUsage>('/api/me/agent/usage', { token })
}

/** Sends a message and waits for the answer of the agent, which can take a few minutes (D52). */
export function sendMessage(token: string, content: string): Promise<ChatAnswer> {
  return apiRequest<ChatAnswer>('/api/me/agent/messages', { method: 'POST', body: { content }, token })
}
