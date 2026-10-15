import { describe, expect, it } from 'vitest'
import { characterIconUrl } from './icons'

describe('characterIconUrl', () => {
  it('turns a path of public/ into an address of this site', () => {
    expect(characterIconUrl('characters/jinhsi.webp')).toBe('/characters/jinhsi.webp')
    expect(characterIconUrl('characters/lingyang-2.webp')).toBe('/characters/lingyang-2.webp')
  })

  it('refuses a path that could lead to another site or folder', () => {
    // "/evil.example/x.webp" would become "//evil.example/x.webp", an address of another site
    expect(characterIconUrl('/evil.example/x.webp')).toBeNull()
    expect(characterIconUrl('//evil.example/x.webp')).toBeNull()
    expect(characterIconUrl('https://evil.example/x.webp')).toBeNull()
    expect(characterIconUrl('characters/../secret.webp')).toBeNull()
    expect(characterIconUrl('a/b/c.webp')).toBeNull()
  })

  it('refuses another format, an empty value and no value', () => {
    expect(characterIconUrl('characters/jinhsi.png')).toBeNull()
    expect(characterIconUrl('characters/Jinhsi.webp')).toBeNull()
    expect(characterIconUrl('')).toBeNull()
    expect(characterIconUrl(null)).toBeNull()
  })
})
