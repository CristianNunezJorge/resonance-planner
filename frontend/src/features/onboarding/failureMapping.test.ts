import { describe, expect, it } from 'vitest'
import type { RequestFailure } from '../../api/client'
import { mapFailure } from './failureMapping'

const IDS = ['aalto', 'jinhsi']

function validationFailure(fields: Record<string, string>): RequestFailure {
  return { code: 'VALIDATION_ERROR', message: 'Alcuni campi non sono validi.', status: 400, fields }
}

// The server names a field like characters[1].currentLevel (D109); the page knows it by the id of the character
describe('mapFailure', () => {
  it('puts the error of a level under the character that the index points to, in step 2', () => {
    const result = mapFailure(validationFailure({ 'characters[1].currentLevel': 'Con il grado 1 il livello va da 20 a 40.' }), IDS)

    expect(result).toEqual({
      errors: { 'progress.jinhsi.current': 'Con il grado 1 il livello va da 20 a 40.' },
      step: 2,
      generalError: null,
    })
  })

  it('maps both the level and the rank of a side to that side, keeping the first message', () => {
    const result = mapFailure(
      validationFailure({
        'characters[0].targetRank': "L'obiettivo non può essere più basso della situazione attuale.",
        'characters[0].targetLevel': 'Con il grado 0 il livello va da 1 a 20.',
      }),
      IDS,
    )

    expect(result.errors).toEqual({
      'progress.aalto.target': "L'obiettivo non può essere più basso della situazione attuale.",
    })
  })

  it('maps the Waveplate, the Crystals and the play time to step 3', () => {
    const result = mapFailure(
      validationFailure({
        'waveplate.value': 'Le Waveplate vanno da 0 a 240.',
        'waveplate.crystals': 'I Waveplate Crystals vanno da 0 a 480.',
        playTimeMinutes: 'Scegli un tempo di gioco tra 30, 60 o 120 minuti.',
      }),
      IDS,
    )

    expect(result).toEqual({
      errors: {
        waveplate: 'Le Waveplate vanno da 0 a 240.',
        crystals: 'I Waveplate Crystals vanno da 0 a 480.',
        playTimeMinutes: 'Scegli un tempo di gioco tra 30, 60 o 120 minuti.',
      },
      step: 3,
      generalError: null,
    })
  })

  it('maps the list of characters, and an error of an identifier, to step 1', () => {
    expect(mapFailure(validationFailure({ characters: 'Scegli almeno un personaggio.' }), IDS)).toEqual({
      errors: { characters: 'Scegli almeno un personaggio.' },
      step: 1,
      generalError: null,
    })
    expect(mapFailure(validationFailure({ 'characters[0].characterId': 'Questo personaggio non esiste.' }), IDS)).toEqual({
      errors: { characters: 'Questo personaggio non esiste.' },
      step: 1,
      generalError: null,
    })
  })

  it('goes back to the earliest step that has an error', () => {
    const result = mapFailure(
      validationFailure({
        playTimeMinutes: 'Scegli un tempo di gioco tra 30, 60 o 120 minuti.',
        'characters[0].currentLevel': 'Con il grado 0 il livello va da 1 a 20.',
        'waveplate.value': 'Le Waveplate vanno da 0 a 240.',
      }),
      IDS,
    )

    expect(result.step).toBe(2)
    expect(Object.keys(result.errors)).toHaveLength(3)
  })

  it('keeps the message of a failure that names no field, for the alert', () => {
    const conflict: RequestFailure = { code: 'CONFLICT', message: 'La configurazione è già stata completata.', status: 409 }
    const network: RequestFailure = { code: 'NETWORK_ERROR', message: 'Impossibile contattare il server.', status: 0 }

    expect(mapFailure(conflict, IDS)).toEqual({ errors: {}, step: null, generalError: conflict.message })
    expect(mapFailure(network, IDS)).toEqual({ errors: {}, step: null, generalError: network.message })
  })

  it('shows in the alert the fields that the page cannot place, together with those it can', () => {
    const result = mapFailure(
      validationFailure({ 'waveplate.value': 'Le Waveplate vanno da 0 a 240.', 'characters[7].currentLevel': 'x', weird: 'y' }),
      IDS,
    )

    expect(result.errors).toEqual({ waveplate: 'Le Waveplate vanno da 0 a 240.' })
    expect(result.step).toBe(3)
    expect(result.generalError).toBe('Alcuni campi non sono validi.')
  })
})
