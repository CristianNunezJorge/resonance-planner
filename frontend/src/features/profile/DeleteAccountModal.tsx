import { useId, useRef, useState, type FormEvent } from 'react'
import FormField from '../../components/FormField'
import Modal from '../../components/Modal'
import { emailMatches } from './emailMatches'

interface DeleteAccountModalProps {
  email: string
  // A guest of the demo (D134) does not know the address of its account: it writes its name instead
  guestName?: string
  // Deletes the account; the message of a failure, or null if it worked (then the page leaves at once)
  onDelete: () => Promise<string | null>
  onClose: () => void
}

// F12, D42, D121: the account goes away only after the user writes their own email, because it cannot be undone
function DeleteAccountModal({ email, guestName, onDelete, onClose }: DeleteAccountModalProps) {
  const formId = useId()
  const messageId = useId()
  const [typed, setTyped] = useState('')
  const [deleting, setDeleting] = useState(false)
  const [failure, setFailure] = useState<string | null>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const matches = guestName === undefined ? emailMatches(typed, email) : typed.trim() === guestName

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
          confermare scrivi {guestName === undefined ? 'la tua email' : 'il tuo nome'},{' '}
          <strong className="text-break">{guestName ?? email}</strong>.
        </p>
        {failure && (
          <div className="alert alert-danger" role="alert">
            {failure}
          </div>
        )}
        <FormField
          id="delete-confirmation"
          label={guestName === undefined ? 'La tua email' : 'Il tuo nome'}
          type={guestName === undefined ? 'email' : 'text'}
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
