import type { DateRange } from './types'

// The Excel file of the plan (D133) is saved by the page: the name is decided here, so the API does not have to let
// the browser read its Content-Disposition header through CORS
// Some browsers read the file after the click returns: its memory is freed a little later
const REVOKE_DELAY_MS = 10_000

/** The name of the file of a range: only the dates, the same the server suggests. */
export function planFileName(range: DateRange): string {
  return `resonance-planner_${range.from}_${range.to}.xlsx`
}

/** Saves a file with the download of the browser, through a link that exists only for the click. */
export function saveFile(file: Blob, name: string): void {
  const url = URL.createObjectURL(file)
  const link = document.createElement('a')
  link.href = url
  link.download = name
  document.body.append(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), REVOKE_DELAY_MS)
}
