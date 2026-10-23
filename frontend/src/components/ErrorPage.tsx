import { Link } from 'react-router'
import { usePageTitle } from './usePageTitle'

// Shown by the router when a page crashes, instead of React Router's default English stack trace
function ErrorPage() {
  usePageTitle('Errore')

  return (
    <main className="container py-4 text-center">
      <h1>Qualcosa è andato storto</h1>
      <p>Ricarica la pagina o torna alla presentazione.</p>
      {/* A full reload: after a new deploy the old file of a page can be gone (D137), and only a reload gets the new one */}
      <Link to="/" reloadDocument className="btn btn-primary">
        Torna alla presentazione
      </Link>
    </main>
  )
}

export default ErrorPage
