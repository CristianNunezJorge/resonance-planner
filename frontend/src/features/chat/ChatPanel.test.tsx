import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { Provider } from 'react-redux'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client'
import { setupStore } from '../../app/store'
import { signedIn } from '../../test/utils'
import { getMessages, getUsage, sendMessage } from './chatApi'
import ChatPanel from './ChatPanel'
import type { ChatAnswer, ChatMessage, ChatUsage } from './types'

vi.mock('./chatApi', () => ({ getMessages: vi.fn(), getUsage: vi.fn(), sendMessage: vi.fn() }))

function message(id: number, role: ChatMessage['role'], content: string): ChatMessage {
  return { id, role, content, proposalId: null, createdAt: '2026-10-17T12:00:00Z' }
}

const usage: ChatUsage = {
  used: 1,
  limit: 5,
  resetsAt: '2026-10-17T22:00:00Z',
  callsLimitReached: false,
  appLimitReached: false,
}

const answer: ChatAnswer = {
  message: message(6, 'ASSISTANT', 'Oggi fai **due run** di Forgery.'),
  proposal: null,
  steps: ['Ho letto il tuo roster e le Waveplate', 'Ho guardato il tuo calendario'],
  usage: { ...usage, used: 2 },
}

function renderPanel(onBasicPlan = vi.fn()) {
  const store = setupStore(signedIn())
  render(
    <Provider store={store}>
      <ChatPanel onBasicPlan={onBasicPlan} isPlanning={false} />
    </Provider>,
  )
  return store
}

const field = () => screen.getByRole('textbox', { name: 'Messaggio all’agente' })
const log = () => screen.getByRole('log', { name: 'Messaggi con l’agente' })

describe('ChatPanel', () => {
  beforeEach(() => {
    vi.mocked(getMessages).mockResolvedValue({
      messages: [message(4, 'ASSISTANT', '## Ciao!\nSono il tuo **assistente**.'), message(3, 'USER', 'Ciao')],
      hasMore: false,
    })
    vi.mocked(getUsage).mockResolvedValue(usage)
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  it('shows the chat from the oldest message, the counter of today and where the messages go', async () => {
    renderPanel()

    const items = within(await screen.findByRole('log')).getAllByRole('listitem')
    expect(items.map((item) => item.textContent)).toEqual(['Tu: Ciao', 'Agente: Ciao!\nSono il tuo assistente.'])
    expect(screen.getByText('1 / 5 messaggi oggi')).toBeInTheDocument()
    expect(screen.getByText(/modello gratuito di terzi tramite OpenRouter/)).toBeInTheDocument()
  })

  it('shows a text of the model as text, never as HTML', async () => {
    vi.mocked(getMessages).mockResolvedValue({
      messages: [message(4, 'ASSISTANT', '<img src="x" alt="trappola"> <a href="https://evil.example">clic</a>')],
      hasMore: false,
    })
    renderPanel()

    expect(await within(await screen.findByRole('log')).findByText(/<img src="x"/)).toBeInTheDocument()
    expect(screen.queryByRole('img')).not.toBeInTheDocument()
    expect(screen.queryByRole('link')).not.toBeInTheDocument()
  })

  it('shows the message that waits with the indicator, then the answer with its steps', async () => {
    let reply: (value: ChatAnswer) => void = () => {}
    vi.mocked(sendMessage).mockReturnValue(new Promise((resolve) => (reply = resolve)))
    renderPanel()
    fireEvent.change(await screen.findByRole('textbox'), { target: { value: 'Cosa faccio oggi?' } })
    field().focus()

    fireEvent.keyDown(field(), { key: 'Enter' })

    // Read-only, not disabled: the focus stays in the field during the wait (WCAG 2.4.3)
    expect(field()).toHaveValue('')
    expect(field()).toHaveAttribute('readonly')
    expect(field()).toHaveAttribute('aria-disabled', 'true')
    expect(field()).toHaveFocus()
    expect(within(log()).getByText('Cosa faccio oggi?')).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('L’assistente sta lavorando… può richiedere fino a qualche minuto.')
    reply(answer)
    expect(await within(log()).findByText('Oggi fai due run di Forgery.')).toBeInTheDocument()
    const steps = screen.getByRole('list', { name: 'Passaggi dell’agente' })
    expect(within(steps).getAllByRole('listitem').map((item) => item.textContent)).toEqual(answer.steps)
    expect(screen.getByText('2 / 5 messaggi oggi')).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('')
    expect(field()).not.toHaveAttribute('readonly')
    expect(field()).toHaveFocus()
  })

  it('keeps the text written in the field when the chat closes and opens again', async () => {
    const store = setupStore(signedIn())
    const { unmount } = render(
      <Provider store={store}>
        <ChatPanel onBasicPlan={vi.fn()} isPlanning={false} />
      </Provider>,
    )
    fireEvent.change(await screen.findByRole('textbox'), { target: { value: 'Una domanda lunga' } })

    unmount()
    render(
      <Provider store={store}>
        <ChatPanel onBasicPlan={vi.fn()} isPlanning={false} />
      </Provider>,
    )

    expect(await screen.findByRole('textbox')).toHaveValue('Una domanda lunga')
  })

  it('sends with Invio and goes to a new line with Maiusc+Invio', async () => {
    vi.mocked(sendMessage).mockResolvedValue(answer)
    renderPanel()
    fireEvent.change(await screen.findByRole('textbox'), { target: { value: 'Ciao' } })

    fireEvent.keyDown(field(), { key: 'Enter', shiftKey: true })
    expect(sendMessage).not.toHaveBeenCalled()
    fireEvent.keyDown(field(), { key: 'Enter' })

    await waitFor(() => expect(sendMessage).toHaveBeenCalledWith('test.jwt.token', 'Ciao'))
  })

  it('sends a suggestion with one click', async () => {
    vi.mocked(sendMessage).mockResolvedValue(answer)
    renderPanel()

    fireEvent.click(await screen.findByRole('button', { name: 'Organizzami la settimana' }))

    await waitFor(() => expect(sendMessage).toHaveBeenCalledWith('test.jwt.token', 'Organizzami la settimana'))
  })

  it('gives the text back to the field and shows the message of the server when the agent fails', async () => {
    vi.mocked(sendMessage).mockRejectedValue(
      new ApiError('AGENT_UNAVAILABLE', 'L’assistente non è disponibile in questo momento.', 502),
    )
    renderPanel()
    fireEvent.change(await screen.findByRole('textbox'), { target: { value: 'Organizza tutto' } })

    fireEvent.click(screen.getByRole('button', { name: 'Invia' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('L’assistente non è disponibile in questo momento.')
    expect(field()).toHaveValue('Organizza tutto')
    expect(within(log()).queryByText('Organizza tutto')).not.toBeInTheDocument()
  })

  it('stops the field at the daily limit and offers the basic plan', async () => {
    vi.mocked(getUsage).mockResolvedValue({ ...usage, used: 5 })
    const onBasicPlan = vi.fn()
    renderPanel(onBasicPlan)

    expect(await screen.findByText(/Hai usato i 5 messaggi di oggi/)).toBeInTheDocument()
    expect(field()).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Organizzami la settimana' })).toBeDisabled()
    fireEvent.click(screen.getByRole('button', { name: 'Genera piano base' }))
    expect(onBasicPlan).toHaveBeenCalled()
  })

  it('loads the older messages on request', async () => {
    vi.mocked(getMessages).mockResolvedValueOnce({ messages: [message(3, 'USER', 'Ciao')], hasMore: true })
    vi.mocked(getMessages).mockResolvedValueOnce({ messages: [message(2, 'ASSISTANT', 'Ieri')], hasMore: false })
    renderPanel()

    fireEvent.click(await screen.findByRole('button', { name: 'Messaggi precedenti' }))

    expect(await within(log()).findByText('Ieri')).toBeInTheDocument()
    expect(getMessages).toHaveBeenLastCalledWith('test.jwt.token', 3)
    expect(screen.queryByRole('button', { name: 'Messaggi precedenti' })).not.toBeInTheDocument()
  })

  it('offers to try again when the chat cannot be loaded', async () => {
    vi.mocked(getMessages).mockRejectedValueOnce(new ApiError('NETWORK_ERROR', 'Impossibile contattare il server.', 0))
    renderPanel()

    expect(await screen.findByText('Impossibile contattare il server.')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Riprova' }))

    expect(await screen.findByRole('log')).toBeInTheDocument()
  })
})
