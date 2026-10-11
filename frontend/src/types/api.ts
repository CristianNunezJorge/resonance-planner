// Envelope of every backend response (D46)
export interface ApiErrorBody {
  code: string
  message: string
  fields?: Record<string, string>
}

export interface ApiResponse<T> {
  success: boolean
  data: T | null
  error: ApiErrorBody | null
}
