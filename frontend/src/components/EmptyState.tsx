import type { ReactNode } from 'react'

interface EmptyStateProps {
  message: string
  // What the user can do about it, for example a button that adds the first item
  children?: ReactNode
}

// A page or a list with nothing to show yet (D69): a short sentence, never an empty area
function EmptyState({ message, children }: EmptyStateProps) {
  return (
    <div className="text-center py-4">
      <p className="text-body-secondary mb-3">{message}</p>
      {children}
    </div>
  )
}

export default EmptyState
