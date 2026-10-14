import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import SelectField from './SelectField'

const OPTIONS = [
  { value: '1', label: '1' },
  { value: '20', label: '20' },
  { value: '40', label: '40' },
]

describe('SelectField', () => {
  it('shows a label that belongs to the select, the options and the chosen value', () => {
    render(<SelectField id="level" label="Livello attuale" value="20" onChange={vi.fn()} options={OPTIONS} />)

    const select = screen.getByLabelText('Livello attuale')
    expect(select.tagName).toBe('SELECT')
    expect(select).toHaveValue('20')
    expect(screen.getAllByRole('option').map((option) => option.textContent)).toEqual(['1', '20', '40'])
  })

  it('reports the value that was chosen, as text', () => {
    const onChange = vi.fn()
    render(<SelectField id="level" label="Livello attuale" value="1" onChange={onChange} options={OPTIONS} />)

    fireEvent.change(screen.getByLabelText('Livello attuale'), { target: { value: '40' } })

    expect(onChange).toHaveBeenCalledWith('40')
  })

  it('links the hint to the select, so it is read together with the label', () => {
    render(<SelectField id="level" label="Livello" value="1" onChange={vi.fn()} options={OPTIONS} hint="Da 1 a 40." />)

    expect(screen.getByLabelText('Livello')).toHaveAccessibleDescription('Da 1 a 40.')
  })

  it('shows the error under the select and marks it as invalid, with hint and error read together', () => {
    render(
      <SelectField id="target" label="Obiettivo" value="1" onChange={vi.fn()} options={OPTIONS} hint="Da 1 a 40." error="Scegli un livello più alto." />,
    )

    const select = screen.getByLabelText('Obiettivo')
    expect(select).toBeInvalid()
    expect(select).toHaveClass('is-invalid')
    expect(select).toHaveAccessibleDescription('Da 1 a 40. Scegli un livello più alto.')
    expect(screen.getByText('Scegli un livello più alto.')).toHaveClass('invalid-feedback')
  })

  it('does not mark a select without error', () => {
    render(<SelectField id="level" label="Livello" value="1" onChange={vi.fn()} options={OPTIONS} />)

    const select = screen.getByLabelText('Livello')
    expect(select).toBeValid()
    expect(select).not.toHaveAttribute('aria-describedby')
  })
})
