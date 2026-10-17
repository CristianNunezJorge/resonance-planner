import { useId, type FormEvent, type KeyboardEvent } from 'react'
import { SUGGESTIONS } from './format'

interface ChatComposerProps {
  canSend: boolean
  // A message waits for its answer: the field and the buttons wait too, but keep the focus
  isSending: boolean
  draft: string
  onDraftChange: (draft: string) => void
  onSend: (content: string) => void
}

const MAX_CHARS = 1000
// The count of the characters appears only near the limit
const SHOW_COUNT_FROM = 900
// The key code of an Enter that ends a composition (IME) in Safari, where isComposing is already false
const IME_KEY_CODE = 229

// Where the user writes to the agent (D130): Invio sends, Maiusc+Invio goes to a new line. The text lives in the slice,
// so it survives the chat closing; a message that fails comes back into the field. While an answer is awaited the
// field is read-only instead of disabled, so the focus stays where it was (WCAG 2.4.3).
function ChatComposer({ canSend, isSending, draft, onDraftChange, onSend }: ChatComposerProps) {
  const fieldId = useId()
  const noteId = useId()
  const text = draft.trim()
  // Disabled only when the user cannot write at all today (a limit, D129)
  const blocked = !canSend && !isSending

  const send = () => {
    if (!canSend || text === '') {
      return
    }
    onDraftChange('')
    onSend(text)
  }

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    send()
  }

  const handleKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    const composing = event.nativeEvent.isComposing || event.keyCode === IME_KEY_CODE
    if (event.key === 'Enter' && !event.shiftKey && !composing) {
      event.preventDefault()
      send()
    }
  }

  return (
    <>
      <div className="d-flex flex-wrap gap-2 mb-2" role="group" aria-label="Suggerimenti">
        {SUGGESTIONS.map((suggestion) => (
          <button
            key={suggestion}
            type="button"
            className="btn btn-sm btn-outline-light"
            onClick={() => canSend && onSend(suggestion)}
            disabled={blocked}
            aria-disabled={isSending || undefined}
          >
            {suggestion}
          </button>
        ))}
      </div>
      <form onSubmit={handleSubmit} noValidate>
        <label htmlFor={fieldId} className="form-label visually-hidden">
          Messaggio all’agente
        </label>
        <textarea
          id={fieldId}
          className="form-control mb-2"
          rows={2}
          maxLength={MAX_CHARS}
          placeholder="Scrivi all’agente…"
          value={draft}
          onChange={(event) => onDraftChange(event.target.value)}
          onKeyDown={handleKeyDown}
          disabled={blocked}
          readOnly={isSending}
          aria-disabled={isSending || undefined}
          aria-describedby={noteId}
        />
        <div className="d-flex align-items-center gap-2">
          <p id={noteId} className="small text-body-secondary mb-0 me-auto">
            I messaggi e i tuoi dati di gioco (roster, obiettivi, calendario) vanno a un modello gratuito di terzi tramite
            OpenRouter, che può conservarli: non scrivere dati personali.
            {draft.length >= SHOW_COUNT_FROM && ` ${draft.length} / ${MAX_CHARS} caratteri.`}
          </p>
          <button
            type="submit"
            className="btn btn-primary"
            disabled={blocked}
            aria-disabled={isSending || text === '' || undefined}
          >
            Invia
          </button>
        </div>
      </form>
    </>
  )
}

export default ChatComposer
