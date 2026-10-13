import { describe, expect, it } from 'vitest'
import { homePath, requestedPath } from './navigation'
import type { User } from './types'

const user = (onboardingCompleted: boolean): User => ({
  id: 1,
  username: 'rover',
  email: 'rover@example.com',
  role: 'USER',
  playTimeMinutes: null,
  onboardingCompleted,
})

describe('homePath', () => {
  it('goes to the guided setup while it is still to do, otherwise to the calendar', () => {
    expect(homePath(user(false))).toBe('/configurazione')
    expect(homePath(user(true))).toBe('/calendario')
  })
})

describe('requestedPath', () => {
  it('gives back the page that was asked for, with its query and anchor', () => {
    expect(requestedPath({ from: { pathname: '/roster', search: '?sort=level', hash: '#top' } })).toBe(
      '/roster?sort=level#top',
    )
  })

  it('accepts a page without query and anchor', () => {
    expect(requestedPath({ from: { pathname: '/profilo' } })).toBe('/profilo')
  })

  it('ignores everything that is not a path inside the app', () => {
    expect(requestedPath({ from: { pathname: '//evil.example.com' } })).toBeNull()
    expect(requestedPath({ from: { pathname: 'https://evil.example.com' } })).toBeNull()
    expect(requestedPath({ from: { pathname: 42 } })).toBeNull()
    expect(requestedPath({ from: 'nothing' })).toBeNull()
    expect(requestedPath({})).toBeNull()
    expect(requestedPath(null)).toBeNull()
    expect(requestedPath(undefined)).toBeNull()
  })
})
