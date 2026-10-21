import { useState, type MouseEvent } from 'react'
import { Link, NavLink, useLocation } from 'react-router'
import { useAppSelector } from '../app/hooks'
import { useLogout } from '../features/auth/useLogout'

const MENU_ID = 'main-menu'

// Navigation bar of every page (docs/07-schermate.md). It collapses into a menu on small screens.
function NavBar() {
  const logout = useLogout()
  const { pathname } = useLocation()
  const { user, sessionStatus } = useAppSelector((state) => state.auth)
  // The menu is open only for the page where it was opened. When the page changes (also with the Back button) it is
  // closed during the render, so it cannot reopen later when the user comes back to that page.
  const [menu, setMenu] = useState({ pathname, isOpen: false })
  if (menu.pathname !== pathname) {
    setMenu({ pathname, isOpen: false })
  }
  const isMenuOpen = menu.isOpen && menu.pathname === pathname

  // Tapping any link closes the menu, also the link of the page that is already open
  const closeMenuOnLinkClick = (event: MouseEvent<HTMLElement>) => {
    if (event.target instanceof Element && event.target.closest('a')) {
      setMenu({ pathname, isOpen: false })
    }
  }

  // F11, in the same order as the profile page (useLogout)
  const handleLogout = () => logout('Sei uscito dall\'account.')

  const navLinkClass = ({ isActive }: { isActive: boolean }) => (isActive ? 'nav-link active' : 'nav-link')

  return (
    <header>
      <nav className="navbar navbar-expand-md border-bottom" data-bs-theme="dark" aria-label="Navigazione principale">
        <div className="container">
          <Link className="navbar-brand" to="/">
            Resonance Planner
          </Link>
          <button
            type="button"
            className="navbar-toggler"
            aria-controls={MENU_ID}
            aria-expanded={isMenuOpen}
            aria-label={isMenuOpen ? 'Chiudi il menu' : 'Apri il menu'}
            onClick={() => setMenu({ pathname, isOpen: !isMenuOpen })}
          >
            <span className="navbar-toggler-icon" />
          </button>
          <div
            id={MENU_ID}
            className={isMenuOpen ? 'collapse navbar-collapse show' : 'collapse navbar-collapse'}
            onClick={closeMenuOnLinkClick}
          >
            <ul className="navbar-nav me-auto">
              {user &&
                (user.onboardingCompleted ? (
                  <>
                    <li className="nav-item">
                      <NavLink to="/calendario" className={navLinkClass}>
                        Calendario
                      </NavLink>
                    </li>
                    <li className="nav-item">
                      <NavLink to="/roster" className={navLinkClass}>
                        Roster
                      </NavLink>
                    </li>
                  </>
                ) : (
                  <li className="nav-item">
                    <NavLink to="/configurazione" className={navLinkClass}>
                      Configurazione
                    </NavLink>
                  </li>
                ))}
              {user?.role === 'ADMIN' && (
                <li className="nav-item">
                  <NavLink to="/admin/eventi" className={navLinkClass}>
                    Eventi
                  </NavLink>
                </li>
              )}
              {user && (
                <li className="nav-item">
                  <NavLink to="/profilo" className={navLinkClass}>
                    Profilo
                  </NavLink>
                </li>
              )}
            </ul>
            {user && (
              <div className="d-flex align-items-center gap-3">
                <span className="navbar-text">
                  <span className="visually-hidden">Account: </span>
                  {user.username}
                  {/* The temporary account of "Prova la demo" (D134) */}
                  {user.role === 'GUEST' && <span className="ms-1">(ospite)</span>}
                </span>
                <button type="button" className="btn btn-outline-light btn-sm" onClick={() => void handleLogout()}>
                  Esci
                </button>
              </div>
            )}
            {!user && sessionStatus !== 'restoring' && (
              <div className="d-flex gap-2">
                <Link className="btn btn-outline-light btn-sm" to="/login">
                  Accedi
                </Link>
                <Link className="btn btn-primary btn-sm" to="/registrati">
                  Registrati
                </Link>
              </div>
            )}
          </div>
        </div>
      </nav>
    </header>
  )
}

export default NavBar
