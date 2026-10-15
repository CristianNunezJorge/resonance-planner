import { useId, useRef, useState, type FormEvent, type RefObject } from 'react'
import type { RequestFailure } from '../../api/client'
import FormField from '../../components/FormField'
import Modal from '../../components/Modal'
import type { CalendarTask, TaskForm } from './types'

interface TaskFormModalProps {
  // The task to change, or null for a new one
  task: CalendarTask | null
  // The day proposed for a new task
  defaultDay: string
  onSubmit: (form: TaskForm) => Promise<RequestFailure | null>
  onClose: () => void
}

type FieldName = 'title' | 'taskDate' | 'minutes' | 'notes'

const FIELD_ORDER: FieldName[] = ['title', 'taskDate', 'minutes', 'notes']
const MAX_MINUTES = 1440
const WHOLE_NUMBER = /^\d+$/
const NOTES_ID = 'task-notes'

// "Attività manuale" (F9, D117): title, day, duration and notes. The page checks only what it must to send a valid
// request (a day, a whole number of minutes); the other rules are the server's, and its messages come back under the
// right field (D103).
function TaskFormModal({ task, defaultDay, onSubmit, onClose }: TaskFormModalProps) {
  const formId = useId()
  const [title, setTitle] = useState(task?.title ?? '')
  const [taskDate, setTaskDate] = useState(task?.taskDate ?? defaultDay)
  const [minutes, setMinutes] = useState(task ? String(task.minutes) : '30')
  const [notes, setNotes] = useState(task?.notes ?? '')
  const [errors, setErrors] = useState<Partial<Record<FieldName, string>>>({})
  const [generalError, setGeneralError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const titleRef = useRef<HTMLInputElement>(null)
  const taskDateRef = useRef<HTMLInputElement>(null)
  const minutesRef = useRef<HTMLInputElement>(null)
  const notesRef = useRef<HTMLTextAreaElement>(null)

  // Read only in the handlers, never while drawing
  const fieldRef = (name: FieldName): RefObject<HTMLInputElement | HTMLTextAreaElement | null> =>
    ({ title: titleRef, taskDate: taskDateRef, minutes: minutesRef, notes: notesRef })[name]

  // The keyboard goes to the first wrong field (WCAG 3.3.1)
  const showErrors = (found: Partial<Record<FieldName, string>>) => {
    setErrors(found)
    const first = FIELD_ORDER.find((name) => found[name])
    if (first) {
      fieldRef(first).current?.focus()
    }
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (saving) {
      return
    }
    setGeneralError(null)
    const found: Partial<Record<FieldName, string>> = {}
    if (taskDate === '') {
      found.taskDate = 'Scegli il giorno.'
    }
    const minutesText = minutes.trim()
    if (!WHOLE_NUMBER.test(minutesText) || Number(minutesText) < 1 || Number(minutesText) > MAX_MINUTES) {
      found.minutes = `Inserisci un numero intero di minuti, da 1 a ${MAX_MINUTES}.`
    }
    if (Object.keys(found).length > 0) {
      showErrors(found)
      return
    }
    setSaving(true)
    const failure = await onSubmit({ title, taskDate, minutes: Number(minutesText), notes })
    if (!failure) {
      onClose()
      return
    }
    setSaving(false)
    const fromServer = Object.fromEntries(
      FIELD_ORDER.filter((name) => failure.fields?.[name]).map((name) => [name, failure.fields?.[name]]),
    ) as Partial<Record<FieldName, string>>
    if (Object.keys(fromServer).length > 0) {
      showErrors(fromServer)
    } else {
      setErrors({})
      setGeneralError(failure.message)
    }
  }

  const notesDescribedBy = errors.notes ? `${NOTES_ID}-error` : undefined

  return (
    <Modal
      open
      title={task ? 'Modifica attività' : 'Nuova attività'}
      busy={saving}
      onClose={onClose}
      initialFocusRef={titleRef}
      footer={
        <>
          <button
            type="button"
            className="btn btn-outline-light"
            onClick={saving ? undefined : onClose}
            aria-disabled={saving || undefined}
          >
            Annulla
          </button>
          <button type="submit" form={formId} className="btn btn-primary" aria-disabled={saving || undefined}>
            {saving ? 'Salvataggio…' : 'Salva'}
          </button>
        </>
      }
    >
      <form id={formId} noValidate onSubmit={(event) => void handleSubmit(event)}>
        {generalError && (
          <div className="alert alert-danger" role="alert">
            {generalError}
          </div>
        )}
        <FormField
          id="task-title"
          label="Titolo"
          value={title}
          onChange={setTitle}
          required
          error={errors.title}
          inputRef={titleRef}
        />
        <FormField
          id="task-date"
          label="Giorno"
          type="date"
          value={taskDate}
          onChange={setTaskDate}
          required
          error={errors.taskDate}
          inputRef={taskDateRef}
        />
        <FormField
          id="task-minutes"
          label="Durata in minuti"
          value={minutes}
          onChange={setMinutes}
          inputMode="numeric"
          required
          hint={`Da 1 a ${MAX_MINUTES} minuti.`}
          error={errors.minutes}
          inputRef={minutesRef}
        />
        <div className="mb-0">
          <label htmlFor={NOTES_ID} className="form-label">
            Note (facoltative)
          </label>
          <textarea
            id={NOTES_ID}
            ref={notesRef}
            className={errors.notes ? 'form-control is-invalid' : 'form-control'}
            rows={3}
            value={notes}
            onChange={(event) => setNotes(event.target.value)}
            aria-invalid={errors.notes ? true : undefined}
            aria-describedby={notesDescribedBy}
          />
          {errors.notes && (
            <div id={`${NOTES_ID}-error`} className="invalid-feedback">
              {errors.notes}
            </div>
          )}
        </div>
      </form>
    </Modal>
  )
}

export default TaskFormModal
