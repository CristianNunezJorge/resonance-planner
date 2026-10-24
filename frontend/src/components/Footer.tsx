import type { NotificationStatus } from '../features/notifications/useNotifications'

const WIKI_URL = 'https://wutheringwaves.fandom.com/'
const LICENSE_URL = 'https://creativecommons.org/licenses/by-sa/4.0/'

// The light of the terminal (D139) says the state of the real-time notifications (D135); the text says it too
const STATUS: Record<NotificationStatus, { text: string; dot: string }> = {
  off: { text: 'Notifiche: con l’accesso', dot: 'status-dot' },
  connecting: { text: 'Notifiche: in collegamento', dot: 'status-dot status-dot--waiting' },
  connected: { text: 'Notifiche: collegate', dot: 'status-dot status-dot--on' },
  stopped: { text: 'Notifiche: non attive', dot: 'status-dot' },
}

interface FooterProps {
  notificationStatus: NotificationStatus
}

function ExternalLink({ href, children }: { href: string; children: string }) {
  return (
    <a href={href} target="_blank" rel="noopener noreferrer">
      {children}
      <span className="visually-hidden"> (si apre in una nuova scheda)</span>
    </a>
  )
}

// Credits that the sources ask for (D25, D30): the game belongs to Kuro Games and the data come from a CC BY-SA wiki
function Footer({ notificationStatus }: FooterProps) {
  const status = STATUS[notificationStatus]
  return (
    <footer className="terminal-footer py-3">
      <div className="container d-flex flex-column flex-lg-row gap-2 gap-lg-4 justify-content-between">
        <p className="terminal-status mb-0">
          <span className={status.dot} aria-hidden="true" />
          {status.text}
        </p>
        <div className="text-lg-end">
          <p className="mb-1">
            Progetto non ufficiale, senza alcun legame con Kuro Games. Nomi e immagini di Wuthering Waves appartengono a
            Kuro Games.
          </p>
          <p className="mb-0">
            Dati dal <ExternalLink href={WIKI_URL}>Wuthering Waves Wiki</ExternalLink>, con licenza{' '}
            <ExternalLink href={LICENSE_URL}>CC BY-SA 4.0</ExternalLink>.
          </p>
        </div>
      </div>
    </footer>
  )
}

export default Footer
