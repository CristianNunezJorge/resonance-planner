import SelectField from '../../components/SelectField'
import type { GameRules } from '../catalog/types'
import { isAscensionCap, levelsOf } from './levels'
import type { LevelDraft } from './types'

interface LevelPickerProps {
  // Prefix of the ids of the controls, unique for the character and the side
  id: string
  label: string
  value: LevelDraft
  rules: GameRules
  // The text of the ascension checkbox for the level (it is shown only at the levels where an ascension is needed)
  ascensionLabel: (level: number) => string
  error?: string
  onLevel: (level: number) => void
  onAscension: (ascended: boolean) => void
}

// A level of a character (D34, D104): a select, and, only at a level where an ascension is needed, a checkbox that
// says whether it is done. The rank is never shown: it follows from the two.
function LevelPicker({ id, label, value, rules, ascensionLabel, error, onLevel, onAscension }: LevelPickerProps) {
  const options = levelsOf(rules).map((level) => ({ value: String(level), label: String(level) }))
  const ascensionId = `${id}-ascended`

  return (
    <div>
      <SelectField
        id={`${id}-level`}
        label={label}
        value={String(value.level)}
        onChange={(level) => onLevel(Number(level))}
        options={options}
        error={error}
      />
      {isAscensionCap(value.level, rules) && (
        <div className="form-check mb-3">
          <input
            id={ascensionId}
            type="checkbox"
            className="form-check-input"
            checked={value.ascended}
            onChange={(event) => onAscension(event.target.checked)}
          />
          <label htmlFor={ascensionId} className="form-check-label">
            {ascensionLabel(value.level)}
          </label>
        </div>
      )}
    </div>
  )
}

export default LevelPicker
