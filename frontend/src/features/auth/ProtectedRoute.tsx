import { Navigate, Outlet, useLocation } from 'react-router'
import { useAppDispatch, useAppSelector } from '../../app/hooks'
import ErrorState from '../../components/ErrorState'
import LoadingState from '../../components/LoadingState'
import RedirectWithToast from '../../components/RedirectWithToast'
import { retrySession } from './authSlice'
import { CALENDAR_PATH, homePath, LOGIN_PATH, SETUP_PATH } from './navigation'
import type { Role } from './types'

// complete: only users who finished the guided setup · incomplete: only those who still have to do it
type OnboardingRule = 'any' | 'complete' | 'incomplete'

interface ProtectedRouteProps {
  role?: Role
  onboarding?: OnboardingRule
}

/**
 * Access rules of the pages (D73). They only guide the user to the right page: the real controls are
 * in the backend (D44), which checks every request.
 */
function ProtectedRoute({ role, onboarding = 'any' }: ProtectedRouteProps) {
  const location = useLocation()
  const dispatch = useAppDispatch()
  const { user, sessionStatus, sessionError } = useAppSelector((state) => state.auth)

  if (sessionStatus === 'restoring') {
    return <LoadingState />
  }
  if (sessionStatus === 'failed' && !user) {
    // The saved session could not be checked (server not reachable): the token is still saved, so retry
    return (
      <ErrorState
        message={sessionError ?? 'Impossibile ripristinare la sessione.'}
        onRetry={() => dispatch(retrySession())}
      />
    )
  }
  if (!user) {
    return <Navigate to={LOGIN_PATH} replace state={{ from: location }} />
  }
  if (role && user.role !== role) {
    return <RedirectWithToast to={homePath(user)} message="Area riservata." variant="danger" />
  }
  if (onboarding === 'complete' && !user.onboardingCompleted) {
    return <Navigate to={SETUP_PATH} replace />
  }
  if (onboarding === 'incomplete' && user.onboardingCompleted) {
    return <Navigate to={CALENDAR_PATH} replace />
  }
  return <Outlet />
}

export default ProtectedRoute
