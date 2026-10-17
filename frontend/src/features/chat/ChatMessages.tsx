import { useEffect, useRef } from 'react'
import { plainText } from './format'
import type { ChatMessage } from './types'

interface ChatMessagesProps {
  messages: ChatMessage[]
  // The steps of the answers that arrived in this visit (D62)
  steps: Record<number, string[]>
  // The message that waits for its answer
  pending: string | null
  hasMore: boolean
  olderStatus: 'idle' | 'loading' | 'failed'
  onLoadOlder: () => void
}

// The messages of the chat, from the oldest (D130). The texts are always plain text, never HTML (D60): the model
// writes them, and its marks of markdown go away. A screen reader hears the new messages (role log) and the wait
// (role status, outside the log so it is not heard twice). The list scrolls, so it can take the focus.
function ChatMessages({ messages, steps, pending, hasMore, olderStatus, onLoadOlder }: ChatMessagesProps) {
  const logRef = useRef<HTMLDivElement>(null)
  // A new message at the end, or the one that waits, brings the list to the bottom; older ones added on top do not
  const lastId = messages.at(-1)?.id
  useEffect(() => {
    const log = logRef.current
    if (log) {
      log.scrollTop = log.scrollHeight
    }
  }, [lastId, pending])

  const olderLabel =
    olderStatus === 'failed' ? 'Riprova a caricare i messaggi precedenti' : 'Messaggi precedenti'

  return (
    <>
      <div ref={logRef} className="chat-log mb-2" role="log" aria-label="Messaggi con l’agente" tabIndex={0}>
        {hasMore && (
          <p className="text-center">
            <button
              type="button"
              className="btn btn-sm btn-link"
              onClick={onLoadOlder}
              disabled={olderStatus === 'loading'}
            >
              {olderStatus === 'loading' ? 'Caricamento…' : olderLabel}
            </button>
          </p>
        )}
        {messages.length === 0 && !pending && (
          <p className="small text-body-secondary">Nessun messaggio: scrivi all’agente o scegli un suggerimento.</p>
        )}
        <ol className="list-unstyled mb-0 d-grid gap-2">
          {messages.map((item) => (
            <li key={item.id}>
              <p className={`chat-message chat-message--${item.role === 'USER' ? 'user' : 'assistant'} mb-1`}>
                <span className="visually-hidden">{item.role === 'USER' ? 'Tu: ' : 'Agente: '}</span>
                {item.role === 'USER' ? item.content : plainText(item.content)}
              </p>
              {steps[item.id] && steps[item.id].length > 0 && (
                <ul className="small text-body-secondary mb-0 ps-3" aria-label="Passaggi dell’agente">
                  {/* A fixed list: the same step can come twice */}
                  {steps[item.id].map((step, index) => (
                    <li key={`${index}-${step}`}>{step}</li>
                  ))}
                </ul>
              )}
            </li>
          ))}
          {/* The user knows what they wrote: it is heard once, with the answer */}
          {pending && (
            <li aria-hidden="true">
              <p className="chat-message chat-message--user mb-1">
                <span className="visually-hidden">Tu: </span>
                {pending}
              </p>
            </li>
          )}
        </ol>
      </div>
      <p role="status" className="small text-body-secondary mb-3">
        {pending && (
          <>
            <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />
            L’assistente sta lavorando… può richiedere fino a qualche minuto.
          </>
        )}
      </p>
    </>
  )
}

export default ChatMessages
