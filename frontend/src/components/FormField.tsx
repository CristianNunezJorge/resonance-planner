import type { Ref } from 'react'

interface FormFieldProps {
  id: string
  label: string
  value: string
  onChange: (value: string) => void
  type?: 'text' | 'email' | 'password'
  autoComplete?: string
  // Tells the user, and the screen reader, that the field must be filled. The server checks it anyway (forms have noValidate).
  required?: boolean
  // Short help under the field, read by screen readers together with the label
  hint?: string
  // The message of the server for this field (D46): shown under the input and linked to it (D70)
  error?: string
  inputRef?: Ref<HTMLInputElement>
}

// A labelled input with its hint and its error, for every form of the app
function FormField({
  id,
  label,
  value,
  onChange,
  type = 'text',
  autoComplete,
  required,
  hint,
  error,
  inputRef,
}: FormFieldProps) {
  const hintId = `${id}-hint`
  const errorId = `${id}-error`
  const describedBy = [hint ? hintId : null, error ? errorId : null].filter(Boolean).join(' ') || undefined

  return (
    <div className="mb-3">
      <label htmlFor={id} className="form-label">
        {label}
      </label>
      <input
        id={id}
        name={id}
        ref={inputRef}
        type={type}
        className={error ? 'form-control is-invalid' : 'form-control'}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        autoComplete={autoComplete}
        required={required}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
      />
      {hint && (
        <div id={hintId} className="form-text">
          {hint}
        </div>
      )}
      {error && (
        <div id={errorId} className="invalid-feedback">
          {error}
        </div>
      )}
    </div>
  )
}

export default FormField
