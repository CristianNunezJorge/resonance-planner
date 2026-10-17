import type { ChatUsage } from './types'

// Texts of the chat (D130)

// The suggestions of D11: the agent reads the data it needs with its tools
export const SUGGESTIONS = ['Organizzami la settimana', 'Cosa faccio oggi?', 'Spiegami gli eventi in corso']

// The name of an event is written by an admin: in the question it is only a short name, never more text (D60)
const MAX_EVENT_NAME = 80

/** What "Spiegamelo" asks for an event of the calendar. */
export function explainEventText(name: string): string {
  const shortName = name.replace(/[«»\r\n]+/g, ' ').trim().slice(0, MAX_EVENT_NAME)
  return `Spiegami l’evento «${shortName}».`
}

// What "Chiedi all'agente" asks when the plan is out of date (D125)
export const UPDATE_PLAN_TEXT = 'Roster, obiettivi o eventi sono cambiati dopo il mio ultimo piano: aggiornalo.'

/**
 * The text of the model as plain text: the marks of bold text, of code and of titles go away, the lines and the
 * dashes of the lists stay. The result is still shown as text, never as HTML (D60).
 */
export function plainText(text: string): string {
  return text
    .replace(/\*\*(.+?)\*\*/gs, '$1')
    .replace(/__(.+?)__/gs, '$1')
    .replace(/`([^`\n]*)`/g, '$1')
    .replace(/^#{1,6}[ \t]+/gm, '')
}

/** Why the user cannot write now, or null if a daily limit is not over (D129). */
export function limitText(usage: ChatUsage): string | null {
  if (usage.appLimitReached) {
    return 'Per oggi l’assistente ha finito le richieste gratuite di tutta l’app. Riprova domani.'
  }
  if (usage.used >= usage.limit) {
    return `Hai usato i ${usage.limit} messaggi di oggi: puoi scrivere di nuovo da mezzanotte.`
  }
  if (usage.callsLimitReached) {
    return 'Per oggi hai finito le richieste all’assistente: anche i messaggi non riusciti le consumano. Riprova domani.'
  }
  return null
}
