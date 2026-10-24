import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useAppDispatch, useAppSelector } from '../../app/hooks'
import ErrorState from '../../components/ErrorState'
import LoadingState from '../../components/LoadingState'
import { usePageTitle } from '../../components/usePageTitle'
import { enterAsGuest } from '../auth/authSlice'
import { CALENDAR_PATH, homePath } from '../auth/navigation'
import { fetchCharacters } from '../catalog/catalogSlice'
import CharacterCard from './CharacterCard'
import HowItWorks from './HowItWorks'

type CallToAction = { kind: 'home'; to: string; label: string } | { kind: 'visitor' }

// The button of the presentation: a visitor tries the demo, a logged user goes back to their own home (D72, D73)
function useCallToAction(): CallToAction | null {
  const { user, sessionStatus } = useAppSelector((state) => state.auth)
  if (user) {
    const to = homePath(user)
    return { kind: 'home', to, label: to === CALENDAR_PATH ? 'Vai al calendario' : 'Continua la configurazione' }
  }
  // While the saved session is checked we do not know yet who is looking: no button, instead of a wrong one.
  // If the server could not be reached ('failed') the user is still unknown: like the NavBar, we show the visitor button.
  if (sessionStatus === 'restoring') {
    return null
  }
  return { kind: 'visitor' }
}

// "Prova la demo" (D134): one click opens a temporary guest account and the guided setup; "Registrati" stays next to it
function VisitorButtons() {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const [starting, setStarting] = useState(false)
  const [failure, setFailure] = useState<string | null>(null)

  const tryTheDemo = async () => {
    if (starting) {
      return
    }
    setStarting(true)
    setFailure(null)
    const action = await dispatch(enterAsGuest())
    if (enterAsGuest.fulfilled.match(action)) {
      void navigate(homePath(action.payload.user))
      return
    }
    setStarting(false)
    setFailure(action.payload?.message ?? 'Non è stato possibile aprire la demo. Riprova.')
  }

  return (
    <>
      <div className="d-flex flex-wrap gap-2">
        {/* aria-disabled and not disabled: the free server may need minutes to wake up, and the focus stays here */}
        <button
          type="button"
          className="btn btn-primary btn-lg"
          onClick={() => void tryTheDemo()}
          aria-disabled={starting || undefined}
        >
          {starting ? 'Preparazione della demo…' : 'Prova la demo'}
        </button>
        <Link to="/registrati" className="btn btn-outline-light btn-lg">
          Registrati
        </Link>
      </div>
      {failure && (
        <p className="text-danger-emphasis mt-2 mb-0" role="alert">
          {failure}
        </p>
      )}
    </>
  )
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
      {/* The panel with the artwork of the game, like the cards of Vaelithar (D139) */}
      <section className="landing-hero" aria-labelledby="landing-title">
        <p className="terminal-label mb-2" aria-hidden="true">
          [ Wuthering Waves // planner ]
        </p>
        <h1 id="landing-title">Resonance Planner</h1>
        <p className="lead">
          Il planner per Wuthering Waves che unisce eventi a tempo, Waveplate e obiettivi di crescita in un solo
          calendario.
        </p>
        {callToAction?.kind === 'home' && (
          <Link to={callToAction.to} className="btn btn-primary btn-lg">
            {callToAction.label}
          </Link>
        )}
        {callToAction?.kind === 'visitor' && <VisitorButtons />}
      </section>

      <HowItWorks />

      <section className="mt-5" aria-labelledby="demo-characters-title">
        <h2 id="demo-characters-title" ref={headingRef} tabIndex={-1} className="h4 mb-3 terminal-section-title">
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
