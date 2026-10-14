import type { Step } from './types'

const STEP_COUNT = 3

interface StepProgressProps {
  step: Step
}

// The progress of the three steps. A native <progress> is announced by screen readers with its value, and needs no
// inline style (D103); the text "Passo 2 di 3" next to it, in the title of the step, says the same without the colour.
function StepProgress({ step }: StepProgressProps) {
  return (
    <progress
      className="onboarding-progress mb-3"
      value={step}
      max={STEP_COUNT}
      aria-label="Avanzamento della configurazione"
    />
  )
}

export default StepProgress
