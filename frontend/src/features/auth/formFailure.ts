import { useEffect, useEffectEvent, type RefObject } from 'react'
import type { RequestFailure } from '../../api/client'

type FieldRef = [name: string, ref: RefObject<HTMLInputElement | null>]

/**
 * The message for the alert above the form: the errors of the fields are shown under their input, so the alert
 * is only for the others (wrong credentials, too many requests, server not reachable).
 */
export function generalErrorMessage(failure: RequestFailure | null, fieldNames: string[]): string | null {
  if (!failure) {
    return null
  }
  const hasFieldError = fieldNames.some((name) => failure.fields?.[name])
  return hasFieldError ? null : failure.message
}

/** After a failed submit the keyboard focus goes to the first wrong field, or to the alert (WCAG 3.3.1). */
export function useFocusOnFailure(
  failure: RequestFailure | null,
  fieldRefs: FieldRef[],
  alertRef: RefObject<HTMLElement | null>,
): void {
  // An effect event reads the latest refs without being a dependency: the focus moves only when a new failure arrives
  const focusProblem = useEffectEvent((current: RequestFailure) => {
    const firstWrongField = fieldRefs.find(([name]) => current.fields?.[name])
    ;(firstWrongField?.[1] ?? alertRef).current?.focus()
  })

  useEffect(() => {
    if (failure) {
      focusProblem(failure)
    }
  }, [failure])
}
