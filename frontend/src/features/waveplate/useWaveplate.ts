import { useCallback, useEffect, useRef, useState } from 'react'
import { describeError, type RequestFailure } from '../../api/client'
import { useAppSelector } from '../../app/hooks'
import type { WaveplateReading, WaveplateStatus } from './types'
import { getWaveplate, saveWaveplate } from './waveplateApi'

// The Waveplate of the panel of the calendar (D120): the server computes them (D3), the page asks again every minute
// while it is visible, and as soon as it becomes visible again. They live in the panel, not in Redux: nobody else
// reads them, and they are forgotten when the panel goes away.

const REFRESH_MS = 60_000
const NOT_FOUND = 404

export type WaveplateView =
  | { status: 'loading' }
  // No reading was ever saved
  | { status: 'missing' }
  // checkedAt is when the values arrived: "already full" is decided against it, not against a clock read while drawing
  | { status: 'ready'; data: WaveplateStatus; checkedAt: Date; stale: boolean }
  | { status: 'failed'; message: string }

const NO_SESSION: RequestFailure = { code: 'NO_SESSION', message: 'Devi accedere per aggiornare le Waveplate.', status: 0 }

export function useWaveplate() {
  const token = useAppSelector((state) => state.auth.token)
  const [view, setView] = useState<WaveplateView>({ status: 'loading' })
  // Only the answer of the last request counts: a slow old one never covers a newer value
  const latest = useRef(0)

  // The state changes only in the callbacks of the request, never while the effect that starts it runs
  const load = useCallback(() => {
    if (!token) {
      return
    }
    const request = ++latest.current
    getWaveplate(token).then(
      (data) => {
        if (request === latest.current) {
          setView({ status: 'ready', data, checkedAt: new Date(), stale: false })
        }
      },
      (error: unknown) => {
        if (request !== latest.current) {
          return
        }
        const failure = describeError(error)
        setView((current) => {
          if (failure.status === NOT_FOUND) {
            return { status: 'missing' }
          }
          // A refresh that fails keeps the last values, marked as possibly old
          return current.status === 'ready' ? { ...current, stale: true } : { status: 'failed', message: failure.message }
        })
      },
    )
  }, [token])

  useEffect(() => {
    load()
    const refreshIfVisible = () => {
      if (document.visibilityState === 'visible') {
        load()
      }
    }
    const timer = setInterval(refreshIfVisible, REFRESH_MS)
    document.addEventListener('visibilitychange', refreshIfVisible)
    return () => {
      clearInterval(timer)
      document.removeEventListener('visibilitychange', refreshIfVisible)
    }
  }, [load])

  const retry = useCallback(() => {
    setView({ status: 'loading' })
    load()
  }, [load])

  /** Saves what the user read in the game; the failure is returned for the form. */
  const save = useCallback(
    async (reading: WaveplateReading): Promise<RequestFailure | null> => {
      if (!token) {
        return NO_SESSION
      }
      try {
        const data = await saveWaveplate(token, reading)
        latest.current += 1
        setView({ status: 'ready', data, checkedAt: new Date(), stale: false })
        return null
      } catch (error) {
        return describeError(error)
      }
    },
    [token],
  )

  return { view, retry, save }
}
