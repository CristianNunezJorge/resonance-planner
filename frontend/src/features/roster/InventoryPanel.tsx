import { useState } from 'react'
import { useAppDispatch, useAppSelector } from '../../app/hooks'
import ErrorState from '../../components/ErrorState'
import LoadingState from '../../components/LoadingState'
import { fetchMaterials } from '../catalog/catalogSlice'
import type { MaterialCategory, MaterialSummary } from '../catalog/types'
import { formatNumber } from './format'
import { saveQuantity } from './rosterSlice'

// The groups of the table, in the order a player looks for them
const GROUPS: { category: MaterialCategory; title: string }[] = [
  { category: 'CREDIT', title: 'Crediti' },
  { category: 'CHARACTER_EXP', title: 'Pozioni EXP' },
  { category: 'ENEMY_DROP', title: 'Drop dei nemici comuni' },
  { category: 'BOSS_DROP', title: 'Drop dei boss' },
  { category: 'LOCAL_SPECIALTY', title: 'Specialità locali' },
]

// The same limit of the server (D108)
const MAX_QUANTITY = 999_999_999
const WHOLE_NUMBER = /^\d+$/
const INVALID_QUANTITY = `Inserisci un numero intero da 0 a ${formatNumber(MAX_QUANTITY)}.`

// The tab "Inventario" of P5: every material of the catalog with the quantity the user owns (0 if never saved)
function InventoryPanel() {
  const dispatch = useAppDispatch()
  const { materials, materialsStatus, materialsError } = useAppSelector((state) => state.catalog)
  const inventory = useAppSelector((state) => state.roster.inventory)
  // One live region for the whole table: "Salvato" is announced once, with the name of the material
  const [announcement, setAnnouncement] = useState('')

  if (materialsStatus === 'failed') {
    return <ErrorState message={materialsError ?? ''} onRetry={() => void dispatch(fetchMaterials())} />
  }
  if (materialsStatus !== 'succeeded') {
    return <LoadingState />
  }

  return (
    <div>
      <h2 className="h4">I tuoi materiali</h2>
      <p className="text-body-secondary">
        Scrivi quanti ne hai nel gioco: la quantità si salva quando esci dal campo, e i calcoli si aggiornano.
      </p>
      <p role="status" className="visually-hidden">
        {announcement}
      </p>
      {GROUPS.map(({ category, title }) => {
        const rows = materials.filter((material) => material.category === category)
        if (rows.length === 0) {
          return null
        }
        return (
          <table key={category} className="table align-middle mb-4">
            <caption className="caption-top fw-semibold text-body">{title}</caption>
            <thead>
              <tr>
                <th scope="col">Materiale</th>
                <th scope="col" className="inventory-quantity">
                  Quantità
                </th>
              </tr>
            </thead>
            <tbody>
              {rows.map((material) => (
                <InventoryRow
                  key={material.id}
                  material={material}
                  saved={Object.hasOwn(inventory, material.id) ? inventory[material.id] : 0}
                  onSaved={(quantity) => setAnnouncement(`${material.name}: ${formatNumber(quantity)} salvato.`)}
                />
              ))}
            </tbody>
          </table>
        )
      })}
    </div>
  )
}

interface InventoryRowProps {
  material: MaterialSummary
  saved: number
  onSaved: (quantity: number) => void
}

type RowStatus = 'idle' | 'saving' | 'saved'

// One material: the quantity is saved when the field is left, only if it changed and is a whole number (D108).
// While it is saved the field is read-only, so two saves of the same field never overlap and nothing typed is lost.
// The row keeps what it saved: the panel stays mounted when the tab changes, so a row is never drawn again with an
// older quantity of the store (the page loads everything again when it opens, D115).
function InventoryRow({ material, saved, onSaved }: InventoryRowProps) {
  const dispatch = useAppDispatch()
  const [value, setValue] = useState(String(saved))
  const [lastSaved, setLastSaved] = useState(saved)
  const [status, setStatus] = useState<RowStatus>('idle')
  const [error, setError] = useState<string | null>(null)
  const inputId = `inventory-${material.id}`
  const errorId = `${inputId}-error`
  const statusId = `${inputId}-status`

  const handleBlur = async () => {
    if (status === 'saving') {
      return
    }
    const text = value.trim()
    if (!WHOLE_NUMBER.test(text) || Number(text) > MAX_QUANTITY) {
      setError(INVALID_QUANTITY)
      setStatus('idle')
      return
    }
    const quantity = Number(text)
    // "007" and " 7 " are written as 7 before anything is sent
    setValue(String(quantity))
    setError(null)
    if (quantity === lastSaved) {
      return
    }
    setStatus('saving')
    const result = await dispatch(saveQuantity({ materialId: material.id, quantity }))
    if (saveQuantity.rejected.match(result)) {
      setStatus('idle')
      setError(result.payload?.fields?.quantity ?? result.payload?.message ?? INVALID_QUANTITY)
      return
    }
    setLastSaved(quantity)
    setStatus('saved')
    onSaved(quantity)
  }

  const describedBy = [error ? errorId : null, statusId].filter(Boolean).join(' ')

  return (
    <tr>
      <th scope="row" className="fw-normal">
        <label htmlFor={inputId} className="mb-0">
          {material.name}
        </label>
        {material.exp !== null && (
          <span className="d-block small text-body-secondary">{formatNumber(material.exp)} EXP l'una</span>
        )}
      </th>
      <td className="inventory-quantity">
        <input
          id={inputId}
          type="text"
          inputMode="numeric"
          className={error ? 'form-control is-invalid' : 'form-control'}
          value={value}
          onChange={(event) => {
            setValue(event.target.value)
            setStatus('idle')
          }}
          onBlur={() => void handleBlur()}
          readOnly={status === 'saving'}
          aria-busy={status === 'saving' || undefined}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy}
        />
        {/* The error appears when the field is left: role="alert" reads it even if the focus is elsewhere */}
        {error && (
          <div id={errorId} role="alert" className="invalid-feedback">
            {error}
          </div>
        )}
        <span id={statusId} className="small text-success-emphasis">
          {status === 'saving' ? 'Salvataggio…' : status === 'saved' ? 'Salvato' : ''}
        </span>
      </td>
    </tr>
  )
}

export default InventoryPanel
