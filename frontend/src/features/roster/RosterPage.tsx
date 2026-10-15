import { useCallback, useEffect, useRef, useState, type KeyboardEvent } from 'react'
import { useAppDispatch, useAppSelector } from '../../app/hooks'
import ErrorState from '../../components/ErrorState'
import LoadingState from '../../components/LoadingState'
import { usePageTitle } from '../../components/usePageTitle'
import { fetchCharacters, fetchMaterials, fetchRules } from '../catalog/catalogSlice'
import CharactersPanel from './CharactersPanel'
import InventoryPanel from './InventoryPanel'
import { loadRoster } from './rosterSlice'

const TABS = [
  { id: 'characters', label: 'Personaggi' },
  { id: 'inventory', label: 'Inventario' },
] as const

type TabId = (typeof TABS)[number]['id']

// Page P5, the roster and the inventory: characters with their goals and what is missing, and the quantities of the
// materials. The catalog is loaded once; the data of the user come from the roster slice (D115).
function RosterPage() {
  usePageTitle('Roster e inventario')
  const dispatch = useAppDispatch()
  const { characters, charactersStatus, charactersError, rules, rulesStatus, rulesError } = useAppSelector(
    (state) => state.catalog,
  )
  const rosterStatus = useAppSelector((state) => state.roster.status)
  const rosterError = useAppSelector((state) => state.roster.error)
  const refreshing = useAppSelector((state) => state.roster.refreshing)
  const [tab, setTab] = useState<TabId>('characters')
  const tabRefs = useRef<Record<TabId, HTMLButtonElement | null>>({ characters: null, inventory: null })

  // The thunks of the catalog ask again only what failed: what arrived is kept
  const loadCatalog = useCallback(() => {
    void dispatch(fetchCharacters())
    void dispatch(fetchRules())
    void dispatch(fetchMaterials())
  }, [dispatch])

  useEffect(() => {
    loadCatalog()
  }, [loadCatalog])

  // At the first visit, and again whenever the data of the user are forgotten while the page is open (a login in
  // another tab restores the session and empties the slice)
  useEffect(() => {
    if (rosterStatus === 'idle') {
      void dispatch(loadRoster())
    }
  }, [rosterStatus, dispatch])

  const retry = () => {
    loadCatalog()
    void dispatch(loadRoster())
  }

  const failureMessage =
    charactersStatus === 'failed'
      ? charactersError
      : rulesStatus === 'failed'
        ? rulesError
        : rosterStatus === 'failed'
          ? rosterError
          : null
  const isReady =
    charactersStatus === 'succeeded' && rulesStatus === 'succeeded' && rules !== null && rosterStatus === 'succeeded'

  // The tabs of WAI-ARIA: the arrows, Home and End move between them, and only the chosen one is in the order of Tab
  const handleTabKeyDown = (event: KeyboardEvent<HTMLButtonElement>) => {
    const index = TABS.findIndex((item) => item.id === tab)
    const targets: Record<string, number> = {
      ArrowRight: (index + 1) % TABS.length,
      ArrowLeft: (index - 1 + TABS.length) % TABS.length,
      Home: 0,
      End: TABS.length - 1,
    }
    if (!(event.key in targets)) {
      return
    }
    event.preventDefault()
    const next = TABS[targets[event.key]].id
    setTab(next)
    tabRefs.current[next]?.focus()
  }

  return (
    <section>
      <h1>Roster e inventario</h1>
      {failureMessage !== null && <ErrorState message={failureMessage} onRetry={retry} />}
      {failureMessage === null && !isReady && <LoadingState />}
      {failureMessage === null && isReady && rules && (
        <>
          {rosterError && (
            <ErrorState
              message={`I calcoli non sono aggiornati. ${rosterError}`}
              onRetry={() => void dispatch(loadRoster())}
            />
          )}
          <p role="status" className="small text-body-secondary">
            {refreshing ? 'Aggiornamento dei calcoli…' : ''}
          </p>
          <ul className="nav nav-tabs mb-3" role="tablist" aria-label="Sezioni del roster">
            {TABS.map((item) => {
              const isActive = item.id === tab
              return (
                <li key={item.id} className="nav-item" role="presentation">
                  <button
                    ref={(element) => {
                      tabRefs.current[item.id] = element
                    }}
                    id={`tab-${item.id}`}
                    type="button"
                    role="tab"
                    className={isActive ? 'nav-link active' : 'nav-link'}
                    aria-selected={isActive}
                    aria-controls={`panel-${item.id}`}
                    tabIndex={isActive ? 0 : -1}
                    onClick={() => setTab(item.id)}
                    onKeyDown={handleTabKeyDown}
                  >
                    {item.label}
                  </button>
                </li>
              )
            })}
          </ul>
          {/* Both panels stay mounted and only one is shown: what a row of the inventory saved is not drawn again
              from an older store when the tab changes. tabIndex lets the keyboard reach a panel from its tab */}
          <div
            id="panel-characters"
            role="tabpanel"
            aria-labelledby="tab-characters"
            tabIndex={0}
            hidden={tab !== 'characters'}
          >
            <CharactersPanel characters={characters} rules={rules} />
          </div>
          <div id="panel-inventory" role="tabpanel" aria-labelledby="tab-inventory" tabIndex={0} hidden={tab !== 'inventory'}>
            <InventoryPanel />
          </div>
        </>
      )}
    </section>
  )
}

export default RosterPage
