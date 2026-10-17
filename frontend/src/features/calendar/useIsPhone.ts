import { useMediaQuery } from '../../components/useMediaQuery'

// Below the "md" breakpoint of Bootstrap the calendar becomes a list by day (D66, D67)
const PHONE_QUERY = '(max-width: 767.98px)'
// Below the "lg" breakpoint there is no side column: the chat opens from the side (D130)
const NARROW_QUERY = '(max-width: 991.98px)'

/** True on a narrow screen; it follows the window when it is resized or the phone is turned. */
export function useIsPhone(): boolean {
  return useMediaQuery(PHONE_QUERY)
}

/** True when the page has no side column for the chat (D130). */
export function useIsNarrow(): boolean {
  return useMediaQuery(NARROW_QUERY)
}
