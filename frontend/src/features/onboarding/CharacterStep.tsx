import { useAppDispatch, useAppSelector } from '../../app/hooks'
import type { CharacterSummary } from '../catalog/types'
import CharacterChoice from './CharacterChoice'
import { characterToggled } from './onboardingSlice'
import { CHARACTERS_KEY } from './validation'

const ERROR_ID = 'characters-error'

interface CharacterStepProps {
  characters: CharacterSummary[]
  // The goal that a newly chosen character starts with
  maxLevel: number
}

// Step 1: the characters the user wants to grow (D20). At least one is needed.
function CharacterStep({ characters, maxLevel }: CharacterStepProps) {
  const dispatch = useAppDispatch()
  const selected = useAppSelector((state) => state.onboarding.selected)
  const error = useAppSelector((state) => state.onboarding.errors[CHARACTERS_KEY])

  return (
    <fieldset>
      <legend className="visually-hidden">Personaggi della demo</legend>
      <p className="text-body-secondary">Scegli i personaggi che vuoi far crescere: ne serve almeno uno.</p>
      {/* Six columns on a wide screen, as on the presentation page: the 256 px portraits are never shown larger */}
      <div className="row row-cols-2 row-cols-md-3 row-cols-xl-6 g-3">
        {characters.map((character) => (
          <div className="col" key={character.id}>
            <CharacterChoice
              character={character}
              checked={selected.includes(character.id)}
              onToggle={() => dispatch(characterToggled(character.id, maxLevel))}
              invalid={error !== undefined}
              describedBy={error ? ERROR_ID : undefined}
            />
          </div>
        ))}
      </div>
      {error && (
        // text-danger is 4.07:1 on the dark background; the emphasis colour, like invalid-feedback, is 7.30:1 (D70)
        <p id={ERROR_ID} className="text-danger-emphasis mt-2 mb-0">
          {error}
        </p>
      )}
    </fieldset>
  )
}

export default CharacterStep
