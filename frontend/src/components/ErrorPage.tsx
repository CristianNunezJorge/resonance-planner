import { Link } from 'react-router'

// Shown by the router when a page crashes, instead of React Router's default English stack trace
function ErrorPage() {
  return (
    <main className="container py-4 text-center">
      <h1>Qualcosa è andato storto</h1>
      <p>Ricarica la pagina o torna alla presentazione.</p>
      <Link to="/" className="btn btn-primary">
        Torna alla presentazione
      </Link>
    </main>
  )
}

export default ErrorPage
