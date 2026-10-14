import { useAppDispatch, useAppSelector } from '../../app/hooks'
import FormField from '../../components/FormField'
import type { GameRules } from '../catalog/types'
import { formatPlayTime } from './levels'
import { crystalsTyped, playTimeChosen, waveplateTyped } from './onboardingSlice'
import { CRYSTALS_KEY, PLAY_TIME_KEY, WAVEPLATE_KEY } from './validation'

const PLAY_TIME_ERROR_ID = 'play-time-error'

interface StatusStepProps {
  rules: GameRules
}

// Step 3: the Waveplate and the Crystals the user has now, and how long they play every day (D20)
function StatusStep({ rules }: StatusStepProps) {
  const dispatch = useAppDispatch()
  const { waveplate, crystals, playTimeMinutes, errors } = useAppSelector((state) => state.onboarding)
  const playTimeError = errors[PLAY_TIME_KEY]

  return (
    <div>
      <FormField
        id="waveplate"
        label="Waveplate attuali"
        required
        inputMode="numeric"
        autoComplete="off"
        hint={`Il valore che vedi nel gioco, da 0 a ${rules.waveplate.max}.`}
        value={waveplate}
        onChange={(text) => dispatch(waveplateTyped(text))}
        error={errors[WAVEPLATE_KEY]}
      />
      <FormField
        id="crystals"
        label="Waveplate Crystals attuali"
        inputMode="numeric"
        autoComplete="off"
        hint={`Da 0 a ${rules.waveplateCrystals.max}. Se non ne hai, lascia 0.`}
        value={crystals}
        onChange={(text) => dispatch(crystalsTyped(text))}
        error={errors[CRYSTALS_KEY]}
      />
      <fieldset className="mb-3">
        <legend className="form-label fs-6">Quanto giochi al giorno?</legend>
        {rules.playTimeOptionsMinutes.map((minutes) => {
          const inputId = `play-time-${minutes}`
          return (
            <div className="form-check" key={minutes}>
              <input
                id={inputId}
                type="radio"
                name="play-time"
                className="form-check-input"
                value={minutes}
                checked={playTimeMinutes === minutes}
                onChange={() => dispatch(playTimeChosen(minutes))}
                aria-invalid={playTimeError ? true : undefined}
                aria-describedby={playTimeError ? PLAY_TIME_ERROR_ID : undefined}
              />
              <label htmlFor={inputId} className="form-check-label">
                {formatPlayTime(minutes)}
              </label>
            </div>
          )
        })}
        {playTimeError && (
          // Same colour as the error of the other fields (invalid-feedback): readable on the dark background (D70)
          <p id={PLAY_TIME_ERROR_ID} className="text-danger-emphasis small mt-1 mb-0">
            {playTimeError}
          </p>
        )}
      </fieldset>
    </div>
  )
}

export default StatusStep
