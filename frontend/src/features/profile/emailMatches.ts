/**
 * The email typed to confirm the deletion of the account (F12, D121) is the one of the account: spaces around and
 * capitals do not count, because the server stores the email lowercase and without spaces.
 */
export function emailMatches(typed: string, email: string): boolean {
  return typed.trim().toLowerCase() === email.trim().toLowerCase()
}
