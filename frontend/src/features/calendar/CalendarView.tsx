import bootstrapTheme from '@fullcalendar/bootstrap5'
import '@fullcalendar/bootstrap5/theme.css'
import FullCalendar, {
  type CalendarController,
  type DatesSetInfo,
  type DayCellInfo,
  type DayHeaderInfo,
  type EventClickInfo,
  type EventInput,
  type ListDayHeaderInfo,
} from '@fullcalendar/react'
import dayGridPlugin from '@fullcalendar/react/daygrid'
import listPlugin from '@fullcalendar/react/list'
import italian from '@fullcalendar/react/locales/it'
import '@fullcalendar/react/skeleton.css'
import 'bootstrap-icons/font/bootstrap-icons.css'
import { isItemProps, rangeOf, type ItemProps } from './calendarItems'
import type { DateRange } from './types'

export type CalendarViewName = 'dayGridWeek' | 'dayGridMonth' | 'listWeek'

const PLUGINS = [bootstrapTheme, dayGridPlugin, listPlugin]
// The events above the tasks of the same day, then by day and title
const ORDER = ['rank', 'start', 'title']

// FullCalendar 7 has no fixed class names: the classes of the terminal look (D139) come from these options, and
// _calendar.scss styles them. Today has its own class in the week, the month and the list of the phone
const dayHeaderClass = ({ isToday }: DayHeaderInfo) =>
  isToday ? 'calendar-day-header calendar-day-header--today' : 'calendar-day-header'
const dayCellClass = ({ isToday }: DayCellInfo) => (isToday ? 'calendar-day calendar-day--today' : 'calendar-day')
const listDayHeaderClass = ({ isToday }: ListDayHeaderInfo) =>
  isToday ? 'calendar-list-day calendar-list-day--today' : 'calendar-list-day'

interface CalendarViewProps {
  controller: CalendarController
  view: CalendarViewName
  items: EventInput[]
  // The days on the screen, the last one included, and the title of the period ("ottobre 2026")
  onRangeChange: (range: DateRange, title: string) => void
  onItemClick: (item: ItemProps) => void
}

// The only component that knows FullCalendar (D119): version 7 with the Bootstrap 5 theme, whole days only (D67),
// in Italian with weeks from Monday. Its styles are plain CSS files bundled by Vite, so the Content-Security-Policy
// stays as it is (D103). The toolbar is the page's own, which drives the calendar through the controller.
function CalendarView({ controller, view, items, onRangeChange, onItemClick }: CalendarViewProps) {
  const handleDatesSet = (info: DatesSetInfo) => {
    onRangeChange(rangeOf(info.start, info.end), info.view.title)
  }

  const handleClick = (info: EventClickInfo) => {
    info.jsEvent.preventDefault()
    const props: unknown = info.event.extendedProps
    if (isItemProps(props)) {
      onItemClick(props)
    }
  }

  return (
    <FullCalendar
      plugins={PLUGINS}
      controller={controller}
      initialView={view}
      locale={italian}
      firstDay={1}
      headerToolbar={false}
      height="auto"
      // Everything lasts whole days: no time, not even "Tutto il giorno" in the list of the phone
      displayEventTime={false}
      eventOrder={ORDER}
      dayHeaderClass={dayHeaderClass}
      dayCellClass={dayCellClass}
      listDayHeaderClass={listDayHeaderClass}
      events={items}
      datesSet={handleDatesSet}
      eventClick={handleClick}
    />
  )
}

export default CalendarView
