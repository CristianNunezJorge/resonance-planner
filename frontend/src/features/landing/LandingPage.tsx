import { useEffect, useRef } from 'react'
import { Link } from 'react-router'
import { useAppDispatch, useAppSelector } from '../../app/hooks'
import ErrorState from '../../components/ErrorState'
import LoadingState from '../../components/LoadingState'
import { usePageTitle } from '../../components/usePageTitle'
import { CALENDAR_PATH, homePath } from '../auth/navigation'
import { fetchCharacters } from '../catalog/catalogSlice'
import CharacterCard from './CharacterCard'
import HowItWorks from './HowItWorks'

// The button of the presentation: a visitor tries the demo, a logged user goes back to their own home (D72, D73)
function useCallToAction(): { to: string; label: string } | null {
  const { user, sessionStatus } = useAppSelector((state) => state.auth)
  if (user) {
    const to = homePath(user)
    return { to, label: to === CALENDAR_PATH ? 'Vai al calendario' : 'Continua la configurazione' }
  }
  // While the saved session is checked we do not know yet who is looking: no button, instead of a wrong one.
  // If the server could not be reached ('failed') the user is still unknown: like the NavBar, we show the visitor button.
  if (sessionStatus === 'restoring') {
    return null
  }
  return { to: '/registrati', label: 'Prova la demo' }
}

// Page P1: what the app does, how it works and the characters of the demo
function LandingPage() {
  usePageTitle('Presentazione')
  const dispatch = useAppDispatch()
  const { characters, charactersStatus, charactersError } = useAppSelector((state) => state.catalog)
  const callToAction = useCallToAction()
  const headingRef = useRef<HTMLHeadingElement>(null)

  useEffect(() => {
    void dispatch(fetchCharacters())
  }, [dispatch])

  // The "Riprova" button disappears with the error: the focus moves to the section title (WCAG 2.4.3)
  const retry = () => {
    headingRef.current?.focus()
    void dispatch(fetchCharacters())
  }

  return (
    <>
      <div className="py-4">
        <h1>Resonance Planner</h1>
        <p className="lead">
          Il planner per Wuthering Waves che unisce eventi a tempo, Waveplate e obiettivi di crescita in un solo
          calendario.
        </p>
        {callToAction && (
          <Link to={callToAction.to} className="btn btn-primary btn-lg">
            {callToAction.label}
          </Link>
        )}
      </div>

      <HowItWorks />

      <section className="mt-5" aria-labelledby="demo-characters-title">
        <h2 id="demo-characters-title" ref={headingRef} tabIndex={-1} className="h4 mb-3">
          Personaggi della demo
        </h2>
        {(charactersStatus === 'idle' || charactersStatus === 'loading') && <LoadingState />}
        {charactersStatus === 'failed' && (
          <ErrorState message={charactersError ?? 'Errore nel caricamento.'} onRetry={retry} />
        )}
        {charactersStatus === 'succeeded' && characters.length === 0 && (
          <p className="text-body-secondary">Nessun personaggio disponibile.</p>
        )}
        {charactersStatus === 'succeeded' && characters.length > 0 && (
          <div className="row row-cols-2 row-cols-md-3 row-cols-xl-6 g-3">
            {characters.map((character) => (
              <div className="col" key={character.id}>
                <CharacterCard character={character} />
              </div>
            ))}
          </div>
        )}
      </section>
    </>
  )
}

export default LandingPage
