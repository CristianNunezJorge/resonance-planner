import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiRequest } from './client'

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('apiRequest', () => {
  const fetchMock = vi.fn<typeof fetch>()

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
    vi.stubEnv('VITE_API_BASE_URL', 'https://api.example.com')
  })

  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
    vi.unstubAllEnvs()
  })

  it('returns the data of a successful response', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ success: true, data: [{ id: 'jinhsi' }], error: null }))

    await expect(apiRequest('/api/game/characters')).resolves.toEqual([{ id: 'jinhsi' }])
    expect(fetchMock).toHaveBeenCalledWith(
      'https://api.example.com/api/game/characters',
      expect.objectContaining({ method: 'GET' }),
    )
  })

  it('turns an error of the API into an ApiError with code, message and fields', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(
        {
          success: false,
          data: null,
          error: {
            code: 'VALIDATION_ERROR',
            message: 'Alcuni campi non sono validi.',
            fields: { name: 'Il nome è obbligatorio.' },
          },
        },
        400,
      ),
    )

    const error = await apiRequest('/api/me/tasks').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({
      code: 'VALIDATION_ERROR',
      status: 400,
      message: 'Alcuni campi non sono validi.',
      fields: { name: 'Il nome è obbligatorio.' },
    })
  })

  it('reports a network failure with a message in Italian', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))

    await expect(apiRequest('/api/game/characters')).rejects.toMatchObject({
      code: 'NETWORK_ERROR',
      message: 'Impossibile contattare il server. Controlla la connessione e riprova.',
    })
  })

  it('handles a response that is not in the ApiResponse format', async () => {
    fetchMock.mockResolvedValueOnce(new Response('<html>Bad Request</html>', { status: 400 }))
    fetchMock.mockResolvedValueOnce(new Response('', { status: 500 }))

    await expect(apiRequest('/api/game/rules')).rejects.toMatchObject({
      code: 'REQUEST_FAILED',
      status: 400,
      message: 'Richiesta non valida.',
    })
    await expect(apiRequest('/api/game/rules')).rejects.toMatchObject({
      code: 'INTERNAL_ERROR',
      status: 500,
      message: 'Si è verificato un errore imprevisto. Riprova più tardi.',
    })
  })

  it('asks to wait when the gateway answers while the server is still starting', async () => {
    fetchMock.mockResolvedValue(new Response('', { status: 503 }))

    await expect(apiRequest('/api/game/rules')).rejects.toMatchObject({
      code: 'SERVICE_UNAVAILABLE',
      status: 503,
      message: 'Il server non è ancora pronto: riprova tra qualche istante.',
    })
  })

  it('returns nothing for a response without content', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))

    await expect(apiRequest('/api/me/tasks/1', { method: 'DELETE' })).resolves.toBeUndefined()
  })

  it('ignores a slash at the end of the base address', async () => {
    vi.stubEnv('VITE_API_BASE_URL', 'https://api.example.com/')
    fetchMock.mockResolvedValue(jsonResponse({ success: true, data: [], error: null }))

    await apiRequest('/api/game/characters')

    expect(fetchMock.mock.calls[0][0]).toBe('https://api.example.com/api/game/characters')
  })

  it('keeps the original error as the cause of a network failure', async () => {
    const networkFailure = new TypeError('Failed to fetch')
    fetchMock.mockRejectedValue(networkFailure)

    const error = await apiRequest('/api/game/characters').catch((e: unknown) => e)

    expect((error as ApiError).cause).toBe(networkFailure)
  })

  it('sends the token and the body as JSON', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ success: true, data: { id: 1 }, error: null }, 201))

    await apiRequest('/api/me/tasks', { method: 'POST', body: { title: 'Daily quests' }, token: 'abc.def.ghi' })

    const [, init] = fetchMock.mock.calls[0]
    expect(init?.method).toBe('POST')
    expect(init?.body).toBe(JSON.stringify({ title: 'Daily quests' }))
    expect(new Headers(init?.headers).get('Authorization')).toBe('Bearer abc.def.ghi')
    expect(new Headers(init?.headers).get('Content-Type')).toBe('application/json')
  })

  it('lets a cancelled request fail with its own AbortError', async () => {
    const controller = new AbortController()
    fetchMock.mockRejectedValue(new DOMException('The operation was aborted.', 'AbortError'))

    const error = await apiRequest('/api/game/characters', { signal: controller.signal }).catch((e: unknown) => e)

    expect(error).toBeInstanceOf(DOMException)
    expect((error as DOMException).name).toBe('AbortError')
    expect(fetchMock.mock.calls[0][1]?.signal).toBe(controller.signal)
  })

  it('stays an AbortError when the request is cancelled while the body is being read', async () => {
    const controller = new AbortController()
    const response = new Response('{"success": true', { status: 200 })
    vi.spyOn(response, 'json').mockImplementation(() => {
      controller.abort()
      return Promise.reject(new DOMException('The operation was aborted.', 'AbortError'))
    })
    fetchMock.mockResolvedValue(response)

    const error = await apiRequest('/api/game/characters', { signal: controller.signal }).catch((e: unknown) => e)

    expect((error as DOMException).name).toBe('AbortError')
  })
})
