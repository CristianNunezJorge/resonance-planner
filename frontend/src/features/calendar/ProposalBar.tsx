import { shortDayText } from './format'
import type { Proposal } from './types'

interface ProposalBarProps {
  proposal: Proposal
  // A request is running: the buttons wait for it
  busy: boolean
  onConfirm: () => void
  onReject: () => void
}

// The pending proposal over the calendar (D68, F6): what the plan is, and Conferma or Scarta, which act at once.
// The summary is shown as text, never as HTML: with the agent it is written by the model.
function ProposalBar({ proposal, busy, onConfirm, onReject }: ProposalBarProps) {
  const days = proposal.tasks.map((task) => task.taskDate).sort()
  const count = proposal.tasks.length
  const first = days[0]
  const last = days[days.length - 1]
  let span = ''
  if (first && last) {
    span = first === last ? ` il ${shortDayText(first)}` : ` dal ${shortDayText(first)} al ${shortDayText(last)}`
  }

  return (
    <section className="proposal-bar card mb-3" aria-labelledby="proposal-bar-title">
      <div className="card-body d-flex flex-wrap align-items-center gap-3">
        <div className="me-auto">
          <h2 id="proposal-bar-title" className="h6 text-uppercase mb-1">
            Proposta in attesa
          </h2>
          {proposal.summary && <p className="mb-1">{proposal.summary}</p>}
          {/* "attività" is the same in the singular and in the plural */}
          <p className="small text-body-secondary mb-0">{`${count} attività${span}`}</p>
        </div>
        <div className="d-flex gap-2">
          <button type="button" className="btn btn-outline-light" onClick={onReject} disabled={busy}>
            Scarta
          </button>
          <button type="button" className="btn btn-primary" onClick={onConfirm} disabled={busy}>
            Conferma
          </button>
        </div>
      </div>
    </section>
  )
}

export default ProposalBar
