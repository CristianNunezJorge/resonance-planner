// Envelope of every backend response (D46)
export interface ApiErrorBody {
  code: string
  message: string
  fields?: Record<string, string>
}

// Only the pages of a list have it, like the history of the chat (D130)
export interface PageMeta {
  limit: number
  hasMore: boolean
}

export interface ApiResponse<T> {
  success: boolean
  data: T | null
  error: ApiErrorBody | null
  meta?: PageMeta
}
