import { useEffect, useState } from 'react'
import { describeError, type RequestFailure } from '../../api/client'
import { useAppDispatch, useAppSelector } from '../../app/hooks'
import { toastShown } from '../../app/toastSlice'
import ConfirmModal from '../../components/ConfirmModal'
import EmptyState from '../../components/EmptyState'
import ErrorState from '../../components/ErrorState'
import LoadingState from '../../components/LoadingState'
import { usePageTitle } from '../../components/usePageTitle'
import { EVENT_TYPE_LABEL, weekdaysText } from '../calendar/format'
import { fetchRules } from '../catalog/catalogSlice'
import { createAdminEvent, deleteAdminEvent, loadAdminEvents, updateAdminEvent } from './adminEventsSlice'
import { scheduleText } from './eventForm'
import EventFormModal from './EventFormModal'
import type { AdminEvent, EventRequest } from './types'

type Dialog = { kind: 'form'; event: AdminEvent | null } | { kind: 'delete'; event: AdminEvent }

// Page P7, the events of the admin (D14, D114, D122): a table and a window with the form. The server checks the role
// at every request (D102); the route only keeps the other users away from a page they could not use (D73).
function AdminEventsPage() {
  usePageTitle('Gestione eventi')
  const dispatch = useAppDispatch()
  const { status, error, refreshing, events } = useAppSelector((state) => state.adminEvents)
  const anchorDate = useAppSelector((state) => state.catalog.rules?.demoEventsAnchorDate ?? null)
  const [dialog, setDialog] = useState<Dialog | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    void dispatch(fetchRules())
  }, [dispatch])

  // At the first visit, and again if the list is forgotten while the page is open (a login in another tab)
  useEffect(() => {
    if (status === 'idle') {
      void dispatch(loadAdminEvents())
    }
  }, [status, dispatch])

  const submit = async (request: EventRequest, event: AdminEvent | null): Promise<RequestFailure | null> => {
    const action = event
      ? await dispatch(updateAdminEvent({ id: event.id, event: request }))
      : await dispatch(createAdminEvent(request))
    if (createAdminEvent.rejected.match(action) || updateAdminEvent.rejected.match(action)) {
      return action.payload ?? describeError(action.error)
    }
    dispatch(toastShown(event ? 'Evento modificato.' : 'Evento creato.'))
    return null
  }

  const remove = async (event: AdminEvent) => {
    setBusy(true)
    const action = await dispatch(deleteAdminEvent(event.id))
    setBusy(false)
    setDialog(null)
    if (deleteAdminEvent.fulfilled.match(action)) {
      dispatch(toastShown('Evento eliminato.'))
    } else {
      dispatch(toastShown(action.payload?.message ?? 'Non è stato possibile eliminare l’evento.', 'danger'))
    }
  }

  const deleteMessage = (event: AdminEvent) =>
    `«${event.name}» sparisce dal calendario di tutti. Le attività già collegate restano, senza il collegamento all’evento.` +
    (event.code ? ' È un evento della demo: ricompare al prossimo riavvio del server.' : '')

  return (
    <section>
      <div className="d-flex flex-wrap align-items-center gap-2 mb-3">
        <h1 className="me-auto mb-0">Gestione eventi</h1>
        <button type="button" className="btn btn-primary" onClick={() => setDialog({ kind: 'form', event: null })}>
          + Nuovo evento
        </button>
      </div>
      <p className="text-body-secondary">Le modifiche si vedono subito nel calendario di tutti gli utenti.</p>
      {status === 'loading' && <LoadingState />}
      {status === 'failed' && error && <ErrorState message={error} onRetry={() => void dispatch(loadAdminEvents())} />}
      {status === 'succeeded' && (
        <>
          {error && (
            <ErrorState message={`L’elenco non è aggiornato. ${error}`} onRetry={() => void dispatch(loadAdminEvents())} />
          )}
          <p role="status" className="small text-body-secondary">
            {refreshing ? 'Aggiornamento dell’elenco…' : ''}
          </p>
          {events.length === 0 ? (
            <EmptyState message="Non ci sono eventi. Crea il primo con «+ Nuovo evento»." />
          ) : (
            <div className="table-responsive">
              <table className="table align-middle">
                <caption className="visually-hidden">Eventi del gioco</caption>
                <thead>
                  <tr>
                    <th scope="col">Nome</th>
                    <th scope="col">Tipo</th>
                    <th scope="col">Date</th>
                    <th scope="col">Giorni attivi</th>
                    <th scope="col">
                      <span className="visually-hidden">Azioni</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {events.map((event) => (
                    <tr key={event.id}>
                      <th scope="row">
                        {event.name}
                        {event.code && <span className="badge text-bg-secondary ms-2">Demo</span>}
                      </th>
                      <td>{EVENT_TYPE_LABEL[event.type]}</td>
                      <td>{scheduleText(event.schedule)}</td>
                      <td>{weekdaysText(event.activeWeekdays)}</td>
                      <td className="text-end text-nowrap">
                        <button
                          type="button"
                          className="btn btn-sm btn-outline-light me-2"
                          aria-label={`Modifica ${event.name}`}
                          onClick={() => setDialog({ kind: 'form', event })}
                        >
                          Modifica
                        </button>
                        <button
                          type="button"
                          className="btn btn-sm btn-outline-danger"
                          aria-label={`Elimina ${event.name}`}
                          onClick={() => setDialog({ kind: 'delete', event })}
                        >
                          Elimina
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
      {dialog?.kind === 'form' && (
        <EventFormModal
          event={dialog.event}
          anchorDate={anchorDate}
          onSubmit={(request) => submit(request, dialog.event)}
          onClose={() => setDialog(null)}
        />
      )}
      {dialog?.kind === 'delete' && (
        <ConfirmModal
          open
          title="Eliminare l’evento?"
          message={deleteMessage(dialog.event)}
          confirmLabel="Elimina"
          busy={busy}
          onConfirm={() => void remove(dialog.event)}
          onCancel={() => setDialog(null)}
        />
      )}
    </section>
  )
}

export default AdminEventsPage
