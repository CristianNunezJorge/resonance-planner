import { describe, expect, it } from 'vitest'
import { apiOriginOf, contentSecurityPolicy } from './csp.ts'

describe('contentSecurityPolicy', () => {
  const policy = contentSecurityPolicy('https://resonance-planner-api.onrender.com')
  const directive = (name: string) => policy.split('; ').find((part) => part.startsWith(`${name} `))

  it('lets the page talk only to itself and to the backend', () => {
    expect(directive('connect-src')).toBe("connect-src 'self' https://resonance-planner-api.onrender.com")
    expect(directive('default-src')).toBe("default-src 'self'")
  })

  it('does not allow inline or evaluated code', () => {
    expect(policy).not.toContain('unsafe-inline')
    expect(policy).not.toContain('unsafe-eval')
    expect(directive('script-src')).toBe("script-src 'self'")
    expect(directive('style-src')).toBe("style-src 'self'")
  })

  it('forbids plugins, other base addresses and being shown inside another site', () => {
    expect(directive('object-src')).toBe("object-src 'none'")
    expect(directive('base-uri')).toBe("base-uri 'self'")
    expect(directive('frame-ancestors')).toBe("frame-ancestors 'none'")
  })

  it('allows data: images only for the icons of Bootstrap', () => {
    expect(directive('img-src')).toBe("img-src 'self' data:")
  })
})

describe('apiOriginOf', () => {
  it('gives the origin of a complete https address, without path and final slash', () => {
    expect(apiOriginOf('https://resonance-planner-api.onrender.com/')).toBe('https://resonance-planner-api.onrender.com')
    expect(apiOriginOf('https://api.example.com:8443/base/path')).toBe('https://api.example.com:8443')
  })

  it('accepts http only for this computer', () => {
    expect(apiOriginOf('http://localhost:8080')).toBe('http://localhost:8080')
    expect(apiOriginOf('http://127.0.0.1:8080')).toBe('http://127.0.0.1:8080')
    expect(() => apiOriginOf('http://api.example.com')).toThrow('must use https')
  })

  it('refuses an address that is not complete, with a clear message', () => {
    expect(() => apiOriginOf('/api')).toThrow('complete address')
    expect(() => apiOriginOf('api.example.com')).toThrow('complete address')
    expect(() => apiOriginOf('')).toThrow('complete address')
  })

  it('refuses other schemes', () => {
    expect(() => apiOriginOf('ftp://api.example.com')).toThrow('must use https')
    expect(() => apiOriginOf('javascript:alert(1)')).toThrow('must use https')
  })
})
