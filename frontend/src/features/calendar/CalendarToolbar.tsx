import type { CalendarViewName } from './CalendarView'

interface CalendarToolbarProps {
  title: string
  view: CalendarViewName
  // On a phone there is only the list by day (D67)
  canChangeView: boolean
  onPrevious: () => void
  onToday: () => void
  onNext: () => void
  onViewChange: (view: CalendarViewName) => void
  onAddTask: () => void
  // The basic plan without AI (D64), disabled while a request on the plan is running
  onGeneratePlan: () => void
  isPlanning: boolean
}

const VIEWS: { view: CalendarViewName; label: string }[] = [
  { view: 'dayGridWeek', label: 'Settimana' },
  { view: 'dayGridMonth', label: 'Mese' },
]

// The toolbar of the calendar (P4), in Italian and with Bootstrap buttons, instead of the one of FullCalendar
function CalendarToolbar({
  title,
  view,
  canChangeView,
  onPrevious,
  onToday,
  onNext,
  onViewChange,
  onAddTask,
  onGeneratePlan,
  isPlanning,
}: CalendarToolbarProps) {
  return (
    <div className="d-flex flex-wrap align-items-center gap-2 mb-3">
      <div className="btn-group" role="group" aria-label="Periodo">
        <button type="button" className="btn btn-outline-light" onClick={onPrevious} aria-label="Periodo precedente">
          ‹
        </button>
        <button type="button" className="btn btn-outline-light" onClick={onToday}>
          Oggi
        </button>
        <button type="button" className="btn btn-outline-light" onClick={onNext} aria-label="Periodo successivo">
          ›
        </button>
      </div>
      {/* Announced when the period changes, so a screen reader knows where the calendar went; the space stays
          even before the first title, so the buttons do not jump */}
      <div className="me-auto" aria-live="polite">
        {title && <h2 className="h5 mb-0">{title}</h2>}
      </div>
      {canChangeView && (
        <div className="btn-group" role="group" aria-label="Vista">
          {VIEWS.map((item) => (
            <button
              key={item.view}
              type="button"
              className={item.view === view ? 'btn btn-light' : 'btn btn-outline-light'}
              aria-pressed={item.view === view}
              onClick={() => onViewChange(item.view)}
            >
              {item.label}
            </button>
          ))}
        </div>
      )}
      <button type="button" className="btn btn-outline-light" onClick={onGeneratePlan} disabled={isPlanning}>
        Genera piano base
      </button>
      <button type="button" className="btn btn-primary" onClick={onAddTask}>
        + Attività
      </button>
    </div>
  )
}

export default CalendarToolbar
