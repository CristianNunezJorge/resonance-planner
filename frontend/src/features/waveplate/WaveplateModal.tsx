import { useId, useRef, useState, type FormEvent } from 'react'
import type { RequestFailure } from '../../api/client'
import FormField from '../../components/FormField'
import Modal from '../../components/Modal'
import type { GameRules } from '../catalog/types'
import type { WaveplateReading } from './types'
import { CRYSTALS_KEY, validateWaveplate, WAVEPLATE_KEY } from './validation'

interface WaveplateModalProps {
  rules: GameRules
  // The values on the panel, null when none was saved yet
  initial: WaveplateReading | null
  onSave: (reading: WaveplateReading) => Promise<RequestFailure | null>
  onClose: () => void
}

// The server names the fields of PUT /api/me/waveplate value and crystals (D105)
const SERVER_FIELDS: Record<string, string> = { value: WAVEPLATE_KEY, crystals: CRYSTALS_KEY }

// "Aggiorna Waveplate" (F5): the values the user reads in the game. The page checks them first, the server decides.
function WaveplateModal({ rules, initial, onSave, onClose }: WaveplateModalProps) {
  const formId = useId()
  const [waveplate, setWaveplate] = useState(initial ? String(initial.value) : '')
  const [crystals, setCrystals] = useState(initial ? String(initial.crystals) : '')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [generalError, setGeneralError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const waveplateRef = useRef<HTMLInputElement>(null)
  const crystalsRef = useRef<HTMLInputElement>(null)

  // The keyboard goes to the first wrong field (WCAG 3.3.1)
  const showErrors = (found: Record<string, string>) => {
    setErrors(found)
    ;(found[WAVEPLATE_KEY] ? waveplateRef : crystalsRef).current?.focus()
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (saving) {
      return
    }
    const found = validateWaveplate({ waveplate, crystals }, rules)
    setGeneralError(null)
    if (Object.keys(found).length > 0) {
      showErrors(found)
      return
    }
    setSaving(true)
    const failure = await onSave({ value: Number(waveplate.trim()), crystals: Number(crystals.trim() || '0') })
    if (!failure) {
      onClose()
      return
    }
    setSaving(false)
    const fromServer = Object.fromEntries(
      Object.entries(failure.fields ?? {})
        .filter(([name]) => Object.hasOwn(SERVER_FIELDS, name))
        .map(([name, message]) => [SERVER_FIELDS[name], message]),
    )
    if (Object.keys(fromServer).length > 0) {
      showErrors(fromServer)
    } else {
      setErrors({})
      setGeneralError(failure.message)
    }
  }

  return (
    <Modal
      open
      title="Aggiorna le Waveplate"
      busy={saving}
      onClose={onClose}
      initialFocusRef={waveplateRef}
      footer={
        <>
          <button
            type="button"
            className="btn btn-outline-light"
            onClick={saving ? undefined : onClose}
            aria-disabled={saving || undefined}
          >
            Annulla
          </button>
          <button type="submit" form={formId} className="btn btn-primary" aria-disabled={saving || undefined}>
            {saving ? 'Salvataggio…' : 'Salva'}
          </button>
        </>
      }
    >
      <form id={formId} noValidate onSubmit={(event) => void handleSubmit(event)}>
        {generalError && (
          <div className="alert alert-danger" role="alert">
            {generalError}
          </div>
        )}
        <FormField
          id="waveplate-value"
          label="Waveplate attuali"
          value={waveplate}
          onChange={setWaveplate}
          inputMode="numeric"
          required
          hint={`Il valore che vedi nel gioco, da 0 a ${rules.waveplate.max}.`}
          error={errors[WAVEPLATE_KEY]}
          inputRef={waveplateRef}
        />
        <FormField
          id="waveplate-crystals"
          label="Waveplate Crystals attuali"
          value={crystals}
          onChange={setCrystals}
          inputMode="numeric"
          hint={`Da 0 a ${rules.waveplateCrystals.max}. Se non ne hai, lascia vuoto.`}
          error={errors[CRYSTALS_KEY]}
          inputRef={crystalsRef}
        />
      </form>
    </Modal>
  )
}

export default WaveplateModal
