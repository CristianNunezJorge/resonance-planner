import { useEffect, useId, useRef, useState, type FormEvent } from 'react'
import { describeError } from '../../api/client'
import { useAppDispatch, useAppSelector } from '../../app/hooks'
import { toastShown } from '../../app/toastSlice'
import ErrorState from '../../components/ErrorState'
import FormField from '../../components/FormField'
import { usePageTitle } from '../../components/usePageTitle'
import { userUpdated } from '../auth/authSlice'
import type { User } from '../auth/types'
import { useLogout } from '../auth/useLogout'
import { fetchRules } from '../catalog/catalogSlice'
import { formatPlayTime } from '../progress/levels'
import DeleteAccountModal from './DeleteAccountModal'
import { deleteAccount, updateProfile, type ProfileChanges } from './profileApi'

type FieldName = 'username' | 'playTimeMinutes'

const PLAY_TIME_ERROR_ID = 'profile-play-time-error'

// The changes to send (D121): only what is different from the account, so an unchanged username is never checked
function changesOf(user: User, username: string, playTimeMinutes: number | null): ProfileChanges {
  const changes: ProfileChanges = {}
  if (username.trim() !== user.username) {
    changes.username = username.trim()
  }
  if (playTimeMinutes !== null && playTimeMinutes !== user.playTimeMinutes) {
    changes.playTimeMinutes = playTimeMinutes
  }
  return changes
}

// Page P6, the profile (D102, D121): username and play time, the logout, and the deletion of the account (F12).
// The route is protected, but the session can end in another tab while the page is open: then nothing is drawn
// until the route sends the visitor away.
function ProfilePage() {
  usePageTitle('Profilo')
  const user = useAppSelector((state) => state.auth.user)
  // A new user (a login in another tab) gets a new form, without the values typed for the one before
  return user ? <ProfileContent key={user.id} user={user} /> : null
}

function ProfileContent({ user }: { user: User }) {
  const dispatch = useAppDispatch()
  const logout = useLogout()
  const token = useAppSelector((state) => state.auth.token)
  const rules = useAppSelector((state) => state.catalog.rules)
  const rulesStatus = useAppSelector((state) => state.catalog.rulesStatus)
  const rulesError = useAppSelector((state) => state.catalog.rulesError)
  const [username, setUsername] = useState(user.username)
  const [playTimeMinutes, setPlayTimeMinutes] = useState(user.playTimeMinutes)
  const [errors, setErrors] = useState<Partial<Record<FieldName, string>>>({})
  const [generalError, setGeneralError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const usernameRef = useRef<HTMLInputElement>(null)
  const playTimeRef = useRef<HTMLFieldSetElement>(null)
  const accountTitleId = useId()
  const dangerTitleId = useId()

  useEffect(() => {
    void dispatch(fetchRules())
  }, [dispatch])

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (saving || !token) {
      return
    }
    setErrors({})
    setGeneralError(null)
    const changes = changesOf(user, username, playTimeMinutes)
    if (Object.keys(changes).length === 0) {
      dispatch(toastShown('Non ci sono modifiche da salvare.', 'info'))
      return
    }
    setSaving(true)
    try {
      const updated = await updateProfile(token, changes)
      dispatch(userUpdated(updated))
      // The form shows what was saved: the server took away the spaces around the username
      setUsername(updated.username)
      dispatch(toastShown('Profilo aggiornato.'))
    } catch (error) {
      const failure = describeError(error)
      const found: Partial<Record<FieldName, string>> = {}
      if (failure.fields?.username) {
        found.username = failure.fields.username
      }
      if (failure.fields?.playTimeMinutes) {
        found.playTimeMinutes = failure.fields.playTimeMinutes
      }
      setErrors(found)
      if (found.username) {
        usernameRef.current?.focus()
      } else if (found.playTimeMinutes) {
        playTimeRef.current?.querySelector('input')?.focus()
      } else {
        setGeneralError(failure.message)
      }
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (): Promise<string | null> => {
    if (!token) {
      return null
    }
    try {
      await deleteAccount(token)
    } catch (error) {
      return describeError(error).message
    }
    // The same way out as "Esci": first the presentation, then the session ends (F12). The account is gone anyway,
    // and useLogout ends the session even if the navigation fails: nothing is left to tell the window
    try {
      await logout('Account eliminato: i tuoi dati sono stati cancellati.')
    } catch {
      // the session is already over
    }
    return null
  }

  return (
    <section>
      <h1>Profilo</h1>
      <section className="card mb-4" aria-labelledby={accountTitleId}>
        <div className="card-body">
          <h2 id={accountTitleId} className="h5">
            Il tuo account
          </h2>
          <form noValidate onSubmit={(event) => void handleSubmit(event)}>
            {generalError && (
              <div className="alert alert-danger" role="alert">
                {generalError}
              </div>
            )}
            <FormField
              id="profile-username"
              label="Username"
              value={username}
              onChange={setUsername}
              autoComplete="username"
              required
              hint="Da 3 a 30 caratteri: lettere, numeri, punto, trattino e underscore."
              error={errors.username}
              inputRef={usernameRef}
            />
            <div className="mb-3">
              <p className="form-label mb-1">Email</p>
              <p className="mb-0 text-break">{user.email}</p>
              <p className="form-text mt-0">L’email non si può cambiare.</p>
            </div>
            {rulesStatus === 'failed' && rulesError && (
              <ErrorState
                message={`Non è stato possibile caricare le scelte del tempo di gioco. ${rulesError}`}
                onRetry={() => void dispatch(fetchRules())}
              />
            )}
            {rules && (
              <fieldset ref={playTimeRef} className="mb-3">
                <legend className="form-label fs-6">Quanto giochi al giorno?</legend>
                {rules.playTimeOptionsMinutes.map((minutes) => {
                  const inputId = `profile-play-time-${minutes}`
                  return (
                    <div className="form-check" key={minutes}>
                      <input
                        id={inputId}
                        type="radio"
                        name="profile-play-time"
                        className="form-check-input"
                        value={minutes}
                        checked={playTimeMinutes === minutes}
                        onChange={() => setPlayTimeMinutes(minutes)}
                        aria-invalid={errors.playTimeMinutes ? true : undefined}
                        aria-describedby={errors.playTimeMinutes ? PLAY_TIME_ERROR_ID : undefined}
                      />
                      <label htmlFor={inputId} className="form-check-label">
                        {formatPlayTime(minutes)}
                      </label>
                    </div>
                  )
                })}
                {errors.playTimeMinutes && (
                  <p id={PLAY_TIME_ERROR_ID} className="text-danger-emphasis small mt-1 mb-0">
                    {errors.playTimeMinutes}
                  </p>
                )}
              </fieldset>
            )}
            <div className="d-flex flex-wrap gap-2">
              <button type="submit" className="btn btn-primary" aria-disabled={saving || undefined}>
                {saving ? 'Salvataggio…' : 'Salva'}
              </button>
              <button
                type="button"
                className="btn btn-outline-light"
                onClick={() => void logout('Sei uscito dall\'account.')}
              >
                Esci
              </button>
            </div>
          </form>
        </div>
      </section>
      <section className="card border-danger" aria-labelledby={dangerTitleId}>
        <div className="card-body">
          <h2 id={dangerTitleId} className="h5 text-danger-emphasis">
            Zona pericolosa
          </h2>
          <p>Eliminando l’account cancelli anche roster, inventario, attività e Waveplate.</p>
          <button type="button" className="btn btn-outline-danger" onClick={() => setDeleting(true)}>
            Elimina account
          </button>
        </div>
      </section>
      {deleting && (
        <DeleteAccountModal email={user.email} onDelete={handleDelete} onClose={() => setDeleting(false)} />
      )}
    </section>
  )
}

export default ProfilePage
