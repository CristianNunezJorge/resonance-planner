import { formatPlayTime } from '../progress/levels'
import { formatDays, formatNumber } from './format'
import type { FarmingPlan } from './types'

interface FarmingSummaryProps {
  plan: FarmingPlan
}

// The estimate for the whole roster (D112): Waveplate, minutes and days, then the runs of every source. The plan of
// the days, with the events, is the work of the planner (D57): here it is only an estimate, and it says so.
function FarmingSummary({ plan }: FarmingSummaryProps) {
  const { waveplates, minutes, estimatedDays } = plan.totals
  const nothingToFarm = plan.sources.length === 0
  const parts = [
    `~${formatNumber(waveplates)} Waveplate`,
    `~${formatPlayTime(minutes)}`,
    estimatedDays === null ? null : `~${formatDays(estimatedDays)}`,
  ].filter((part): part is string => part !== null)

  return (
    <section className="card mt-4" aria-labelledby="farming-summary-title">
      <div className="card-body">
        <h2 id="farming-summary-title" className="h5">
          Totale stimato per tutto il roster
        </h2>
        {nothingToFarm ? (
          <p className="mb-0">Hai già tutto quello che serve per i tuoi obiettivi.</p>
        ) : (
          <>
            <p className="fs-5 mb-2">{parts.join(' · ')}</p>
            {plan.containsEstimates && (
              <p className="small text-body-secondary">
                <span className="badge text-bg-secondary">stima</span> Alcuni drop sono stimati: le quantità possono
                cambiare un po' in gioco.
              </p>
            )}
            <details>
              <summary>Le run da fare</summary>
              <ul className="mt-2 mb-0">
                {plan.sources.map((source) => (
                  <li key={source.farmingSourceId}>
                    {source.name}: {source.runs} run · {formatNumber(source.waveplates)} Waveplate ·{' '}
                    {formatPlayTime(source.minutes)}
                  </li>
                ))}
              </ul>
            </details>
          </>
        )}
      </div>
    </section>
  )
}

export default FarmingSummary
