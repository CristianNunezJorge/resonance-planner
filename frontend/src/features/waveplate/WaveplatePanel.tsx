import { useId, useState } from 'react'
import { useAppDispatch } from '../../app/hooks'
import { toastShown } from '../../app/toastSlice'
import ErrorState from '../../components/ErrorState'
import LoadingState from '../../components/LoadingState'
import type { GameRules } from '../catalog/types'
import { fullAtText } from './format'
import type { WaveplateReading } from './types'
import { useWaveplate } from './useWaveplate'
import WaveplateModal from './WaveplateModal'

// The Waveplate panel of the calendar (P4, F5): the values the server computes and when they will be full, refreshed
// every minute; "Aggiorna" saves the values read in the game.
function WaveplatePanel({ rules }: { rules: GameRules }) {
  const titleId = useId()
  const dispatch = useAppDispatch()
  const { view, retry, save } = useWaveplate()
  const [editing, setEditing] = useState(false)

  const handleSave = async (reading: WaveplateReading) => {
    const failure = await save(reading)
    if (!failure) {
      dispatch(toastShown('Waveplate aggiornate.'))
    }
    return failure
  }

  const initial = view.status === 'ready' ? { value: view.data.value, crystals: view.data.crystals } : null

  return (
    <section className="card mb-3" aria-labelledby={titleId}>
      <div className="card-body">
        <h2 id={titleId} className="h6 text-uppercase text-body-secondary">
          Waveplate
        </h2>
        {view.status === 'loading' && <LoadingState />}
        {view.status === 'failed' && <ErrorState message={view.message} onRetry={retry} />}
        {view.status === 'missing' && (
          <>
            <p>Non hai ancora inserito le tue Waveplate.</p>
            <button type="button" className="btn btn-sm btn-primary" onClick={() => setEditing(true)}>
              Inserisci
            </button>
          </>
        )}
        {view.status === 'ready' && (
          <>
            <p className="fs-3 fw-semibold mb-0">{`${view.data.value} / ${view.data.max}`}</p>
            <p className="mb-1">{fullAtText(view.data.fullAt, view.checkedAt)}</p>
            <p className="small text-body-secondary">{`Crystals ${view.data.crystals} / ${view.data.crystalsMax}`}</p>
            {view.stale && <p className="small text-warning-emphasis">Valori non aggiornati: il server non risponde.</p>}
            <button type="button" className="btn btn-sm btn-outline-light" onClick={() => setEditing(true)}>
              Aggiorna
            </button>
          </>
        )}
      </div>
      {editing && (
        <WaveplateModal rules={rules} initial={initial} onSave={handleSave} onClose={() => setEditing(false)} />
      )}
    </section>
  )
}

export default WaveplatePanel
