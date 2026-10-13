import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import Footer from './Footer'

describe('Footer', () => {
  it('says that the project is not official and credits Kuro Games', () => {
    render(<Footer />)

    expect(screen.getByRole('contentinfo')).toHaveTextContent('Progetto non ufficiale')
    expect(screen.getByRole('contentinfo')).toHaveTextContent('appartengono a Kuro Games')
  })

  it('cites the wiki and its license with safe links that open in a new tab', () => {
    render(<Footer />)

    const wiki = screen.getByRole('link', { name: /Wuthering Waves Wiki/ })
    const license = screen.getByRole('link', { name: /CC BY-SA 4.0/ })
    expect(wiki).toHaveAttribute('href', 'https://wutheringwaves.fandom.com/')
    expect(license).toHaveAttribute('href', 'https://creativecommons.org/licenses/by-sa/4.0/')
    for (const link of [wiki, license]) {
      expect(link).toHaveAttribute('target', '_blank')
      expect(link).toHaveAttribute('rel', 'noopener noreferrer')
      expect(link).toHaveAccessibleName(/si apre in una nuova scheda/)
    }
  })
})
