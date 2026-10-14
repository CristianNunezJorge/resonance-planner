import { useEffect, useRef, type FormEvent } from 'react'
import { useAppDispatch, useAppSelector } from '../../app/hooks'
import ErrorState from '../../components/ErrorState'
import LoadingState from '../../components/LoadingState'
import { usePageTitle } from '../../components/usePageTitle'
import { fetchCharacters, fetchRules } from '../catalog/catalogSlice'
import type { CharacterSummary, GameRules } from '../catalog/types'
import CharacterStep from './CharacterStep'
import { advance, stepBack, submit } from './onboardingSlice'
import ProgressStep from './ProgressStep'
import StatusStep from './StatusStep'
import StepProgress from './StepProgress'
import type { Step } from './types'

const STEP_TITLES: Record<Step, string> = {
  1: 'Scegli i personaggi',
  2: 'Livelli e obiettivi',
  3: 'Waveplate e tempo di gioco',
}
const LAST_STEP: Step = 3

// Page P3, the guided setup (D20, F3): three short steps that collect what the agent needs. It needs the characters
// and the rules of the game, which are loaded once and kept in the store.
function OnboardingPage() {
  usePageTitle('Configurazione guidata')
  const dispatch = useAppDispatch()
  const { characters, charactersStatus, charactersError, rules, rulesStatus, rulesError } = useAppSelector(
    (state) => state.catalog,
  )

  useEffect(() => {
    void dispatch(fetchCharacters())
    void dispatch(fetchRules())
  }, [dispatch])

  const retry = () => {
    void dispatch(fetchCharacters())
    void dispatch(fetchRules())
  }

  const failureMessage =
    charactersStatus === 'failed' ? charactersError : rulesStatus === 'failed' ? rulesError : null

  return (
    <section>
      <h1>Configurazione guidata</h1>
      {failureMessage !== null && <ErrorState message={failureMessage} onRetry={retry} />}
      {failureMessage === null && (charactersStatus !== 'succeeded' || rulesStatus !== 'succeeded' || !rules) && (
        <LoadingState />
      )}
      {failureMessage === null && charactersStatus === 'succeeded' && rulesStatus === 'succeeded' && rules && (
        <>
          {characters.length === 0 && <p className="text-body-secondary">Nessun personaggio disponibile.</p>}
          {characters.length > 0 && <OnboardingWizard characters={characters} rules={rules} />}
        </>
      )}
    </section>
  )
}

interface OnboardingWizardProps {
  characters: CharacterSummary[]
  rules: GameRules
}

// The three steps in one form: Enter goes on like the "Avanti" button, and the last step sends everything (D50)
function OnboardingWizard({ characters, rules }: OnboardingWizardProps) {
  const dispatch = useAppDispatch()
  const { step, generalError, submitStatus, failedAttempts } = useAppSelector((state) => state.onboarding)
  const isSubmitting = submitStatus === 'submitting'
  const formRef = useRef<HTMLFormElement>(null)
  const headingRef = useRef<HTMLHeadingElement>(null)
  const previousStep = useRef(step)

  // When the step changes the button that had the focus is gone: the focus goes to the title of the new step, so a
  // keyboard or screen reader user starts from there (WCAG 2.4.3). Not on the first render: the page does it.
  useEffect(() => {
    if (previousStep.current !== step) {
      previousStep.current = step
      headingRef.current?.focus()
    }
  }, [step])

  // After a failed attempt the focus goes to the first wrong field, or to the alert if no field is to blame (WCAG
  // 3.3.1). It runs after the effect above, so a failure that also changes the step ends on the field.
  useEffect(() => {
    if (failedAttempts === 0) {
      return
    }
    const form = formRef.current
    const target =
      form?.querySelector<HTMLElement>('[aria-invalid="true"]') ?? form?.querySelector<HTMLElement>('[role="alert"]')
    target?.focus()
  }, [failedAttempts])

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (isSubmitting) {
      return
    }
    if (step < LAST_STEP) {
      dispatch(advance(rules))
    } else {
      void dispatch(submit(rules, characters))
    }
  }

  return (
    <form ref={formRef} onSubmit={handleSubmit} noValidate>
      <StepProgress step={step} />
      <h2 ref={headingRef} tabIndex={-1} className="h4 mb-3">
        Passo {step} di {LAST_STEP} · {STEP_TITLES[step]}
      </h2>
      {generalError && (
        <div role="alert" tabIndex={-1} className="alert alert-danger">
          {generalError}
        </div>
      )}
      {step === 1 && <CharacterStep characters={characters} maxLevel={rules.maxLevel} />}
      {step === 2 && <ProgressStep characters={characters} rules={rules} />}
      {step === 3 && <StatusStep rules={rules} />}
      <div className="d-flex justify-content-between gap-2 mt-4">
        {step > 1 ? (
          <button
            type="button"
            className="btn btn-outline-light"
            onClick={() => dispatch(stepBack())}
            disabled={isSubmitting}
          >
            Indietro
          </button>
        ) : (
          <span />
        )}
        <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
          {step < LAST_STEP ? 'Avanti' : isSubmitting ? 'Salvataggio in corso…' : 'Completa la configurazione'}
        </button>
      </div>
    </form>
  )
}

export default OnboardingPage
