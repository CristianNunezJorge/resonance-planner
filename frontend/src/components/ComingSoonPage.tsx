import { usePageTitle } from './usePageTitle'

interface ComingSoonPageProps {
  title: string
}

// Stands in for a page that a later card builds, so the access rules (D73) work from now on
function ComingSoonPage({ title }: ComingSoonPageProps) {
  usePageTitle(title)

  return (
    <section>
      <h1>{title}</h1>
      <p className="text-body-secondary">Questa pagina è in costruzione.</p>
    </section>
  )
}

export default ComingSoonPage
