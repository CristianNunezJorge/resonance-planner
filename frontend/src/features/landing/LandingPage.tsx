import { useEffect, useRef } from 'react'
import { useAppDispatch, useAppSelector } from '../../app/hooks'
import ErrorState from '../../components/ErrorState'
import LoadingState from '../../components/LoadingState'
import { fetchCharacters } from '../catalog/catalogSlice'
import CharacterCard from './CharacterCard'

// Placeholder of the presentation page P1 (T16): for now it shows the characters read from the backend
function LandingPage() {
  const dispatch = useAppDispatch()
  const { characters, charactersStatus, charactersError } = useAppSelector((state) => state.catalog)
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
    <section>
      <h1>Resonance Planner</h1>
      <p className="lead">Il planner per Wuthering Waves è in costruzione.</p>

      <h2 ref={headingRef} tabIndex={-1} className="h4 mt-4 mb-3">
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
  )
}

export default LandingPage
