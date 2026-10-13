import { Navigate, Outlet, useLocation } from 'react-router'
import { useAppSelector } from '../../app/hooks'
import LoadingState from '../../components/LoadingState'
import { homePath, requestedPath } from './navigation'

/**
 * Login and registration are for visitors (D73). Someone who is already logged in, or has just logged in,
 * goes to the page that was asked for before the login, or to the home of their account.
 */
function GuestOnly() {
  const location = useLocation()
  const { user, sessionStatus } = useAppSelector((state) => state.auth)

  if (sessionStatus === 'restoring') {
    return <LoadingState />
  }
  if (user) {
    return <Navigate to={requestedPath(location.state) ?? homePath(user)} replace />
  }
  return <Outlet />
}

export default GuestOnly
