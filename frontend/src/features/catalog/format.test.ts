import { describe, expect, it } from 'vitest'
import { formatGameTerm } from './format'

describe('formatGameTerm', () => {
  it('turns the codes of the API into the names used in the game', () => {
    expect(formatGameTerm('SPECTRO')).toBe('Spectro')
    expect(formatGameTerm('BROADBLADE')).toBe('Broadblade')
    expect(formatGameTerm('BOSS_CHALLENGE')).toBe('Boss Challenge')
  })
})
