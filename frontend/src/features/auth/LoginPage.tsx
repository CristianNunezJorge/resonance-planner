import { useRef, useState, type FormEvent } from 'react'
import { Link, useLocation } from 'react-router'
import { describeError, type RequestFailure } from '../../api/client'
import { useAppDispatch } from '../../app/hooks'
import FormField from '../../components/FormField'
import { usePageTitle } from '../../components/usePageTitle'
import { login } from './authSlice'
import { generalErrorMessage, useFocusOnFailure } from './formFailure'

const FIELDS = ['email', 'password']

// Page P2, login (F2). A wrong email or password gets one message for both, as the server says (D46).
function LoginPage() {
  usePageTitle('Accedi')
  const dispatch = useAppDispatch()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [failure, setFailure] = useState<RequestFailure | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const emailRef = useRef<HTMLInputElement>(null)
  const passwordRef = useRef<HTMLInputElement>(null)
  const alertRef = useRef<HTMLDivElement>(null)

  useFocusOnFailure(failure, [['email', emailRef], ['password', passwordRef]], alertRef)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setIsSubmitting(true)
    setFailure(null)
    const result = await dispatch(login({ email, password }))
    // After a success the page is replaced by the one the user was going to (GuestOnly)
    if (login.rejected.match(result)) {
      setFailure(result.payload ?? describeError(undefined))
      setIsSubmitting(false)
    }
  }

  const alertMessage = generalErrorMessage(failure, FIELDS)

  return (
    <section className="row justify-content-center">
      <div className="col-12 col-md-8 col-lg-5">
        <h1>Accedi</h1>
        {alertMessage && (
          <div ref={alertRef} className="alert alert-danger" role="alert" tabIndex={-1}>
            {alertMessage}
          </div>
        )}
        {/* method="post": even if the script did not run, the password could never end up in the address of a GET */}
        <form method="post" onSubmit={(event) => void handleSubmit(event)} noValidate>
          <FormField
            id="email"
            label="Email"
            type="email"
            // "username" is what password managers look for next to a "current-password" field
            autoComplete="username"
            required
            value={email}
            onChange={setEmail}
            error={failure?.fields?.email}
            inputRef={emailRef}
          />
          <FormField
            id="password"
            label="Password"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={setPassword}
            error={failure?.fields?.password}
            inputRef={passwordRef}
          />
          <button type="submit" className="btn btn-primary w-100" disabled={isSubmitting}>
            {isSubmitting ? 'Accesso in corso…' : 'Accedi'}
          </button>
        </form>
        <p className="mt-3 mb-0 text-center">
          {/* The page that was asked for before the login is kept if the user goes to register instead */}
          Non hai un account?{' '}
          <Link to="/registrati" state={location.state}>
            Registrati
          </Link>
        </p>
      </div>
    </section>
  )
}

export default LoginPage
