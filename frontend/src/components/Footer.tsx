const WIKI_URL = 'https://wutheringwaves.fandom.com/'
const LICENSE_URL = 'https://creativecommons.org/licenses/by-sa/4.0/'

function ExternalLink({ href, children }: { href: string; children: string }) {
  return (
    <a href={href} target="_blank" rel="noopener noreferrer">
      {children}
      <span className="visually-hidden"> (si apre in una nuova scheda)</span>
    </a>
  )
}

// Credits that the sources ask for (D25, D30): the game belongs to Kuro Games and the data come from a CC BY-SA wiki
function Footer() {
  return (
    <footer className="border-top py-3 small text-body-secondary">
      <div className="container">
        <p className="mb-1">
          Progetto non ufficiale, senza alcun legame con Kuro Games. Nomi e immagini di Wuthering Waves appartengono a
          Kuro Games.
        </p>
        <p className="mb-0">
          Dati dal <ExternalLink href={WIKI_URL}>Wuthering Waves Wiki</ExternalLink>, con licenza{' '}
          <ExternalLink href={LICENSE_URL}>CC BY-SA 4.0</ExternalLink>.
        </p>
      </div>
    </footer>
  )
}

export default Footer
