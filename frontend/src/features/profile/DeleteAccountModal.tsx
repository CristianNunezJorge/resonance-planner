import { useId, useRef, useState, type FormEvent } from 'react'
import FormField from '../../components/FormField'
import Modal from '../../components/Modal'
import { emailMatches } from './emailMatches'

interface DeleteAccountModalProps {
  email: string
  // Deletes the account; the message of a failure, or null if it worked (then the page leaves at once)
  onDelete: () => Promise<string | null>
  onClose: () => void
}

// F12, D42, D121: the account goes away only after the user writes their own email, because it cannot be undone
function DeleteAccountModal({ email, onDelete, onClose }: DeleteAccountModalProps) {
  const formId = useId()
  const messageId = useId()
  const [typed, setTyped] = useState('')
  const [deleting, setDeleting] = useState(false)
  const [failure, setFailure] = useState<string | null>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const matches = emailMatches(typed, email)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!matches || deleting) {
      return
    }
    setDeleting(true)
    setFailure(null)
    const message = await onDelete()
    if (message) {
      setDeleting(false)
      setFailure(message)
    }
  }

  return (
    <Modal
      open
      title="Eliminare l’account?"
      busy={deleting}
      onClose={onClose}
      initialFocusRef={inputRef}
      describedBy={messageId}
      footer={
        <>
          <button
            type="button"
            className="btn btn-outline-light"
            onClick={deleting ? undefined : onClose}
            aria-disabled={deleting || undefined}
          >
            Annulla
          </button>
          {/* aria-disabled and not disabled, as in the other windows: the button stays reachable, and the submit stops
              until the email matches */}
          <button
            type="submit"
            form={formId}
            className="btn btn-danger"
            aria-disabled={!matches || deleting || undefined}
          >
            {deleting ? 'Eliminazione…' : 'Elimina definitivamente'}
          </button>
        </>
      }
    >
      <form id={formId} noValidate onSubmit={(event) => void handleSubmit(event)}>
        <p id={messageId}>
          Spariscono l’account e tutti i suoi dati: roster, inventario, attività e Waveplate. Non si può annullare. Per
          confermare scrivi la tua email, <strong className="text-break">{email}</strong>.
        </p>
        {failure && (
          <div className="alert alert-danger" role="alert">
            {failure}
          </div>
        )}
        <FormField
          id="delete-email"
          label="La tua email"
          type="email"
          value={typed}
          onChange={setTyped}
          autoComplete="off"
          inputRef={inputRef}
        />
      </form>
    </Modal>
  )
}

export default DeleteAccountModal
