import { Link } from 'react-router'
import { useAppSelector } from '../app/hooks'
import { CALENDAR_PATH, homePath } from '../features/auth/navigation'
import { usePageTitle } from './usePageTitle'

// Page P8: a logged user goes back to the home of the account, a visitor to the presentation
function NotFoundPage() {
  usePageTitle('Pagina non trovata')
  const user = useAppSelector((state) => state.auth.user)
  const homeLink = user
    ? { to: homePath(user), label: homePath(user) === CALENDAR_PATH ? 'Torna al calendario' : 'Torna alla configurazione' }
    : { to: '/', label: 'Torna alla presentazione' }

  return (
    <section className="text-center">
      <h1>Pagina non trovata</h1>
      <p>La pagina che cerchi non esiste.</p>
      <Link to={homeLink.to} className="btn btn-primary">
        {homeLink.label}
      </Link>
    </section>
  )
}

export default NotFoundPage
