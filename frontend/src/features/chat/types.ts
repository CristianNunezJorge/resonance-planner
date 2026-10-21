import type { Proposal } from '../calendar/types'

// The chat with the agent (D40, D129, D130), as the API sends it

export type ChatRole = 'USER' | 'ASSISTANT'

export interface ChatMessage {
  id: number
  role: ChatRole
  content: string
  // The proposal this answer produced, if any
  proposalId: number | null
  createdAt: string
}

// The messages of today and the state of the limits of the calls to the model (D129)
export interface ChatUsage {
  used: number
  limit: number
  // Midnight in Italy, in UTC
  resetsAt: string
  callsLimitReached: boolean
  appLimitReached: boolean
  // For a guest (D134): all the guests used their share of the calls of today
  guestLimitReached: boolean
}

export interface ChatAnswer {
  message: ChatMessage
  proposal: Proposal | null
  steps: string[]
  usage: ChatUsage
}

// A page of the history, from the newest message
export interface ChatPage {
  messages: ChatMessage[]
  hasMore: boolean
}
