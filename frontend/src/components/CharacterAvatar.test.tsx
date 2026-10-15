import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { aalto, jinhsi } from '../test/fixtures'
import { initialsOf } from '../features/catalog/format'
import CharacterAvatar from './CharacterAvatar'

describe('initialsOf', () => {
  it('takes two letters of a single name and the first letter of two words', () => {
    expect(initialsOf('Jinhsi')).toBe('JI')
    expect(initialsOf('Aalto')).toBe('AA')
    expect(initialsOf('Rover Havoc')).toBe('RH')
    expect(initialsOf('  lingyang  ')).toBe('LI')
    expect(initialsOf('X')).toBe('X')
  })
})

describe('CharacterAvatar', () => {
  it('shows the official icon, hidden from screen readers because the name is next to it', () => {
    const { container } = render(<CharacterAvatar character={jinhsi} />)

    const image = container.querySelector('img')
    expect(image).toHaveAttribute('src', '/characters/jinhsi.webp')
    expect(image).toHaveAttribute('alt', '')
  })

  it('shows the initials in the colour of the element when the icon is missing', () => {
    const { container } = render(<CharacterAvatar character={{ ...aalto, icon: null }} />)

    expect(container.querySelector('img')).toBeNull()
    const initials = screen.getByText('AA')
    expect(initials).toHaveClass('character-avatar', 'element-aero')
    expect(initials).toHaveAttribute('aria-hidden', 'true')
  })

  it('falls back to the initials when the icon does not load', () => {
    const { container } = render(<CharacterAvatar character={jinhsi} />)

    fireEvent.error(container.querySelector('img') as HTMLImageElement)

    expect(container.querySelector('img')).toBeNull()
    expect(screen.getByText('JI')).toHaveClass('element-spectro')
  })

  it('never uses a path that could lead to another site', () => {
    const { container } = render(<CharacterAvatar character={{ ...jinhsi, icon: '/evil.example/x.webp' }} />)

    expect(container.querySelector('img')).toBeNull()
    expect(screen.getByText('JI')).toBeInTheDocument()
  })
})
