import { useRef, useState, type FormEvent } from 'react'
import { Link, useLocation } from 'react-router'
import { describeError, type RequestFailure } from '../../api/client'
import { useAppDispatch } from '../../app/hooks'
import FormField from '../../components/FormField'
import { usePageTitle } from '../../components/usePageTitle'
import { register } from './authSlice'
import { generalErrorMessage, useFocusOnFailure } from './formFailure'

const FIELDS = ['username', 'email', 'password']

// Page P2, registration (F1). The server checks every field and the page shows each message under its input.
function RegisterPage() {
  usePageTitle('Registrati')
  const dispatch = useAppDispatch()
  const location = useLocation()
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [failure, setFailure] = useState<RequestFailure | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const usernameRef = useRef<HTMLInputElement>(null)
  const emailRef = useRef<HTMLInputElement>(null)
  const passwordRef = useRef<HTMLInputElement>(null)
  const alertRef = useRef<HTMLDivElement>(null)

  useFocusOnFailure(failure, [['username', usernameRef], ['email', emailRef], ['password', passwordRef]], alertRef)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setIsSubmitting(true)
    setFailure(null)
    const result = await dispatch(register({ username, email, password }))
    // After a success the user is logged in and GuestOnly sends them to the guided setup
    if (register.rejected.match(result)) {
      setFailure(result.payload ?? describeError(undefined))
      setIsSubmitting(false)
    }
  }

  const alertMessage = generalErrorMessage(failure, FIELDS)

  return (
    <section className="row justify-content-center">
      <div className="col-12 col-md-8 col-lg-5">
        <h1>Registrati</h1>
        {alertMessage && (
          <div ref={alertRef} className="alert alert-danger" role="alert" tabIndex={-1}>
            {alertMessage}
          </div>
        )}
        {/* method="post": even if the script did not run, the password could never end up in the address of a GET */}
        <form method="post" onSubmit={(event) => void handleSubmit(event)} noValidate>
          <FormField
            id="username"
            label="Username"
            autoComplete="username"
            required
            hint="Da 3 a 30 caratteri: lettere, numeri, punto, trattino e underscore."
            value={username}
            onChange={setUsername}
            error={failure?.fields?.username}
            inputRef={usernameRef}
          />
          <FormField
            id="email"
            label="Email"
            type="email"
            autoComplete="email"
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
            autoComplete="new-password"
            required
            hint="Almeno 8 caratteri."
            value={password}
            onChange={setPassword}
            error={failure?.fields?.password}
            inputRef={passwordRef}
          />
          <button type="submit" className="btn btn-primary w-100" disabled={isSubmitting}>
            {isSubmitting ? 'Registrazione in corso…' : 'Registrati'}
          </button>
        </form>
        <p className="mt-3 mb-0 text-center">
          {/* The page that was asked for before the registration is kept if the user goes to log in instead */}
          Hai già un account?{' '}
          <Link to="/login" state={location.state}>
            Accedi
          </Link>
        </p>
      </div>
    </section>
  )
}

export default RegisterPage
