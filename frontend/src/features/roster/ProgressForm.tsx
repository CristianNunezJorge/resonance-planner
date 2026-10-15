import { useEffect, useRef, useState, type FormEvent } from 'react'
import type { RequestFailure } from '../../api/client'
import SelectField from '../../components/SelectField'
import type { CharacterSummary, GameRules } from '../catalog/types'
import LevelPicker from '../progress/LevelPicker'
import { isLowerThan, rankOf } from '../progress/levels'
import type { LevelDraft } from '../progress/types'
import type { RosterEntry } from './types'

interface ProgressFormProps {
  // Prefix of the ids of the controls, unique in the page
  idPrefix: string
  rules: GameRules
  // Only when a character is added: the characters that are not in the roster yet
  choices?: CharacterSummary[]
  characterId: string
  current: LevelDraft
  target: LevelDraft
  submitLabel: string
  // Answers null when the server saved, or the failure to show
  onSubmit: (entry: RosterEntry) => Promise<RequestFailure | null>
  onCancel: () => void
}

interface FormErrors {
  characterId?: string
  current?: string
  target?: string
}

const CHOOSE_CHARACTER = 'Scegli un personaggio.'
const LOWER_GOAL = "L'obiettivo non può essere più basso della situazione attuale."

// The fields of the API that belong to each picker of the form (D104)
function errorsOf(failure: RequestFailure): FormErrors {
  const fields = failure.fields ?? {}
  return {
    characterId: fields.characterId,
    current: fields.currentLevel ?? fields.currentRank,
    target: fields.targetLevel ?? fields.targetRank,
  }
}

// Level now and goal of a character, to add it to the roster or to change it (D104, D111): the same pickers of the
// guided setup. The page checks the goal first; the server checks everything again.
function ProgressForm({
  idPrefix,
  rules,
  choices,
  characterId: initialCharacterId,
  current: initialCurrent,
  target: initialTarget,
  submitLabel,
  onSubmit,
  onCancel,
}: ProgressFormProps) {
  const [characterId, setCharacterId] = useState(initialCharacterId)
  const [current, setCurrent] = useState(initialCurrent)
  const [target, setTarget] = useState(initialTarget)
  const [errors, setErrors] = useState<FormErrors>({})
  const [generalError, setGeneralError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [failedAttempts, setFailedAttempts] = useState(0)
  const formRef = useRef<HTMLFormElement>(null)

  // After a failed attempt the focus goes to the first wrong field, or to the alert (WCAG 3.3.1)
  useEffect(() => {
    if (failedAttempts === 0) {
      return
    }
    const form = formRef.current
    const problem =
      form?.querySelector<HTMLElement>('[aria-invalid="true"]') ?? form?.querySelector<HTMLElement>('[role="alert"]')
    problem?.focus()
  }, [failedAttempts])

  const fail = (nextErrors: FormErrors, message: string | null) => {
    setErrors(nextErrors)
    setGeneralError(message)
    setFailedAttempts((attempts) => attempts + 1)
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (isSubmitting) {
      return
    }
    const found: FormErrors = {}
    if (choices && characterId === '') {
      found.characterId = CHOOSE_CHARACTER
    }
    if (isLowerThan(target, current, rules)) {
      found.target = LOWER_GOAL
    }
    if (found.characterId || found.target) {
      fail(found, null)
      return
    }
    setIsSubmitting(true)
    const failure = await onSubmit({
      characterId,
      currentLevel: current.level,
      currentRank: rankOf(current, rules),
      targetLevel: target.level,
      targetRank: rankOf(target, rules),
    })
    if (failure) {
      setIsSubmitting(false)
      const fieldErrors = errorsOf(failure)
      const aboutAField = Boolean(fieldErrors.characterId || fieldErrors.current || fieldErrors.target)
      fail(fieldErrors, aboutAField ? null : failure.message)
    }
  }

  const characterOptions = [
    { value: '', label: 'Scegli un personaggio' },
    ...(choices ?? []).map((character) => ({ value: character.id, label: character.name })),
  ]

  return (
    <form ref={formRef} onSubmit={handleSubmit} noValidate className="border rounded p-3 mb-3">
      {generalError && (
        <div role="alert" tabIndex={-1} className="alert alert-danger">
          {generalError}
        </div>
      )}
      {choices && (
        <SelectField
          id={`${idPrefix}-character`}
          label="Personaggio"
          value={characterId}
          onChange={(value) => {
            setCharacterId(value)
            setErrors((previous) => ({ ...previous, characterId: undefined }))
          }}
          options={characterOptions}
          error={errors.characterId}
        />
      )}
      <div className="row g-3">
        <div className="col-md-6">
          <LevelPicker
            id={`${idPrefix}-current`}
            label="Livello attuale"
            value={current}
            rules={rules}
            ascensionLabel={(level) => `Ho già fatto l'ascensione al ${level}`}
            error={errors.current}
            // A new level starts with the ascension not done, as in the guided setup (D111)
            onLevel={(level) => {
              setCurrent({ level, ascended: false })
              setErrors((previous) => ({ ...previous, current: undefined }))
            }}
            onAscension={(ascended) => setCurrent((previous) => ({ ...previous, ascended }))}
          />
        </div>
        <div className="col-md-6">
          <LevelPicker
            id={`${idPrefix}-target`}
            label="Obiettivo"
            value={target}
            rules={rules}
            ascensionLabel={(level) => `Voglio fare l'ascensione al ${level}`}
            error={errors.target}
            onLevel={(level) => {
              setTarget({ level, ascended: false })
              setErrors((previous) => ({ ...previous, target: undefined }))
            }}
            onAscension={(ascended) => {
              setTarget((previous) => ({ ...previous, ascended }))
              setErrors((previous) => ({ ...previous, target: undefined }))
            }}
          />
        </div>
      </div>
      <div className="d-flex justify-content-end gap-2">
        <button type="button" className="btn btn-outline-light" onClick={onCancel} disabled={isSubmitting}>
          Annulla
        </button>
        <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
          {isSubmitting ? 'Salvataggio in corso…' : submitLabel}
        </button>
      </div>
    </form>
  )
}

export default ProgressForm
