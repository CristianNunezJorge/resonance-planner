import { useRef } from 'react'
import Modal from '../../components/Modal'
import { formatPlayTime } from '../progress/levels'
import { formatNumber } from '../roster/format'
import { dayText } from './format'
import type { CalendarTask } from './types'

interface TaskDetailModalProps {
  task: CalendarTask
  // The name of the character the task is for, if any
  characterName: string | null
  onClose: () => void
  onComplete: () => void
  onEdit: () => void
  onDelete: () => void
}

// "Dettaglio attività" (P4, F7, F9): day, duration, Waveplate and character; "Segna come fatta" for a task still to
// do, and Modifica and Elimina only for the tasks the user wrote (D117)
function TaskDetailModal({ task, characterName, onClose, onComplete, onEdit, onDelete }: TaskDetailModalProps) {
  const closeRef = useRef<HTMLButtonElement>(null)
  const isManual = task.origin === 'MANUAL'
  const isDone = task.status === 'DONE'

  return (
    <Modal
      open
      title={task.title}
      onClose={onClose}
      initialFocusRef={closeRef}
      footer={
        <>
          {isManual && (
            <>
              <button type="button" className="btn btn-outline-danger me-auto" onClick={onDelete}>
                Elimina
              </button>
              <button type="button" className="btn btn-outline-light" onClick={onEdit}>
                Modifica
              </button>
            </>
          )}
          {task.status === 'CONFIRMED' && (
            <button type="button" className="btn btn-primary" onClick={onComplete}>
              Segna come fatta
            </button>
          )}
          <button ref={closeRef} type="button" className="btn btn-outline-light" onClick={onClose}>
            Chiudi
          </button>
        </>
      }
    >
      {task.status === 'PROPOSED' && (
        <p>Fa parte della proposta in attesa: confermala per aggiungerla al calendario.</p>
      )}
      <dl className="mb-0">
        <dt>Giorno</dt>
        <dd>{dayText(task.taskDate)}</dd>
        <dt>Durata</dt>
        <dd>{formatPlayTime(task.minutes)}</dd>
        {task.runs !== null && (
          <>
            <dt>Run</dt>
            <dd>{task.runs}</dd>
          </>
        )}
        {task.waveplates > 0 && (
          <>
            <dt>Waveplate</dt>
            <dd>{formatNumber(task.waveplates)}</dd>
          </>
        )}
        {characterName && (
          <>
            <dt>Personaggio</dt>
            <dd>{characterName}</dd>
          </>
        )}
        <dt>Stato</dt>
        <dd>{isDone ? 'Fatta ✓' : 'Da fare'}</dd>
        <dt>Origine</dt>
        <dd>{isManual ? 'Creata da te' : 'Dal piano dell’agente'}</dd>
        {task.notes && (
          <>
            <dt>Note</dt>
            <dd className="mb-0 text-break">{task.notes}</dd>
          </>
        )}
      </dl>
    </Modal>
  )
}

export default TaskDetailModal
