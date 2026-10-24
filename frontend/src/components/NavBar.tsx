import { useState, type MouseEvent } from 'react'
import { Link, NavLink, useLocation } from 'react-router'
import { useAppSelector } from '../app/hooks'
import type { User } from '../features/auth/types'
import { useLogout } from '../features/auth/useLogout'

const MENU_ID = 'main-menu'

interface MenuLink {
  to: string
  label: string
}

// The pages of the menu, in order (D73): the guided setup until it is done, the admin area only for an admin
function menuLinks(user: User | null): MenuLink[] {
  if (!user) {
    return []
  }
  const home = user.onboardingCompleted
    ? [
        { to: '/calendario', label: 'Calendario' },
        { to: '/roster', label: 'Roster' },
      ]
    : [{ to: '/configurazione', label: 'Configurazione' }]
  const admin = user.role === 'ADMIN' ? [{ to: '/admin/eventi', label: 'Eventi' }] : []
  return [...home, ...admin, { to: '/profilo', label: 'Profilo' }]
}

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
      <nav className="navbar navbar-expand-md terminal-navbar" data-bs-theme="dark" aria-label="Navigazione principale">
        <div className="container">
          {/* The logo of the terminal (D139): the name is the only text a screen reader reads */}
          <Link className="navbar-brand terminal-brand" to="/">
            <span className="terminal-brand-accent" aria-hidden="true">
              {'//'}
            </span>
            <span>
              <span className="terminal-brand-name">Resonance Planner</span>
              <span className="terminal-brand-sub" aria-hidden="true">
                Wuthering Waves · planner
              </span>
            </span>
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
              {menuLinks(user).map((link, index) => (
                <li className="nav-item" key={link.to}>
                  <NavLink to={link.to} className={navLinkClass}>
                    <span className="nav-number" aria-hidden="true">
                      {String(index + 1).padStart(2, '0')}.
                    </span>
                    {link.label}
                  </NavLink>
                </li>
              ))}
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
