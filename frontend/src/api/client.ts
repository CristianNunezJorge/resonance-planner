import type { ApiResponse, PageMeta } from '../types/api'

// The single HTTP client of the app (D76): it adds the token, opens the ApiResponse envelope
// and turns every failure into an ApiError with a message in Italian.

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

export interface RequestOptions {
  method?: HttpMethod
  body?: unknown
  token?: string | null
  signal?: AbortSignal
}

const NETWORK_ERROR_MESSAGE = 'Impossibile contattare il server. Controlla la connessione e riprova.'
const INVALID_REQUEST_MESSAGE = 'Richiesta non valida.'
const UNEXPECTED_ERROR_MESSAGE = 'Si è verificato un errore imprevisto. Riprova più tardi.'
const NOT_READY_MESSAGE = 'Il server non è ancora pronto: riprova tra qualche istante.'
const NO_CONTENT = 204
const UNAUTHORIZED = 401
// Answers of Render's gateway while the free backend is still waking up (D82)
const GATEWAY_STATUSES = [502, 503, 504]

export class ApiError extends Error {
  readonly code: string
  readonly status: number
  readonly fields?: Record<string, string>

  constructor(code: string, message: string, status: number, fields?: Record<string, string>, cause?: unknown) {
    super(message, { cause })
    this.name = 'ApiError'
    this.code = code
    this.status = status
    this.fields = fields
  }
}

// What a form or a page needs to show about a failed request
export interface RequestFailure {
  code: string
  message: string
  status: number
  fields?: Record<string, string>
}

// Called when a request that carried a token is answered with 401: the token has expired or its
// account is gone (D45, D102). The store registers it, so the client does not depend on Redux.
// It receives the refused token, so a late answer for an old session cannot end the next one.
let unauthorizedHandler: ((refusedToken: string) => void) | null = null

export function setUnauthorizedHandler(handler: ((refusedToken: string) => void) | null): void {
  unauthorizedHandler = handler
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const envelope = await requestEnvelope<T>(path, options)
  return envelope?.data as T
}

/** One page of a list (D46, D130): the data with the meta part, which says whether more items follow. */
export async function apiPage<T>(path: string, options: RequestOptions = {}): Promise<{ data: T; meta: PageMeta }> {
  const envelope = await requestEnvelope<T>(path, options)
  if (!envelope?.meta) {
    throw new ApiError('INTERNAL_ERROR', UNEXPECTED_ERROR_MESSAGE, 200)
  }
  return { data: envelope.data as T, meta: envelope.meta }
}

// The envelope of a successful answer, or null for 204; every failure becomes an ApiError
async function requestEnvelope<T>(path: string, options: RequestOptions): Promise<ApiResponse<T> | null> {
  const { method = 'GET', body, token, signal } = options
  // The token goes only to our backend: a path like //other.site would change the destination
  if (!path.startsWith('/') || path.startsWith('//')) {
    throw new Error(`Invalid API path: ${path}`)
  }
  const response = await send(path, method, buildHeaders(body, token), body, signal)
  // Also when the answer has no ApiResponse body (a gateway, a firewall): the token is refused all the same
  if (token && response.status === UNAUTHORIZED) {
    unauthorizedHandler?.(token)
  }
  if (response.status === NO_CONTENT) {
    return null
  }
  const envelope = await readEnvelope<T>(response, signal)
  if (!envelope) {
    throw errorWithoutEnvelope(response.status)
  }
  if (!envelope.success || envelope.error) {
    const error = envelope.error
    throw new ApiError(
      error?.code ?? 'INTERNAL_ERROR',
      error?.message ?? UNEXPECTED_ERROR_MESSAGE,
      response.status,
      error?.fields,
    )
  }
  return envelope
}

/** Turns anything thrown by apiRequest into a plain object that can travel in a Redux action. */
export function describeError(error: unknown): RequestFailure {
  if (error instanceof ApiError) {
    return { code: error.code, message: error.message, status: error.status, fields: error.fields }
  }
  return { code: 'INTERNAL_ERROR', message: UNEXPECTED_ERROR_MESSAGE, status: 0 }
}

function buildHeaders(body: unknown, token: string | null | undefined): Headers {
  const headers = new Headers({ Accept: 'application/json' })
  if (body !== undefined) {
    headers.set('Content-Type', 'application/json')
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  return headers
}

async function send(
  path: string,
  method: HttpMethod,
  headers: Headers,
  body: unknown,
  signal: AbortSignal | undefined,
): Promise<Response> {
  try {
    return await fetch(`${baseUrl()}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    })
  } catch (error) {
    rethrowIfCancelled(error, signal)
    throw new ApiError('NETWORK_ERROR', NETWORK_ERROR_MESSAGE, 0, undefined, error)
  }
}

// Some errors never reach our controllers (gateway, firewall): their body is not an ApiResponse
async function readEnvelope<T>(response: Response, signal: AbortSignal | undefined): Promise<ApiResponse<T> | null> {
  let body: unknown
  try {
    body = await response.json()
  } catch (error) {
    rethrowIfCancelled(error, signal)
    // Not JSON at all (empty or HTML): no envelope. Anything else: the connection broke while reading
    if (error instanceof SyntaxError) {
      return null
    }
    throw new ApiError('NETWORK_ERROR', NETWORK_ERROR_MESSAGE, 0, undefined, error)
  }
  return isEnvelope<T>(body) ? body : null
}

// A cancelled request is not a failure: the caller decides what to do
function rethrowIfCancelled(error: unknown, signal: AbortSignal | undefined): void {
  if (signal?.aborted) {
    throw signal.reason
  }
  if (error instanceof DOMException && error.name === 'AbortError') {
    throw error
  }
}

function isEnvelope<T>(body: unknown): body is ApiResponse<T> {
  return typeof body === 'object' && body !== null && 'success' in body
}

function errorWithoutEnvelope(status: number): ApiError {
  if (GATEWAY_STATUSES.includes(status)) {
    return new ApiError('SERVICE_UNAVAILABLE', NOT_READY_MESSAGE, status)
  }
  if (status >= 400 && status < 500) {
    return new ApiError('REQUEST_FAILED', INVALID_REQUEST_MESSAGE, status)
  }
  return new ApiError('INTERNAL_ERROR', UNEXPECTED_ERROR_MESSAGE, status)
}

function baseUrl(): string {
  return (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/+$/, '')
}
