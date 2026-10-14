import { describe, expect, it } from 'vitest'
import { gameRules } from '../../test/fixtures'
import type { CharacterDraft } from './types'
import { progressKey, validateProgress, validateSelection, validateStatus, validateStep } from './validation'

const MAX_GOAL: CharacterDraft = { current: { level: 1, ascended: false }, target: { level: 40, ascended: true } }

describe('validateSelection', () => {
  it('asks for at least one character', () => {
    expect(validateSelection([])).toEqual({ characters: 'Scegli almeno un personaggio.' })
  })

  it('accepts one or more characters', () => {
    expect(validateSelection(['jinhsi'])).toEqual({})
    expect(validateSelection(['jinhsi', 'aalto'])).toEqual({})
  })
})

describe('validateProgress', () => {
  it('accepts the default goal and a goal equal to the present', () => {
    const equal: CharacterDraft = { current: { level: 30, ascended: false }, target: { level: 30, ascended: false } }

    expect(validateProgress(['jinhsi', 'aalto'], { jinhsi: MAX_GOAL, aalto: equal }, gameRules)).toEqual({})
  })

  it('refuses a goal lower than the present and names the character that has to be fixed', () => {
    const lower: CharacterDraft = { current: { level: 30, ascended: false }, target: { level: 25, ascended: false } }

    expect(validateProgress(['jinhsi', 'aalto'], { jinhsi: MAX_GOAL, aalto: lower }, gameRules)).toEqual({
      [progressKey('aalto', 'target')]: "L'obiettivo non può essere più basso della situazione attuale.",
    })
  })

  it('compares the ascension too: ascended at 20 and a goal of 20 without it is lower', () => {
    const lower: CharacterDraft = { current: { level: 20, ascended: true }, target: { level: 20, ascended: false } }

    expect(Object.keys(validateProgress(['jinhsi'], { jinhsi: lower }, gameRules))).toEqual(['progress.jinhsi.target'])
  })

  it('judges only the characters that are chosen', () => {
    const lower: CharacterDraft = { current: { level: 30, ascended: false }, target: { level: 25, ascended: false } }

    expect(validateProgress(['jinhsi'], { jinhsi: MAX_GOAL, aalto: lower }, gameRules)).toEqual({})
  })
})

describe('validateStatus', () => {
  const valid = { waveplate: '180', crystals: '0', playTimeMinutes: 60 }

  it('accepts a valid status, with the boundaries included', () => {
    expect(validateStatus(valid, gameRules)).toEqual({})
    expect(validateStatus({ ...valid, waveplate: '0' }, gameRules)).toEqual({})
    expect(validateStatus({ ...valid, waveplate: '240', crystals: '480' }, gameRules)).toEqual({})
    // Spaces around the number are not a mistake
    expect(validateStatus({ ...valid, waveplate: ' 180 ' }, gameRules)).toEqual({})
  })

  it('asks for the Waveplate, but takes empty Crystals as zero', () => {
    expect(validateStatus({ ...valid, waveplate: '' }, gameRules)).toEqual({
      waveplate: 'Inserisci le Waveplate che vedi nel gioco.',
    })
    expect(validateStatus({ ...valid, waveplate: '   ' }, gameRules)).toEqual({
      waveplate: 'Inserisci le Waveplate che vedi nel gioco.',
    })
    expect(validateStatus({ ...valid, crystals: '' }, gameRules)).toEqual({})
  })

  it.each(['241', '-1', '12.5', '1e2', 'abc', '99999999999999999999', '+5', '0x10'])(
    'refuses "%s" as Waveplate',
    (text) => {
      expect(validateStatus({ ...valid, waveplate: text }, gameRules)).toEqual({
        waveplate: 'Inserisci un numero intero da 0 a 240.',
      })
    },
  )

  it.each(['481', '-1', '2.5', 'many'])('refuses "%s" as Crystals', (text) => {
    expect(validateStatus({ ...valid, crystals: text }, gameRules)).toEqual({
      crystals: 'Inserisci un numero intero da 0 a 480.',
    })
  })

  it('accepts Crystals even when the Waveplate are not full: they stay when the Waveplate are spent (D105)', () => {
    expect(validateStatus({ ...valid, waveplate: '100', crystals: '300' }, gameRules)).toEqual({})
  })

  it('asks for the play time', () => {
    expect(validateStatus({ ...valid, playTimeMinutes: null }, gameRules)).toEqual({
      playTimeMinutes: 'Scegli quanto giochi al giorno.',
    })
  })

  it('lists every wrong field at once', () => {
    expect(Object.keys(validateStatus({ waveplate: '', crystals: '999', playTimeMinutes: null }, gameRules))).toEqual([
      'waveplate',
      'crystals',
      'playTimeMinutes',
    ])
  })
})

describe('validateStep', () => {
  const draft = {
    selected: [] as string[],
    drafts: {},
    waveplate: '',
    crystals: '0',
    playTimeMinutes: null,
  }

  it('checks only what belongs to the step', () => {
    expect(Object.keys(validateStep(1, draft, gameRules))).toEqual(['characters'])
    expect(validateStep(2, draft, gameRules)).toEqual({})
    expect(Object.keys(validateStep(3, draft, gameRules))).toEqual(['waveplate', 'playTimeMinutes'])
  })
})
