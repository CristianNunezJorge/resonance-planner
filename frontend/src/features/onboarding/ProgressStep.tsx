import { useAppDispatch, useAppSelector } from '../../app/hooks'
import { formatGameTerm } from '../catalog/format'
import type { CharacterSummary, GameRules } from '../catalog/types'
import LevelPicker from './LevelPicker'
import { ascensionChanged, levelChanged } from './onboardingSlice'
import { progressKey } from './validation'

interface ProgressStepProps {
  characters: CharacterSummary[]
  rules: GameRules
}

// Step 2: where each chosen character is now, and how far the user wants to take it (D20, D104)
function ProgressStep({ characters, rules }: ProgressStepProps) {
  const dispatch = useAppDispatch()
  const { selected, drafts, errors } = useAppSelector((state) => state.onboarding)
  // In the order of the catalog, like the roster that the server saves
  const chosen = characters.filter((character) => selected.includes(character.id))

  return (
    <div>
      <p className="text-body-secondary">
        Per ogni personaggio, a che livello è adesso e fin dove vuoi arrivare (da 1 a {rules.maxLevel}).
      </p>
      {chosen.map((character) => {
        const draft = drafts[character.id]
        return (
          <fieldset key={character.id} className="border rounded p-3 mb-3">
            <legend className="float-none w-auto px-2 fs-5 mb-0">
              <span className="fw-semibold">{character.name}</span>{' '}
              <span className={`badge element-${character.element.toLowerCase()}`}>
                {formatGameTerm(character.element)}
              </span>{' '}
              <span className="small text-body-secondary">{formatGameTerm(character.weaponType)}</span>
            </legend>
            <div className="row g-3">
              <div className="col-md-6">
                <LevelPicker
                  id={`current-${character.id}`}
                  label="Livello attuale"
                  value={draft.current}
                  rules={rules}
                  ascensionLabel={(level) => `Ho già fatto l'ascensione al ${level}`}
                  error={errors[progressKey(character.id, 'current')]}
                  onLevel={(level) => dispatch(levelChanged({ id: character.id, side: 'current', level }))}
                  onAscension={(ascended) =>
                    dispatch(ascensionChanged({ id: character.id, side: 'current', ascended }))
                  }
                />
              </div>
              <div className="col-md-6">
                <LevelPicker
                  id={`target-${character.id}`}
                  label="Obiettivo"
                  value={draft.target}
                  rules={rules}
                  ascensionLabel={(level) => `Voglio fare l'ascensione al ${level}`}
                  error={errors[progressKey(character.id, 'target')]}
                  onLevel={(level) => dispatch(levelChanged({ id: character.id, side: 'target', level }))}
                  onAscension={(ascended) =>
                    dispatch(ascensionChanged({ id: character.id, side: 'target', ascended }))
                  }
                />
              </div>
            </div>
          </fieldset>
        )
      })}
    </div>
  )
}

export default ProgressStep
