import { Link } from 'react-router'

// Page P8; the link to the calendar for logged-in users arrives with T15
function NotFoundPage() {
  return (
    <section className="text-center">
      <h1>Pagina non trovata</h1>
      <p>La pagina che cerchi non esiste.</p>
      <Link to="/" className="btn btn-primary">
        Torna alla presentazione
      </Link>
    </section>
  )
}

export default NotFoundPage
