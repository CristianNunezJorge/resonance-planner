import { formatGameTerm } from '../catalog/format'
import { characterIconUrl } from '../catalog/icons'
import type { CharacterSummary } from '../catalog/types'

interface CharacterChoiceProps {
  character: CharacterSummary
  checked: boolean
  onToggle: () => void
  // The group has an error (no character chosen): every choice is marked and linked to the message (D70)
  invalid: boolean
  describedBy?: string
}

// A character of step 1: the whole card is the label of a real checkbox, so the mouse, the keyboard and the screen
// reader work as with any checkbox, and the state is the tick, not only a colour
function CharacterChoice({ character, checked, onToggle, invalid, describedBy }: CharacterChoiceProps) {
  const inputId = `character-${character.id}`
  const iconUrl = characterIconUrl(character.icon)

  return (
    <label htmlFor={inputId} className="card character-choice h-100">
      <input
        id={inputId}
        type="checkbox"
        className="form-check-input character-choice-check"
        checked={checked}
        onChange={onToggle}
        aria-invalid={invalid || undefined}
        aria-describedby={describedBy}
      />
      {/* Empty alt: the name is in the label, so the image would be read twice */}
      {iconUrl && (
        <img
          src={iconUrl}
          alt=""
          className="card-img-top h-auto"
          width={256}
          height={256}
          loading="lazy"
        />
      )}
      <span className="card-body">
        <span className="d-block fw-semibold">{character.name}</span>
        <span className={`badge element-${character.element.toLowerCase()}`}>{formatGameTerm(character.element)}</span>{' '}
        <span className="small text-body-secondary">{formatGameTerm(character.weaponType)}</span>
      </span>
    </label>
  )
}

export default CharacterChoice
