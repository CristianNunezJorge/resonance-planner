// Texts of the Waveplate panel. The values come from the server (D3): here they are only written

// A moment is shown in the time of the browser
const timeFormat = new Intl.DateTimeFormat('it-IT', { hour: '2-digit', minute: '2-digit' })
const weekdayDayFormat = new Intl.DateTimeFormat('it-IT', { weekday: 'long', day: 'numeric' })

function isSameLocalDay(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
}

/** When the Waveplate will be full, in the time of the browser: "piene alle 16:24", "piene domani alle 03:05". */
export function fullAtText(fullAt: string, now: Date): string {
  const moment = new Date(fullAt)
  if (moment.getTime() <= now.getTime()) {
    return 'già piene'
  }
  const time = timeFormat.format(moment)
  if (isSameLocalDay(moment, now)) {
    return `piene alle ${time}`
  }
  const tomorrow = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1)
  if (isSameLocalDay(moment, tomorrow)) {
    return `piene domani alle ${time}`
  }
  return `piene ${weekdayDayFormat.format(moment)} alle ${time}`
}
