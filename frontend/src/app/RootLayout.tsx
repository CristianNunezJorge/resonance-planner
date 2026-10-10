import { Outlet } from 'react-router'

// Shared page frame: the navbar and the footer arrive with T15
function RootLayout() {
  return (
    <main className="container py-4">
      <Outlet />
    </main>
  )
}

export default RootLayout
