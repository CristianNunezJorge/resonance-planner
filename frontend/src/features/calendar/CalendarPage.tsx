import { useCalendarController } from '@fullcalendar/react'
import { isFulfilled, isRejectedWithValue, type UnknownAction } from '@reduxjs/toolkit'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { describeError, type RequestFailure } from '../../api/client'
import { useAppDispatch, useAppSelector, useAppStore } from '../../app/hooks'
import { toastShown } from '../../app/toastSlice'
import ConfirmModal from '../../components/ConfirmModal'
import EmptyState from '../../components/EmptyState'
import ErrorState from '../../components/ErrorState'
import LoadingState from '../../components/LoadingState'
import { usePageTitle } from '../../components/usePageTitle'
import { fetchCharacters, fetchMaterials, fetchRules } from '../catalog/catalogSlice'
import ChatDrawer from '../chat/ChatDrawer'
import ChatPanel from '../chat/ChatPanel'
import { loadChat, selectCanSend, sendChatMessage } from '../chat/chatSlice'
import { explainEventText, UPDATE_PLAN_TEXT } from '../chat/format'
import WaveplatePanel from '../waveplate/WaveplatePanel'
import { exportPlan } from './calendarApi'
import { eventKey, toCalendarItems, type ItemProps } from './calendarItems'
import {
  completeCalendarTask,
  confirmPendingProposal,
  createCalendarTask,
  deleteCalendarTask,
  generateBasicPlan,
  loadCalendar,
  rejectPendingProposal,
  updateCalendarTask,
} from './calendarSlice'
import CalendarToolbar from './CalendarToolbar'
import CalendarView, { type CalendarViewName } from './CalendarView'
import EventDetailModal from './EventDetailModal'
import { planFileName, saveFile } from './exportFile'
import { addedText, dayText } from './format'
import TaskDetailModal from './TaskDetailModal'
import TaskFormModal from './TaskFormModal'
import type { CalendarTask, DateRange, TaskForm } from './types'
import ProposalBar from './ProposalBar'
import { useIsNarrow, useIsPhone } from './useIsPhone'

// The window open over the calendar, if any
type Dialog =
  | { kind: 'event'; key: string }
  | { kind: 'task'; taskId: number }
  | { kind: 'form'; task: CalendarTask | null }
  | { kind: 'complete'; taskId: number }
  | { kind: 'delete'; taskId: number }

// Today in Italian time (D41), as "2026-10-16": the day proposed for a new task. The Canadian English format of
// Intl writes dates exactly in this form
const romeDay = new Intl.DateTimeFormat('en-CA', { timeZone: 'Europe/Rome', year: 'numeric', month: '2-digit', day: '2-digit' })

function todayText(): string {
  return romeDay.format(new Date())
}

// Page P4, the calendar (D66, D67, D120): events only on their active days, the tasks of the user, the windows of
// detail and the Waveplate panel; the pending proposal with its bar, the basic plan and the warning that the plan is
// out of date (D23, D64, D68, D125); the chat with the agent, on the side or sliding from it below 992 px (D130).
function CalendarPage() {
  usePageTitle('Calendario')
  const dispatch = useAppDispatch()
  const store = useAppStore()
  const isPhone = useIsPhone()
  const isNarrow = useIsNarrow()
  const [chatOpen, setChatOpen] = useState(false)
  const controller = useCalendarController()
  const [desktopView, setDesktopView] = useState<CalendarViewName>('dayGridWeek')
  const view: CalendarViewName = isPhone ? 'listWeek' : desktopView
  const [title, setTitle] = useState('')
  const [dialog, setDialog] = useState<Dialog | null>(null)
  const [busy, setBusy] = useState(false)
  // A request on the plan is running (basic plan, confirm, reject): its buttons wait
  const [planning, setPlanning] = useState(false)
  const [exporting, setExporting] = useState(false)
  const exportRunning = useRef(false)
  // The range on the screen: asked again if the data are forgotten while the page is open
  const shownRange = useRef<DateRange | null>(null)
  // After Conferma or Scarta the bar goes away with the focused button: the focus moves to the title of the page
  const headingRef = useRef<HTMLHeadingElement>(null)

  const { characters, materials, rules, rulesStatus, rulesError } = useAppSelector((state) => state.catalog)
  const userId = useAppSelector((state) => state.auth.user?.id)
  const { status, error, refreshing, events, tasks, pendingProposal, planOutdated } = useAppSelector(
    (state) => state.calendar,
  )
  const chatStatus = useAppSelector((state) => state.chat.status)
  const canAskAgent = useAppSelector(selectCanSend)

  useEffect(() => {
    void dispatch(fetchRules())
    void dispatch(fetchCharacters())
    void dispatch(fetchMaterials())
  }, [dispatch])

  // The usage of today is needed also while the chat is closed: "Spiegamelo" and "Chiedi all'agente" depend on it
  useEffect(() => {
    if (chatStatus === 'idle') {
      void dispatch(loadChat())
    }
  }, [chatStatus, dispatch])

  // The calendar was drawn with the first view: a change (phone turned, Settimana/Mese) is applied through the controller
  useEffect(() => {
    controller.changeView(view)
  }, [controller, view])

  // A login in another tab empties the slice while the page is open: the same range is asked again. The state is
  // read from the store, not from this drawing: the calendar may have just asked for its first range
  useEffect(() => {
    if (status === 'idle' && store.getState().calendar.status === 'idle' && shownRange.current) {
      void dispatch(loadCalendar(shownRange.current))
    }
  }, [status, dispatch, store])

  // The tasks of the proposal are drawn with the others, dashed (D68)
  const shownTasks = useMemo(() => [...tasks, ...(pendingProposal?.tasks ?? [])], [tasks, pendingProposal])
  const items = useMemo(() => toCalendarItems(events, shownTasks), [events, shownTasks])

  const handleRangeChange = useCallback(
    (range: DateRange, periodTitle: string) => {
      shownRange.current = range
      setTitle(periodTitle)
      // A window belongs to the period it was opened in
      setDialog(null)
      void dispatch(loadCalendar(range))
    },
    [dispatch],
  )

  const handleItemClick = useCallback((item: ItemProps) => {
    setDialog(item.kind === 'event' ? { kind: 'event', key: item.key } : { kind: 'task', taskId: item.taskId })
  }, [])

  const retry = () => {
    void dispatch(fetchRules())
    if (shownRange.current) {
      void dispatch(loadCalendar(shownRange.current))
    }
  }

  const closeDialog = () => setDialog(null)

  const submitTask = async (form: TaskForm, task: CalendarTask | null): Promise<RequestFailure | null> => {
    const action = task
      ? await dispatch(updateCalendarTask({ id: task.id, form }))
      : await dispatch(createCalendarTask(form))
    // A failure always has its message, even without a payload: the form never takes it for a success
    if (createCalendarTask.rejected.match(action) || updateCalendarTask.rejected.match(action)) {
      return action.payload ?? describeError(action.error)
    }
    dispatch(toastShown(task ? 'Attività modificata.' : `Attività aggiunta: ${dayText(form.taskDate)}.`))
    return null
  }

  const completeTask = async (taskId: number) => {
    setBusy(true)
    const action = await dispatch(completeCalendarTask(taskId))
    setBusy(false)
    setDialog(null)
    if (completeCalendarTask.fulfilled.match(action)) {
      const added = addedText(action.payload.addedToInventory, materials)
      dispatch(toastShown(added ? `Attività fatta. ${added}` : 'Attività fatta.'))
    } else {
      dispatch(toastShown(action.payload?.message ?? 'Non è stato possibile segnare l’attività come fatta.', 'danger'))
    }
  }

  const deleteTask = async (taskId: number) => {
    setBusy(true)
    const action = await dispatch(deleteCalendarTask(taskId))
    setBusy(false)
    setDialog(null)
    if (deleteCalendarTask.fulfilled.match(action)) {
      dispatch(toastShown('Attività eliminata.'))
    } else {
      dispatch(toastShown(action.payload?.message ?? 'Non è stato possibile eliminare l’attività.', 'danger'))
    }
  }

  // One request on the plan at a time; the server message of a failure becomes the toast (a 409 explains itself)
  const runOnPlan = async (
    request: () => Promise<UnknownAction>,
    success: string,
    fallback: string,
    moveFocus = false,
  ) => {
    setPlanning(true)
    const action = await request()
    setPlanning(false)
    if (moveFocus) {
      headingRef.current?.focus()
    }
    if (isFulfilled(action)) {
      dispatch(toastShown(success))
      return
    }
    const failure = isRejectedWithValue(action) ? (action.payload as RequestFailure) : null
    dispatch(toastShown(failure?.message ?? fallback, 'danger'))
  }

  const makeBasicPlan = () =>
    void runOnPlan(
      () => dispatch(generateBasicPlan()),
      'Piano base pronto: controllalo e confermalo.',
      'Non è stato possibile generare il piano base.',
    )

  const confirmProposal = (id: number) =>
    void runOnPlan(
      () => dispatch(confirmPendingProposal(id)),
      'Piano confermato: le attività sono nel calendario.',
      'Non è stato possibile confermare la proposta.',
      true,
    )

  const rejectProposal = (id: number) =>
    void runOnPlan(
      () => dispatch(rejectPendingProposal(id)),
      'Proposta scartata.',
      'Non è stato possibile scartare la proposta.',
      true,
    )

  // The period on the screen as an Excel file (D133). The file is not a state of the app, so it does not go through
  // Redux; the answer for a session that ended meanwhile is not saved, like the other answers (D103)
  const exportExcel = async () => {
    const token = store.getState().auth.token
    const range = shownRange.current
    // The button stays clickable while it waits (aria-disabled): one file at a time
    if (!token || !range || exportRunning.current) {
      return
    }
    exportRunning.current = true
    setExporting(true)
    try {
      const file = await exportPlan(token, range)
      if (store.getState().auth.token === token) {
        saveFile(file, planFileName(range))
        dispatch(toastShown('Piano esportato.'))
      }
    } catch (error) {
      if (store.getState().auth.token === token) {
        dispatch(toastShown(describeError(error).message, 'danger'))
      }
    } finally {
      exportRunning.current = false
      setExporting(false)
    }
  }

  // A question for the agent from outside the chat: below 992 px the chat opens to show the answer; on a wide screen
  // the answer arrives in the side column, and a toast says so (D130)
  const askAgent = (content: string) => {
    if (isNarrow) {
      setChatOpen(true)
    } else {
      dispatch(toastShown('Domanda inviata all’agente: la risposta arriva nella chat a destra.', 'info'))
    }
    void dispatch(sendChatMessage(content))
  }

  const taskOf = (taskId: number) => shownTasks.find((task) => task.id === taskId)
  const characterName = (task: CalendarTask) =>
    characters.find((character) => character.id === task.characterId)?.name ?? null

  // A new user (a login in another tab) gets a new chat, without the text written by the one before
  const chat = <ChatPanel key={userId} onBasicPlan={makeBasicPlan} isPlanning={planning} />

  const firstLoadFailed = status === 'failed' || rulesStatus === 'failed'
  const failureMessage = status === 'failed' ? error : rulesError

  const renderDialog = () => {
    if (!dialog) {
      return null
    }
    if (dialog.kind === 'event') {
      const event = events.find((item) => eventKey(item) === dialog.key)
      return event ? (
        <EventDetailModal
          event={event}
          onClose={closeDialog}
          canExplain={canAskAgent}
          onExplain={() => {
            setDialog(null)
            askAgent(explainEventText(event.name))
          }}
        />
      ) : null
    }
    if (dialog.kind === 'form') {
      return (
        <TaskFormModal
          task={dialog.task}
          defaultDay={todayText()}
          onSubmit={(form) => submitTask(form, dialog.task)}
          onClose={closeDialog}
        />
      )
    }
    // The task may be gone after a reload (deleted in another tab): then there is nothing to show
    const task = taskOf(dialog.taskId)
    if (!task) {
      return null
    }
    if (dialog.kind === 'task') {
      return (
        <TaskDetailModal
          task={task}
          characterName={characterName(task)}
          onClose={closeDialog}
          onComplete={() => setDialog({ kind: 'complete', taskId: task.id })}
          onEdit={() => setDialog({ kind: 'form', task })}
          onDelete={() => setDialog({ kind: 'delete', taskId: task.id })}
        />
      )
    }
    return dialog.kind === 'complete' ? (
      <ConfirmModal
        open
        title="Segnare come fatta?"
        message={`«${task.title}» diventa fatta e non si può annullare: le attività di farming aggiungono all’inventario i materiali delle loro run.`}
        confirmLabel="Segna come fatta"
        busy={busy}
        onConfirm={() => void completeTask(task.id)}
        onCancel={closeDialog}
      />
    ) : (
      <ConfirmModal
        open
        title="Eliminare l’attività?"
        message={`«${task.title}» sparisce dal calendario.`}
        confirmLabel="Elimina"
        busy={busy}
        onConfirm={() => void deleteTask(task.id)}
        onCancel={closeDialog}
      />
    )
  }

  return (
    <section>
      <h1 ref={headingRef} tabIndex={-1}>
        Calendario
      </h1>
      {firstLoadFailed && failureMessage && <ErrorState message={failureMessage} onRetry={retry} />}
      <div className="row g-4">
        <div className="col-lg-9 order-2 order-lg-1">
          <CalendarToolbar
            title={title}
            view={view}
            canChangeView={!isPhone}
            onPrevious={() => controller.prev()}
            onToday={() => controller.today()}
            onNext={() => controller.next()}
            onViewChange={setDesktopView}
            onAddTask={() => setDialog({ kind: 'form', task: null })}
            onGeneratePlan={makeBasicPlan}
            isPlanning={planning}
            onExport={() => void exportExcel()}
            isExporting={exporting}
          />
          {planOutdated && (
            <div className="alert alert-warning d-flex flex-wrap align-items-center gap-2">
              <strong>Attenzione:</strong>
              <span className="me-auto">Roster, obiettivi o eventi sono cambiati dopo il tuo ultimo piano.</span>
              <button type="button" className="btn btn-sm btn-dark" onClick={makeBasicPlan} disabled={planning}>
                Genera un nuovo piano
              </button>
              <button
                type="button"
                className="btn btn-sm btn-outline-dark"
                onClick={() => askAgent(UPDATE_PLAN_TEXT)}
                disabled={!canAskAgent}
              >
                Chiedi all’agente
              </button>
            </div>
          )}
          {pendingProposal && (
            <ProposalBar
              proposal={pendingProposal}
              busy={planning}
              onConfirm={() => confirmProposal(pendingProposal.id)}
              onReject={() => rejectProposal(pendingProposal.id)}
            />
          )}
          {status === 'succeeded' && error && (
            <ErrorState message={`Il calendario non è aggiornato. ${error}`} onRetry={retry} />
          )}
          <p role="status" className="small text-body-secondary mb-2">
            {refreshing ? 'Aggiornamento del calendario…' : ''}
          </p>
          {status === 'loading' && <LoadingState />}
          {/* Always on the page: it tells which days are on the screen, and so what to load */}
          <CalendarView
            controller={controller}
            view={view}
            items={items}
            onRangeChange={handleRangeChange}
            onItemClick={handleItemClick}
          />
          {status === 'succeeded' && !refreshing && tasks.length === 0 && !pendingProposal && (
            <EmptyState message="Non hai ancora un piano in questo periodo. Chiedi all’agente di organizzarti la settimana, crealo dai tuoi obiettivi con «Genera piano base» oppure aggiungi un’attività con «+ Attività»." />
          )}
        </div>
        <aside className="col-lg-3 order-1 order-lg-2" aria-label="Riepilogo">
          {/* A new user (a login in another tab) gets a new panel, without the values of the one before */}
          {rules && <WaveplatePanel key={userId} rules={rules} />}
          {!isNarrow && chat}
        </aside>
      </div>
      {isNarrow && (
        <>
          <button type="button" className="btn btn-primary chat-fab" aria-haspopup="dialog" onClick={() => setChatOpen(true)}>
            Agente
          </button>
          <ChatDrawer open={chatOpen} onClose={() => setChatOpen(false)}>
            {chat}
          </ChatDrawer>
        </>
      )}
      {renderDialog()}
    </section>
  )
}

export default CalendarPage
