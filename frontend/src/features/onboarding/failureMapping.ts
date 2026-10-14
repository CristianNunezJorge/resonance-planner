import type { RequestFailure } from '../../api/client'
import type { Step } from './types'
import {
  CHARACTERS_KEY,
  CRYSTALS_KEY,
  PLAY_TIME_KEY,
  WAVEPLATE_KEY,
  progressKey,
  type FieldErrors,
} from './validation'

export interface MappedFailure {
  // The messages of the fields that the page can show, under the keys of validation.ts
  errors: FieldErrors
  // The earliest step that has one of those fields, or null if none
  step: Step | null
  // The message for the alert, when the failure names no field or names one the page cannot place
  generalError: string | null
}

interface Place {
  key: string
  step: Step
}

// A character of the list: characters[1].currentLevel, or only characters[1] for an element that is not an object
const LIST_FIELD = /^characters\[(\d+)\](?:\.(\w+))?$/
const LEVEL_FIELD = /^(current|target)(?:Level|Rank)$/

/**
 * Turns the fields of a failed request into what the page shows. The server names a character by its position in the
 * request (D109: characters[1].currentLevel), so the page needs the ids in the order it sent them.
 */
export function mapFailure(failure: RequestFailure, characterIds: string[]): MappedFailure {
  const errors: FieldErrors = {}
  const steps: Step[] = []
  let hasUnplaced = false
  const fields = Object.entries(failure.fields ?? {})

  for (const [field, message] of fields) {
    const place = placeField(field, characterIds)
    if (!place) {
      hasUnplaced = true
    } else {
      // A level and a rank of the same side are one input for the user: the first message is enough
      errors[place.key] ??= message
      steps.push(place.step)
    }
  }

  const step = steps.length > 0 ? (Math.min(...steps) as Step) : null
  const generalError = fields.length === 0 || hasUnplaced ? failure.message : null
  return { errors, step, generalError }
}

function placeField(field: string, characterIds: string[]): Place | null {
  if (field === CHARACTERS_KEY) {
    return { key: CHARACTERS_KEY, step: 1 }
  }
  const listField = LIST_FIELD.exec(field)
  if (listField) {
    return placeCharacterField(characterIds[Number(listField[1])], listField[2])
  }
  switch (field) {
    case 'waveplate':
    case 'waveplate.value':
      return { key: WAVEPLATE_KEY, step: 3 }
    case 'waveplate.crystals':
      return { key: CRYSTALS_KEY, step: 3 }
    case PLAY_TIME_KEY:
      return { key: PLAY_TIME_KEY, step: 3 }
    default:
      return null
  }
}

function placeCharacterField(characterId: string | undefined, name: string | undefined): Place | null {
  if (characterId === undefined || name === undefined) {
    return null
  }
  if (name === 'characterId') {
    return { key: CHARACTERS_KEY, step: 1 }
  }
  const side = LEVEL_FIELD.exec(name)
  return side ? { key: progressKey(characterId, side[1] as 'current' | 'target'), step: 2 } : null
}
