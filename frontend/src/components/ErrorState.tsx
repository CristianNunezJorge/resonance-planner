interface ErrorStateProps {
  message: string
  onRetry?: () => void
}

// Error state of every page (D69): the message of the API and, when it makes sense, a way to try again
function ErrorState({ message, onRetry }: ErrorStateProps) {
  return (
    <div className="alert alert-danger d-flex flex-wrap align-items-center gap-3" role="alert">
      <span>{message}</span>
      {onRetry && (
        <button type="button" className="btn btn-sm btn-outline-light ms-auto" onClick={onRetry}>
          Riprova
        </button>
      )}
    </div>
  )
}

export default ErrorState
