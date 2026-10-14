import { describe, expect, it } from 'vitest'
import { aalto, catalogCharacters, gameRules, jinhsi } from '../../test/fixtures'
import { buildRequest } from './request'
import type { CharacterDraft, DraftState } from './types'

const FULL_GOAL: CharacterDraft = { current: { level: 1, ascended: false }, target: { level: 40, ascended: true } }

function draftWith(overrides: Partial<DraftState>): DraftState {
  return {
    selected: ['jinhsi', 'aalto'],
    drafts: {
      jinhsi: FULL_GOAL,
      aalto: { current: { level: 20, ascended: true }, target: { level: 40, ascended: false } },
    },
    waveplate: '180',
    crystals: '0',
    playTimeMinutes: 60,
    ...overrides,
  }
}

describe('buildRequest', () => {
  it('turns what the user picked into the body of POST /api/me/onboarding', () => {
    const request = buildRequest(draftWith({}), catalogCharacters, gameRules)

    expect(request).toEqual({
      characters: [
        { characterId: 'aalto', currentLevel: 20, currentRank: 1, targetLevel: 40, targetRank: 1 },
        { characterId: 'jinhsi', currentLevel: 1, currentRank: 0, targetLevel: 40, targetRank: 2 },
      ],
      waveplate: { value: 180, crystals: 0 },
      playTimeMinutes: 60,
    })
  })

  it('lists the characters in the order of the catalog, not in the order they were picked', () => {
    const request = buildRequest(draftWith({ selected: ['jinhsi', 'aalto'] }), [jinhsi, aalto], gameRules)

    expect(request.characters.map((character) => character.characterId)).toEqual(['jinhsi', 'aalto'])
  })

  it('leaves out the characters that were picked and then unpicked', () => {
    const request = buildRequest(draftWith({ selected: ['jinhsi'] }), catalogCharacters, gameRules)

    expect(request.characters.map((character) => character.characterId)).toEqual(['jinhsi'])
  })

  it('takes empty Crystals as zero and reads the numbers without the spaces around them', () => {
    const request = buildRequest(draftWith({ waveplate: ' 240 ', crystals: '' }), catalogCharacters, gameRules)

    expect(request.waveplate).toEqual({ value: 240, crystals: 0 })
  })

  it('stops with an error, not with a wrong request, if a step was not checked (a bug, not a mistake of the user)', () => {
    expect(() => buildRequest(draftWith({ playTimeMinutes: null }), catalogCharacters, gameRules)).toThrow(/play time/)
    expect(() => buildRequest(draftWith({ drafts: {} }), catalogCharacters, gameRules)).toThrow(/levels of aalto/)
  })
})
