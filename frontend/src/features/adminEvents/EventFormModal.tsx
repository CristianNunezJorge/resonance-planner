import { useEffect, useId, useRef, useState, type FormEvent } from 'react'
import type { RequestFailure } from '../../api/client'
import FormField from '../../components/FormField'
import Modal from '../../components/Modal'
import SelectField from '../../components/SelectField'
import { EVENT_TYPE_LABEL } from '../calendar/format'
import type { EventType, FarmingSourceType, Weekday } from '../calendar/types'
import { ALL_WEEKDAYS, draftOf, emptyDraft, toRequest } from './eventForm'
import type { ActivityDraft, AdminEvent, EventDraft, EventRequest } from './types'

interface EventFormModalProps {
  // The event to change, or null for a new one
  event: AdminEvent | null
  // The day from which a recurring event is counted (D29), null until the rules arrive
  anchorDate: string | null
  onSubmit: (request: EventRequest) => Promise<RequestFailure | null>
  onClose: () => void
}

const MAX_REWARDS = 10
const MAX_ACTIVITIES = 20

const TYPE_OPTIONS = (Object.keys(EVENT_TYPE_LABEL) as EventType[]).map((type) => ({ value: type, label: EVENT_TYPE_LABEL[type] }))
const SOURCE_OPTIONS: { value: FarmingSourceType; label: string }[] = [
  { value: 'SIMULATION_TRAINING', label: 'Simulation Training' },
  { value: 'BOSS_CHALLENGE', label: 'Boss Challenge' },
  { value: 'OVERWORLD_ENEMIES', label: 'Nemici comuni' },
  { value: 'OVERWORLD_COLLECTION', label: 'Raccolta' },
]
const FREQUENCY_OPTIONS = [
  { value: 'DAILY', label: 'Ogni giorno' },
  { value: 'ONCE', label: 'Una volta' },
]
const WEEKDAY_LABEL: Record<Weekday, string> = {
  MONDAY: 'Lunedì',
  TUESDAY: 'Martedì',
  WEDNESDAY: 'Mercoledì',
  THURSDAY: 'Giovedì',
  FRIDAY: 'Venerdì',
  SATURDAY: 'Sabato',
  SUNDAY: 'Domenica',
}
const NEW_ACTIVITY: ActivityDraft = { title: '', minutes: '15', frequency: 'ONCE', unlockDay: '' }

const anchorFormat = new Intl.DateTimeFormat('it-IT', { day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'UTC' })

/** The names the server gives to the fields that the form shows now (D114): a message for another name goes on top. */
function shownFields(draft: EventDraft): Set<string> {
  const names = ['name', 'type', 'description', 'activeWeekdays']
  names.push(
    ...(draft.scheduleType === 'FIXED'
      ? ['schedule.startDate', 'schedule.endDate']
      : ['schedule.startOffsetDays', 'schedule.durationDays', 'schedule.repeatEveryDays']),
  )
  draft.rewards.forEach((_, index) => names.push(`rewards[${index}]`))
  draft.activities.forEach((_, index) =>
    names.push(...['title', 'minutes', 'frequency', 'unlockDay'].map((field) => `activities[${index}].${field}`)),
  )
  if (draft.type === 'DOUBLE_DROP') {
    names.push('effect.sourceType', 'effect.multiplier', 'effect.dailyLimit')
  }
  return new Set(names)
}

// The form of an event (P7, D114, D122): the draft lives in this window only, and is lost when it closes. The page
// checks what it must to build the body (toRequest); the server checks the rest and its messages come back under the
// field with the same name, also inside the rows of rewards and activities.
function EventFormModal({ event, anchorDate, onSubmit, onClose }: EventFormModalProps) {
  const formId = useId()
  const [draft, setDraft] = useState<EventDraft>(() => (event ? draftOf(event) : emptyDraft()))
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [otherErrors, setOtherErrors] = useState<string[]>([])
  const [saving, setSaving] = useState(false)
  // Counts the failed saves: after each one the focus goes to the first problem (WCAG 3.3.1)
  const [attempt, setAttempt] = useState(0)
  const formRef = useRef<HTMLFormElement>(null)
  const nameRef = useRef<HTMLInputElement>(null)
  const alertRef = useRef<HTMLDivElement>(null)
  const addRewardRef = useRef<HTMLButtonElement>(null)
  const addActivityRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    if (attempt === 0) {
      return
    }
    const firstWrong = formRef.current?.querySelector<HTMLElement>('[aria-invalid="true"]')
    ;(firstWrong ?? alertRef.current)?.focus()
  }, [attempt])

  const update = (changes: Partial<EventDraft>) => setDraft((current) => ({ ...current, ...changes }))
  const updateActivity = (index: number, changes: Partial<ActivityDraft>) =>
    setDraft((current) => ({
      ...current,
      activities: current.activities.map((activity, at) => (at === index ? { ...activity, ...changes } : activity)),
    }))
  const updateEffect = (changes: Partial<EventDraft['effect']>) =>
    setDraft((current) => ({ ...current, effect: { ...current.effect, ...changes } }))
  // A removed row takes away the errors of its group: they were for rows that now have another index. The focus
  // goes to the button that adds a row, instead of falling on the page behind with the removed button
  const removeRow = (group: 'rewards' | 'activities', index: number) => {
    setDraft((current) =>
      group === 'rewards'
        ? { ...current, rewards: current.rewards.filter((_, at) => at !== index) }
        : { ...current, activities: current.activities.filter((_, at) => at !== index) },
    )
    setErrors((current) => Object.fromEntries(Object.entries(current).filter(([name]) => !name.startsWith(`${group}[`))))
    ;(group === 'rewards' ? addRewardRef : addActivityRef).current?.focus()
  }
  const toggleWeekday = (day: Weekday) =>
    setDraft((current) => ({
      ...current,
      weekdays: current.weekdays.includes(day) ? current.weekdays.filter((item) => item !== day) : [...current.weekdays, day],
    }))

  const handleSubmit = async (submitEvent: FormEvent<HTMLFormElement>) => {
    submitEvent.preventDefault()
    if (saving) {
      return
    }
    const result = toRequest(draft)
    if ('errors' in result) {
      setErrors(result.errors)
      setOtherErrors([])
      setAttempt((count) => count + 1)
      return
    }
    setSaving(true)
    const failure = await onSubmit(result.request)
    if (!failure) {
      onClose()
      return
    }
    setSaving(false)
    const fields = Object.entries(failure.fields ?? {})
    const shown = shownFields(draft)
    setErrors(Object.fromEntries(fields.filter(([name]) => shown.has(name))))
    setOtherErrors(
      fields.length === 0 ? [failure.message] : fields.filter(([name]) => !shown.has(name)).map(([, message]) => message),
    )
    setAttempt((count) => count + 1)
  }

  const weekdaysErrorId = 'event-weekdays-error'
  const descriptionErrorId = 'event-description-error'
  const anchorText = anchorDate ? anchorFormat.format(new Date(`${anchorDate}T00:00:00Z`)) : null

  return (
    <Modal
      open
      size="lg"
      title={event ? `Modifica «${event.name}»` : 'Nuovo evento'}
      busy={saving}
      onClose={onClose}
      initialFocusRef={nameRef}
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
      <form id={formId} ref={formRef} noValidate onSubmit={(submitEvent) => void handleSubmit(submitEvent)}>
        {otherErrors.length > 0 && (
          <div ref={alertRef} className="alert alert-danger" role="alert" tabIndex={-1}>
            {otherErrors.length === 1 ? (
              otherErrors[0]
            ) : (
              <ul className="mb-0 ps-3">
                {otherErrors.map((message) => (
                  <li key={message}>{message}</li>
                ))}
              </ul>
            )}
          </div>
        )}
        {/* While the event is saved nothing can change: the body was built from the form as it was */}
        <fieldset disabled={saving}>
        <FormField
          id="event-name"
          label="Nome"
          value={draft.name}
          onChange={(name) => update({ name })}
          required
          error={errors.name}
          inputRef={nameRef}
        />
        <SelectField
          id="event-type"
          label="Tipo"
          value={draft.type}
          onChange={(type) => update({ type: type as EventType })}
          options={TYPE_OPTIONS}
          error={errors.type}
        />
        <div className="mb-3">
          <label htmlFor="event-description" className="form-label">
            Descrizione
          </label>
          <textarea
            id="event-description"
            className={errors.description ? 'form-control is-invalid' : 'form-control'}
            rows={3}
            value={draft.description}
            onChange={(changeEvent) => update({ description: changeEvent.target.value })}
            aria-invalid={errors.description ? true : undefined}
            aria-describedby={errors.description ? descriptionErrorId : undefined}
          />
          {errors.description && (
            <div id={descriptionErrorId} className="invalid-feedback">
              {errors.description}
            </div>
          )}
        </div>

        <fieldset className="mb-3">
          <legend className="fs-6 fw-semibold">Date</legend>
          {(['FIXED', 'RECURRING'] as const).map((type) => (
            <div className="form-check form-check-inline" key={type}>
              <input
                id={`event-schedule-${type}`}
                type="radio"
                name="event-schedule"
                className="form-check-input"
                checked={draft.scheduleType === type}
                onChange={() => update({ scheduleType: type })}
              />
              <label htmlFor={`event-schedule-${type}`} className="form-check-label">
                {type === 'FIXED' ? 'Date fisse' : 'Si ripete'}
              </label>
            </div>
          ))}
          <div className="row g-3 mt-0">
            {draft.scheduleType === 'FIXED' ? (
              <>
                <div className="col-sm-6">
                  <FormField
                    id="event-start-date"
                    label="Primo giorno"
                    type="date"
                    value={draft.startDate}
                    onChange={(startDate) => update({ startDate })}
                    error={errors['schedule.startDate']}
                  />
                </div>
                <div className="col-sm-6">
                  <FormField
                    id="event-end-date"
                    label="Ultimo giorno"
                    type="date"
                    value={draft.endDate}
                    onChange={(endDate) => update({ endDate })}
                    error={errors['schedule.endDate']}
                  />
                </div>
              </>
            ) : (
              <>
                <div className="col-sm-4">
                  <FormField
                    id="event-offset"
                    label="Inizio, in giorni"
                    inputMode="numeric"
                    value={draft.startOffsetDays}
                    onChange={(startOffsetDays) => update({ startOffsetDays })}
                    hint={`Contati dal ${anchorText ?? 'giorno di riferimento della demo'}; anche negativi.`}
                    error={errors['schedule.startOffsetDays']}
                  />
                </div>
                <div className="col-sm-4">
                  <FormField
                    id="event-duration"
                    label="Durata in giorni"
                    inputMode="numeric"
                    value={draft.durationDays}
                    onChange={(durationDays) => update({ durationDays })}
                    hint="Da 1 a 366."
                    error={errors['schedule.durationDays']}
                  />
                </div>
                <div className="col-sm-4">
                  <FormField
                    id="event-repeat"
                    label="Si ripete ogni (giorni)"
                    inputMode="numeric"
                    value={draft.repeatEveryDays}
                    onChange={(repeatEveryDays) => update({ repeatEveryDays })}
                    hint="Almeno quanto la durata."
                    error={errors['schedule.repeatEveryDays']}
                  />
                </div>
              </>
            )}
          </div>
        </fieldset>

        <fieldset className="mb-3">
          <legend className="fs-6 fw-semibold">Giorni in cui è attivo</legend>
          <p className="form-text mt-0">Tutti spuntati vuol dire tutti i giorni.</p>
          {ALL_WEEKDAYS.map((day) => (
            <div className="form-check form-check-inline" key={day}>
              <input
                id={`event-day-${day}`}
                type="checkbox"
                className="form-check-input"
                checked={draft.weekdays.includes(day)}
                onChange={() => toggleWeekday(day)}
                aria-invalid={errors.activeWeekdays ? true : undefined}
                aria-describedby={errors.activeWeekdays ? weekdaysErrorId : undefined}
              />
              <label htmlFor={`event-day-${day}`} className="form-check-label">
                {WEEKDAY_LABEL[day]}
              </label>
            </div>
          ))}
          {errors.activeWeekdays && (
            <p id={weekdaysErrorId} className="text-danger-emphasis small mt-1 mb-0">
              {errors.activeWeekdays}
            </p>
          )}
        </fieldset>

        <fieldset className="mb-3">
          <legend className="fs-6 fw-semibold">Ricompense</legend>
          {draft.rewards.map((reward, index) => (
            <div className="d-flex gap-2 align-items-start" key={index}>
              <div className="flex-grow-1">
                <FormField
                  id={`event-reward-${index}`}
                  label={`Ricompensa ${index + 1}`}
                  value={reward}
                  onChange={(text) =>
                    setDraft((current) => ({
                      ...current,
                      rewards: current.rewards.map((item, at) => (at === index ? text : item)),
                    }))
                  }
                  error={errors[`rewards[${index}]`]}
                />
              </div>
              <button
                type="button"
                className="btn btn-sm btn-outline-danger mt-4"
                aria-label={`Togli la ricompensa ${index + 1}`}
                onClick={() => removeRow('rewards', index)}
              >
                Togli
              </button>
            </div>
          ))}
          <button
            ref={addRewardRef}
            type="button"
            className="btn btn-sm btn-outline-light"
            disabled={draft.rewards.length >= MAX_REWARDS}
            onClick={() => setDraft((current) => ({ ...current, rewards: [...current.rewards, ''] }))}
          >
            + Ricompensa
          </button>
        </fieldset>

        <fieldset className="mb-3">
          <legend className="fs-6 fw-semibold">Attività dell’evento</legend>
          {draft.activities.map((activity, index) => (
            <div className="border rounded p-3 mb-2" key={index}>
              <div className="d-flex align-items-center mb-2">
                <h3 className="h6 mb-0 me-auto">{`Attività ${index + 1}`}</h3>
                <button
                  type="button"
                  className="btn btn-sm btn-outline-danger"
                  aria-label={`Togli l’attività ${index + 1}`}
                  onClick={() => removeRow('activities', index)}
                >
                  Togli
                </button>
              </div>
              <FormField
                id={`event-activity-${index}-title`}
                label="Titolo"
                value={activity.title}
                onChange={(title) => updateActivity(index, { title })}
                error={errors[`activities[${index}].title`]}
              />
              <div className="row g-3">
                <div className="col-sm-4">
                  <FormField
                    id={`event-activity-${index}-minutes`}
                    label="Minuti"
                    inputMode="numeric"
                    value={activity.minutes}
                    onChange={(minutes) => updateActivity(index, { minutes })}
                    error={errors[`activities[${index}].minutes`]}
                  />
                </div>
                <div className="col-sm-4">
                  <SelectField
                    id={`event-activity-${index}-frequency`}
                    label="Frequenza"
                    value={activity.frequency}
                    onChange={(frequency) => updateActivity(index, { frequency: frequency as ActivityDraft['frequency'] })}
                    options={FREQUENCY_OPTIONS}
                    error={errors[`activities[${index}].frequency`]}
                  />
                </div>
                <div className="col-sm-4">
                  <FormField
                    id={`event-activity-${index}-unlock`}
                    label="Dal giorno"
                    inputMode="numeric"
                    value={activity.unlockDay}
                    onChange={(unlockDay) => updateActivity(index, { unlockDay })}
                    hint="Vuoto = dal primo giorno."
                    error={errors[`activities[${index}].unlockDay`]}
                  />
                </div>
              </div>
            </div>
          ))}
          <button
            ref={addActivityRef}
            type="button"
            className="btn btn-sm btn-outline-light"
            disabled={draft.activities.length >= MAX_ACTIVITIES}
            onClick={() =>
              setDraft((current) => ({ ...current, activities: [...current.activities, { ...NEW_ACTIVITY }] }))
            }
          >
            + Attività
          </button>
        </fieldset>

        {draft.type === 'DOUBLE_DROP' && (
          <fieldset className="mb-0">
            <legend className="fs-6 fw-semibold">Ricompense moltiplicate</legend>
            <div className="row g-3">
              <div className="col-sm-4">
                <SelectField
                  id="event-effect-source"
                  label="Tipo di fonte"
                  value={draft.effect.sourceType}
                  onChange={(sourceType) => updateEffect({ sourceType: sourceType as FarmingSourceType })}
                  options={SOURCE_OPTIONS}
                  error={errors['effect.sourceType']}
                />
              </div>
              <div className="col-sm-4">
                <FormField
                  id="event-effect-multiplier"
                  label="Moltiplicatore"
                  inputMode="text"
                  value={draft.effect.multiplier}
                  onChange={(multiplier) => updateEffect({ multiplier })}
                  hint="Più di 1, al massimo 10: per esempio 2 o 1,5."
                  error={errors['effect.multiplier']}
                />
              </div>
              <div className="col-sm-4">
                <FormField
                  id="event-effect-limit"
                  label="Run al giorno"
                  inputMode="numeric"
                  value={draft.effect.dailyLimit}
                  onChange={(dailyLimit) => updateEffect({ dailyLimit })}
                  hint="Da 1 a 100. Vuoto = tutte."
                  error={errors['effect.dailyLimit']}
                />
              </div>
            </div>
          </fieldset>
        )}
        </fieldset>
      </form>
    </Modal>
  )
}

export default EventFormModal
