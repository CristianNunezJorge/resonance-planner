import { useEffect, useState } from 'react'

// After this delay the free backend is probably waking up (D82)
const SLOW_NOTICE_DELAY_MS = 5000

interface LoadingStateProps {
  slowNoticeDelayMs?: number
}

// Loading state of every page (D69); role="status" lets screen readers announce it (D70)
function LoadingState({ slowNoticeDelayMs = SLOW_NOTICE_DELAY_MS }: LoadingStateProps) {
  const [isSlow, setIsSlow] = useState(false)

  useEffect(() => {
    const timerId = setTimeout(() => setIsSlow(true), slowNoticeDelayMs)
    return () => clearTimeout(timerId)
  }, [slowNoticeDelayMs])

  return (
    <div className="text-center py-4" role="status">
      <div className="spinner-border text-primary" aria-hidden="true" />
      <p className="mt-2 mb-0">Caricamento…</p>
      {isSlow && (
        <p className="small text-body-secondary mt-1 mb-0">
          Il server si sta avviando: può volerci fino a tre minuti.
        </p>
      )}
    </div>
  )
}

export default LoadingState
