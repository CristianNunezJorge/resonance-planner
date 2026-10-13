import { afterEach, describe, expect, it, vi } from 'vitest'
import { clearToken, readToken, saveToken, TOKEN_KEY } from './tokenStorage'

describe('tokenStorage', () => {
  afterEach(() => {
    localStorage.clear()
    vi.restoreAllMocks()
  })

  it('saves, reads and clears the token', () => {
    expect(readToken()).toBeNull()

    saveToken('the.jwt.token')
    expect(readToken()).toBe('the.jwt.token')

    clearToken()
    expect(readToken()).toBeNull()
  })

  it('accepts the characters of a real JWT', () => {
    saveToken('eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI3Iiwicm9sZSI6IlVTRVIifQ.Zm9v-bar_baz')

    expect(readToken()).toBe('eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI3Iiwicm9sZSI6IlVTRVIifQ.Zm9v-bar_baz')
  })

  it('drops and forgets anything in the storage that does not look like a token', () => {
    for (const notAToken of ['', 'one-part', 'two.parts', 'a.b.c.d', 'with space.in.it', 'new\nline.in.it', 'caffè.in.it', `${'a'.repeat(2049)}.b.c`]) {
      localStorage.setItem(TOKEN_KEY, notAToken)

      expect(readToken(), JSON.stringify(notAToken)).toBeNull()
      expect(localStorage.getItem(TOKEN_KEY)).toBeNull()
    }
  })

  it('removes a token only if it is the expected one, when asked to', () => {
    saveToken('newer.jwt.token')

    clearToken('old.jwt.token')
    expect(readToken()).toBe('newer.jwt.token')

    clearToken('newer.jwt.token')
    expect(readToken()).toBeNull()
  })

  it('does not break the app when the storage is not available', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new DOMException('blocked', 'SecurityError')
    })
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('blocked', 'SecurityError')
    })
    vi.spyOn(Storage.prototype, 'removeItem').mockImplementation(() => {
      throw new DOMException('blocked', 'SecurityError')
    })

    expect(readToken()).toBeNull()
    expect(() => saveToken('a.b.c')).not.toThrow()
    expect(() => clearToken()).not.toThrow()
    expect(() => clearToken('a.b.c')).not.toThrow()
  })
})
