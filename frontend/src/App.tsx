import { AppShell } from './components/AppShell'
import { Alert } from './components/Alert'
import { matchRoute, useRouter } from './lib/router'
import { CreateTicketPage } from './pages/CreateTicketPage'
import { DashboardPage } from './pages/DashboardPage'
import { TicketDetailPage } from './pages/TicketDetailPage'
import { TicketListPage } from './pages/TicketListPage'
import './App.css'

function pageMeta(route: ReturnType<typeof matchRoute>): {
  title: string
  context?: string
} {
  switch (route.name) {
    case 'dashboard':
      return {
        title: 'Dashboard',
        context: 'Queue overview from current ticket data',
      }
    case 'tickets':
      return {
        title: 'Support Tickets',
        context: 'Search, filter, and open tickets',
      }
    case 'create':
      return {
        title: 'Create Ticket',
        context: 'New tickets start as OPEN',
      }
    case 'detail':
      return {
        title: 'Ticket details',
        context: `Ticket ${route.ticketId}`,
      }
    default:
      return { title: 'Not found' }
  }
}

function App() {
  const { pathname, navigate } = useRouter()
  const route = matchRoute(pathname)
  const meta = pageMeta(route)

  let content
  switch (route.name) {
    case 'dashboard':
      content = <DashboardPage onNavigate={navigate} />
      break
    case 'tickets':
      content = <TicketListPage onNavigate={navigate} />
      break
    case 'create':
      content = <CreateTicketPage onNavigate={navigate} />
      break
    case 'detail':
      content = <TicketDetailPage ticketId={route.ticketId} onNavigate={navigate} />
      break
    default:
      content = (
        <div className="page-stack">
          <Alert tone="error" title="Page not found">
            That route is not part of the Support Ticket portal.
          </Alert>
          <button type="button" className="btn btn-secondary" onClick={() => navigate('/')}>
            Go to dashboard
          </button>
        </div>
      )
  }

  return (
    <AppShell
      currentPath={pathname}
      pageTitle={meta.title}
      pageContext={meta.context}
      onNavigate={navigate}
    >
      {content}
    </AppShell>
  )
}

export default App
