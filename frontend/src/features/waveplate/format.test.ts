import { describe, expect, it } from 'vitest'
import { fullAtText } from './format'

describe('fullAtText', () => {
  const now = new Date(2026, 9, 16, 10, 0)

  it('says when the Waveplate will be full', () => {
    expect(fullAtText(new Date(2026, 9, 16, 16, 24).toISOString(), now)).toBe('piene alle 16:24')
    expect(fullAtText(new Date(2026, 9, 17, 3, 5).toISOString(), now)).toBe('piene domani alle 03:05')
    expect(fullAtText(new Date(2026, 9, 19, 8, 0).toISOString(), now)).toBe('piene lunedì 19 alle 08:00')
  })

  it('says that they are already full', () => {
    expect(fullAtText(new Date(2026, 9, 16, 9, 0).toISOString(), now)).toBe('già piene')
    expect(fullAtText(now.toISOString(), now)).toBe('già piene')
  })
})
