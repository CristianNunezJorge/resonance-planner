import { useEffect, useId } from 'react'
import { useAppDispatch, useAppSelector } from '../../app/hooks'
import ErrorState from '../../components/ErrorState'
import LoadingState from '../../components/LoadingState'
import ChatComposer from './ChatComposer'
import ChatMessages from './ChatMessages'
import { draftChanged, loadChat, loadOlderMessages, selectCanSend, sendChatMessage } from './chatSlice'
import { limitText } from './format'

interface ChatPanelProps {
  // The basic plan without AI, offered when the agent cannot answer today (D64)
  onBasicPlan: () => void
  isPlanning: boolean
}

// The chat with the agent of the calendar page (P4, F6, D130): the counter of today, the messages, the steps of the
// answers, the wait, the limits of D129 with the basic plan, the suggestions and the field.
function ChatPanel({ onBasicPlan, isPlanning }: ChatPanelProps) {
  const titleId = useId()
  const dispatch = useAppDispatch()
  const { status, error, messages, hasMore, olderStatus, usage, sending, sendError, steps, draft } = useAppSelector(
    (state) => state.chat,
  )
  const canSend = useAppSelector(selectCanSend)

  useEffect(() => {
    if (status === 'idle') {
      void dispatch(loadChat())
    }
  }, [status, dispatch])

  const limit = usage ? limitText(usage) : null

  return (
    <section className="card mb-3" aria-labelledby={titleId}>
      <div className="card-body">
        <div className="d-flex align-items-baseline gap-2 mb-2">
          <h2 id={titleId} className="h6 text-uppercase text-body-secondary me-auto mb-0">
            Agente
          </h2>
          {usage && <p className="small mb-0">{`${usage.used} / ${usage.limit} messaggi oggi`}</p>}
        </div>
        {status === 'loading' && <LoadingState />}
        {status === 'failed' && error && <ErrorState message={error} onRetry={() => void dispatch(loadChat())} />}
        {status === 'succeeded' && (
          <>
            <ChatMessages
              messages={messages}
              steps={steps}
              pending={sending?.content ?? null}
              hasMore={hasMore}
              olderStatus={olderStatus}
              onLoadOlder={() => void dispatch(loadOlderMessages())}
            />
            {sendError && (
              <p className="small text-danger-emphasis" role="alert">
                {sendError}
              </p>
            )}
            {limit && (
              <div className="alert alert-secondary small" role="status">
                <p className="mb-2">{`${limit} Intanto puoi usare il piano base.`}</p>
                <button type="button" className="btn btn-sm btn-primary" onClick={onBasicPlan} disabled={isPlanning}>
                  Genera piano base
                </button>
              </div>
            )}
            <ChatComposer
              canSend={canSend}
              isSending={sending !== null}
              draft={draft}
              onDraftChange={(text) => dispatch(draftChanged(text))}
              onSend={(content) => void dispatch(sendChatMessage(content))}
            />
          </>
        )}
      </div>
    </section>
  )
}

export default ChatPanel
