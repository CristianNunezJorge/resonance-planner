import { useEffect, useRef, useState } from 'react'
import { useAppDispatch, useAppSelector } from '../../app/hooks'
import { toastShown } from '../../app/toastSlice'
import CharacterAvatar from '../../components/CharacterAvatar'
import ConfirmModal from '../../components/ConfirmModal'
import { formatGameTerm } from '../catalog/format'
import type { CharacterSummary, GameRules } from '../catalog/types'
import { draftOf } from '../progress/levels'
import { describeGoal, missingSummary } from './format'
import ProgressForm from './ProgressForm'
import { removeCharacter, requirementsOf, updateCharacter } from './rosterSlice'
import type { RosterEntry } from './types'

interface RosterCardProps {
  entry: RosterEntry
  // Missing only if the catalog changed: then the identifier stands for the name
  character: CharacterSummary | undefined
  rules: GameRules
  onRemoved: () => void
}

// A character of the roster (P5): goal, how much of the materials is already owned and what is missing, with the
// buttons to change the levels or to remove it after a confirmation (D69, D116)
function RosterCard({ entry, character, rules, onRemoved }: RosterCardProps) {
  const dispatch = useAppDispatch()
  const requirements = useAppSelector((state) => requirementsOf(state, entry.characterId))
  const [isEditing, setIsEditing] = useState(false)
  const [isConfirming, setIsConfirming] = useState(false)
  const [isRemoving, setIsRemoving] = useState(false)
  const editButtonRef = useRef<HTMLButtonElement>(null)
  // Set when the form closes: the effect gives the focus back after the button is drawn again
  const returnFocusRef = useRef(false)
  const name = character?.name ?? entry.characterId
  const titleId = `roster-${entry.characterId}-title`
  const progressId = `roster-${entry.characterId}-progress`

  // The form is gone: the focus goes back to the button that opened it (WCAG 2.4.3)
  useEffect(() => {
    if (!isEditing && returnFocusRef.current) {
      returnFocusRef.current = false
      editButtonRef.current?.focus()
    }
  }, [isEditing])

  const closeForm = () => {
    returnFocusRef.current = true
    setIsEditing(false)
  }

  const save = async (saved: RosterEntry) => {
    const { characterId, ...progress } = saved
    const result = await dispatch(updateCharacter({ characterId, progress }))
    if (updateCharacter.rejected.match(result)) {
      return result.payload ?? { code: 'INTERNAL_ERROR', message: 'Non è stato possibile salvare.', status: 0 }
    }
    dispatch(toastShown(`${name}: livelli aggiornati.`))
    closeForm()
    return null
  }

  const remove = async () => {
    setIsRemoving(true)
    const result = await dispatch(removeCharacter(entry.characterId))
    setIsRemoving(false)
    setIsConfirming(false)
    if (removeCharacter.rejected.match(result)) {
      dispatch(toastShown(result.payload?.message ?? 'Non è stato possibile rimuovere il personaggio.', 'danger'))
      return
    }
    dispatch(toastShown(`${name} è uscito dal roster.`))
    onRemoved()
  }

  const missing = requirements ? missingSummary(requirements.materials) : ''

  return (
    <article className="card mb-3" aria-labelledby={titleId}>
      <div className="card-body">
        <div className="d-flex gap-3 align-items-start">
          {character && <CharacterAvatar character={character} />}
          <div className="flex-grow-1">
            <h3 id={titleId} className="h5 mb-1">
              {name}
            </h3>
            {character && (
              <p className="mb-1">
                <span className={`badge element-${character.element.toLowerCase()}`}>
                  {formatGameTerm(character.element)}
                </span>{' '}
                <span className="small text-body-secondary">{formatGameTerm(character.weaponType)}</span>
              </p>
            )}
            <p className="mb-2">{describeGoal(entry, rules)}</p>
            {requirements && (
              <>
                <div className="d-flex align-items-center gap-2 mb-2">
                  <label htmlFor={progressId} className="small mb-0">
                    Materiali
                  </label>
                  <progress
                    id={progressId}
                    className="completion-progress flex-grow-1"
                    value={requirements.completionPercent}
                    max={100}
                  />
                  <span className="small">{requirements.completionPercent}%</span>
                </div>
                {requirements.materials.length === 0 ? (
                  <p className="small mb-0">Obiettivo raggiunto: non serve altro.</p>
                ) : missing === '' ? (
                  <p className="small mb-0">Hai già tutti i materiali che servono.</p>
                ) : (
                  <p className="small mb-0">
                    <span className="fw-semibold">Mancano:</span> {missing}
                    {requirements.containsEstimates && (
                      <>
                        {' '}
                        <span className="badge text-bg-secondary">stima</span>
                      </>
                    )}
                  </p>
                )}
              </>
            )}
          </div>
        </div>
        {isEditing ? (
          <div className="mt-3">
            <ProgressForm
              idPrefix={`edit-${entry.characterId}`}
              rules={rules}
              characterId={entry.characterId}
              current={draftOf(entry.currentLevel, entry.currentRank, rules)}
              target={draftOf(entry.targetLevel, entry.targetRank, rules)}
              submitLabel="Salva"
              onSubmit={save}
              onCancel={closeForm}
            />
          </div>
        ) : (
          <div className="d-flex justify-content-end gap-2 mt-3">
            <button
              ref={editButtonRef}
              type="button"
              className="btn btn-outline-light btn-sm"
              onClick={() => setIsEditing(true)}
              aria-label={`Modifica ${name}`}
            >
              Modifica
            </button>
            <button
              type="button"
              className="btn btn-outline-danger btn-sm"
              onClick={() => setIsConfirming(true)}
              aria-label={`Rimuovi ${name}`}
            >
              Rimuovi
            </button>
          </div>
        )}
      </div>
      <ConfirmModal
        open={isConfirming}
        title={`Rimuovere ${name} dal roster?`}
        message="Il personaggio esce dal roster con i suoi obiettivi. L'inventario e le attività già fatte restano."
        confirmLabel="Rimuovi"
        busy={isRemoving}
        onConfirm={() => void remove()}
        onCancel={() => setIsConfirming(false)}
      />
    </article>
  )
}

export default RosterCard
