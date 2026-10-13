import { useEffect, useRef } from 'react'
import { Outlet, useLocation } from 'react-router'
import Footer from '../components/Footer'
import NavBar from '../components/NavBar'
import ToastContainer from '../components/ToastContainer'

// Frame of every page: skip link, navigation bar, content, footer and notifications.
// The footer stays at the bottom of the window even on short pages.
function RootLayout() {
  const { pathname } = useLocation()
  const mainRef = useRef<HTMLElement>(null)
  const previousPathname = useRef(pathname)

  // In a single-page app the browser does not move the focus when the page changes. After a login, a redirect or a
  // logout the element that had the focus is gone: the focus goes to the content, so a keyboard or screen reader
  // user starts from the new page and not from the top of the document (WCAG 2.4.3).
  useEffect(() => {
    if (previousPathname.current !== pathname) {
      previousPathname.current = pathname
      mainRef.current?.focus()
    }
  }, [pathname])

  return (
    <div className="d-flex flex-column min-vh-100">
      <a className="visually-hidden-focusable" href="#contenuto">
        Vai al contenuto
      </a>
      <NavBar />
      <main id="contenuto" ref={mainRef} className="container py-4 flex-grow-1" tabIndex={-1}>
        <Outlet />
      </main>
      <Footer />
      <ToastContainer />
    </div>
  )
}

export default RootLayout
