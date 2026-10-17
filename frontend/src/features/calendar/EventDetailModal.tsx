import Modal from '../../components/Modal'
import { activityText, editionText, effectText, EVENT_TYPE_LABEL, weekdaysText } from './format'
import type { GameEvent } from './types'

interface EventDetailModalProps {
  event: GameEvent
  onClose: () => void
  // "Spiegamelo" sends a question about the event to the agent (D130); disabled while the agent cannot answer
  onExplain: () => void
  canExplain: boolean
}

// "Dettaglio evento" (P4): what the event is, when it runs and on which days (D96), its effect with the daily limit
// (D94), the rewards and the activities with their day of unlock; "Spiegamelo" asks the agent about it (D130)
function EventDetailModal({ event, onClose, onExplain, canExplain }: EventDetailModalProps) {
  return (
    <Modal
      open
      title={event.name}
      onClose={onClose}
      footer={
        <>
          <button type="button" className="btn btn-outline-light" onClick={onClose}>
            Chiudi
          </button>
          <button type="button" className="btn btn-primary" onClick={onExplain} disabled={!canExplain}>
            Spiegamelo
          </button>
        </>
      }
    >
      <p>
        <span className={`badge calendar-badge calendar-badge--${event.type.toLowerCase().replace('_', '-')}`}>
          {EVENT_TYPE_LABEL[event.type]}
        </span>
      </p>
      <p>{event.description}</p>
      <dl className="mb-0">
        <dt>Date</dt>
        <dd>{editionText(event.startDate, event.endDate)}</dd>
        <dt>Giorni attivi</dt>
        <dd>{weekdaysText(event.activeWeekdays)}</dd>
        {event.effect && (
          <>
            <dt>Effetto</dt>
            <dd>{effectText(event.effect)}</dd>
          </>
        )}
        {event.rewards.length > 0 && (
          <>
            <dt>Ricompense</dt>
            <dd>
              <ul className="mb-0 ps-3">
                {event.rewards.map((reward) => (
                  <li key={reward}>{reward}</li>
                ))}
              </ul>
            </dd>
          </>
        )}
        {event.activities.length > 0 && (
          <>
            <dt>Attività dell'evento</dt>
            <dd className="mb-0">
              <ul className="mb-0 ps-3">
                {event.activities.map((activity) => (
                  <li key={activity.title}>{activityText(activity)}</li>
                ))}
              </ul>
            </dd>
          </>
        )}
      </dl>
    </Modal>
  )
}

export default EventDetailModal
