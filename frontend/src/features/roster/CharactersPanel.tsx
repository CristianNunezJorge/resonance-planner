import { useEffect, useRef, useState } from 'react'
import { useAppDispatch, useAppSelector } from '../../app/hooks'
import { toastShown } from '../../app/toastSlice'
import EmptyState from '../../components/EmptyState'
import type { CharacterSummary, GameRules } from '../catalog/types'
import FarmingSummary from './FarmingSummary'
import ProgressForm from './ProgressForm'
import RosterCard from './RosterCard'
import { addCharacter } from './rosterSlice'
import type { RosterEntry } from './types'

interface CharactersPanelProps {
  characters: CharacterSummary[]
  rules: GameRules
}

// The tab "Personaggi" of P5: the characters of the roster, the form to add one and the estimate of the whole roster
function CharactersPanel({ characters, rules }: CharactersPanelProps) {
  const dispatch = useAppDispatch()
  const entries = useAppSelector((state) => state.roster.entries)
  const plan = useAppSelector((state) => state.roster.plan)
  const [isAdding, setIsAdding] = useState(false)
  const headingRef = useRef<HTMLHeadingElement>(null)
  const addButtonRef = useRef<HTMLButtonElement>(null)
  const returnFocusRef = useRef(false)

  const inRoster = new Set(entries.map((entry) => entry.characterId))
  const choices = characters.filter((character) => !inRoster.has(character.id))
  const byId = new Map(characters.map((character) => [character.id, character]))

  // The form is gone: the focus goes back to "Aggiungi", or to the title when every character is in the roster
  useEffect(() => {
    if (!isAdding && returnFocusRef.current) {
      returnFocusRef.current = false
      ;(addButtonRef.current ?? headingRef.current)?.focus()
    }
  }, [isAdding])

  const closeForm = () => {
    returnFocusRef.current = true
    setIsAdding(false)
  }

  const add = async (entry: RosterEntry) => {
    const result = await dispatch(addCharacter(entry))
    if (addCharacter.rejected.match(result)) {
      return result.payload ?? { code: 'INTERNAL_ERROR', message: 'Non è stato possibile aggiungere.', status: 0 }
    }
    const name = byId.get(entry.characterId)?.name ?? entry.characterId
    dispatch(toastShown(`${name} è entrato nel roster.`))
    closeForm()
    return null
  }

  return (
    <div>
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
        <h2 ref={headingRef} tabIndex={-1} className="h4 mb-0">
          I tuoi personaggi
        </h2>
        {!isAdding && choices.length > 0 && (
          <button ref={addButtonRef} type="button" className="btn btn-primary" onClick={() => setIsAdding(true)}>
            Aggiungi un personaggio
          </button>
        )}
        {!isAdding && choices.length === 0 && (
          <p className="small text-body-secondary mb-0">Tutti i personaggi del catalogo sono nel tuo roster.</p>
        )}
      </div>
      {isAdding && (
        <ProgressForm
          idPrefix="add"
          rules={rules}
          choices={choices}
          characterId=""
          // The usual goal, as in the guided setup: from level 1 to the maximum, ascended (D111)
          current={{ level: 1, ascended: false }}
          target={{ level: rules.maxLevel, ascended: true }}
          submitLabel="Aggiungi"
          onSubmit={add}
          onCancel={closeForm}
        />
      )}
      {entries.length === 0 && !isAdding && <EmptyState message="Nessun personaggio: aggiungine uno per iniziare." />}
      {entries.map((entry) => (
        <RosterCard
          key={entry.characterId}
          entry={entry}
          character={byId.get(entry.characterId)}
          rules={rules}
          // The card and its buttons are gone: the focus goes to the title of the list (WCAG 2.4.3)
          onRemoved={() => headingRef.current?.focus()}
        />
      ))}
      {plan && entries.length > 0 && <FarmingSummary plan={plan} />}
    </div>
  )
}

export default CharactersPanel
