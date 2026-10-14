import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import FormField from './FormField'

describe('FormField', () => {
  it('shows a label that belongs to the input and reports the typed value', () => {
    const onChange = vi.fn()
    render(<FormField id="email" label="Email" value="" onChange={onChange} type="email" autoComplete="email" />)

    const input = screen.getByLabelText('Email')
    fireEvent.change(input, { target: { value: 'rover@example.com' } })

    expect(input).toHaveAttribute('type', 'email')
    expect(input).toHaveAttribute('autocomplete', 'email')
    expect(onChange).toHaveBeenCalledWith('rover@example.com')
  })

  it('links the hint to the input, so it is read together with the label', () => {
    render(<FormField id="password" label="Password" value="" onChange={vi.fn()} hint="Almeno 8 caratteri." />)

    expect(screen.getByLabelText('Password')).toHaveAccessibleDescription('Almeno 8 caratteri.')
  })

  it('shows the error under the input and marks the input as invalid, with hint and error read together', () => {
    render(
      <FormField id="password" label="Password" value="" onChange={vi.fn()} hint="Almeno 8 caratteri." error="Inserisci la password." />,
    )

    const input = screen.getByLabelText('Password')
    expect(input).toBeInvalid()
    expect(input).toHaveClass('is-invalid')
    expect(input).toHaveAccessibleDescription('Almeno 8 caratteri. Inserisci la password.')
    expect(screen.getByText('Inserisci la password.')).toHaveClass('invalid-feedback')
  })

  it('can ask a phone for the numeric keyboard while the input stays a text input', () => {
    render(<FormField id="waveplate" label="Waveplate" value="" onChange={vi.fn()} inputMode="numeric" autoComplete="off" />)

    const input = screen.getByLabelText('Waveplate')
    expect(input).toHaveAttribute('inputmode', 'numeric')
    expect(input).toHaveAttribute('type', 'text')
    expect(input).toHaveAttribute('autocomplete', 'off')
  })

  it('marks a required field, so the screen reader says it', () => {
    render(<FormField id="email" label="Email" value="" onChange={vi.fn()} required />)

    expect(screen.getByLabelText('Email')).toBeRequired()
  })

  it('does not mark a field without error', () => {
    render(<FormField id="username" label="Username" value="rover" onChange={vi.fn()} />)

    const input = screen.getByLabelText('Username')
    expect(input).toBeValid()
    expect(input).not.toHaveAttribute('aria-describedby')
  })
})
