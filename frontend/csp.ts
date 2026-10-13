/**
 * Content-Security-Policy of the frontend (D45, D103): the browser runs only the scripts and styles of our own site
 * and talks only to our own backend, so a script injected into a page (XSS) cannot run from outside, and cannot send
 * data to other sites with a request. (A script that ran anyway could still use the token against our backend, or
 * leave the page with the token in the address: no policy prevents that.)
 *
 * Render does not read this file: the same text is pasted in the Headers section of the static site (see D103).
 * `npm run preview` serves the production build with this header, to check it before changing Render.
 */
export function contentSecurityPolicy(apiOrigin: string): string {
  return [
    "default-src 'self'",
    // No inline scripts and no eval: the build produces one script file
    "script-src 'self'",
    "style-src 'self'",
    // data: for the small icons that Bootstrap writes inside its own CSS (menu button, select arrow)
    "img-src 'self' data:",
    `connect-src 'self' ${apiOrigin}`,
    "font-src 'self'",
    "object-src 'none'",
    "base-uri 'self'",
    "form-action 'self'",
    // The site cannot be shown inside the page of another site (clickjacking)
    "frame-ancestors 'none'",
  ].join('; ')
}

const LOCAL_HOSTS = new Set(['localhost', '127.0.0.1', '[::1]'])

/**
 * The origin (scheme, host and port) of the backend address in VITE_API_BASE_URL. The address must be complete, and
 * https unless it is on this computer: the token travels to it in every request.
 */
export function apiOriginOf(apiBaseUrl: string): string {
  let url: URL
  try {
    url = new URL(apiBaseUrl)
  } catch {
    throw new Error(`VITE_API_BASE_URL must be a complete address like https://api.example.com (it is "${apiBaseUrl}")`)
  }
  if (url.protocol !== 'https:' && !(url.protocol === 'http:' && LOCAL_HOSTS.has(url.hostname))) {
    throw new Error('VITE_API_BASE_URL must use https: http is only allowed for localhost')
  }
  return url.origin
}
