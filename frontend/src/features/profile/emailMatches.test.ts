import { describe, expect, it } from 'vitest'
import { emailMatches } from './emailMatches'

describe('emailMatches', () => {
  it('ignores the spaces around and the capitals', () => {
    expect(emailMatches('rover@example.com', 'rover@example.com')).toBe(true)
    expect(emailMatches('  Rover@Example.COM ', 'rover@example.com')).toBe(true)
  })

  it('refuses another email, a part of it or nothing', () => {
    expect(emailMatches('other@example.com', 'rover@example.com')).toBe(false)
    expect(emailMatches('rover@example', 'rover@example.com')).toBe(false)
    expect(emailMatches('', 'rover@example.com')).toBe(false)
  })
})
