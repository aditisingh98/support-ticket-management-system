import { useEffect, useMemo, useState } from 'react'
import { listTickets } from '../api/tickets'
import { userMessageForError } from '../api/client'
import type { Ticket, TicketStatus } from '../api/types'
import { Alert } from '../components/Alert'
import { EmptyState } from '../components/EmptyState'
import { LoadingState } from '../components/LoadingState'
import { StatusBadge } from '../components/StatusBadge'

interface DashboardPageProps {
  onNavigate: (path: string) => void
}

const STATUS_ORDER: TicketStatus[] = [
  'OPEN',
  'IN_PROGRESS',
  'RESOLVED',
  'CLOSED',
  'CANCELLED',
]

export function DashboardPage({ onNavigate }: DashboardPageProps) {
  const [tickets, setTickets] = useState<Ticket[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      setError(null)
      try {
        const data = await listTickets()
        if (!cancelled) {
          setTickets(data)
        }
      } catch (err) {
        if (!cancelled) {
          setError(userMessageForError(err))
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }
    void load()
    return () => {
      cancelled = true
    }
  }, [])

  const counts = useMemo(() => {
    const map: Record<TicketStatus, number> = {
      OPEN: 0,
      IN_PROGRESS: 0,
      RESOLVED: 0,
      CLOSED: 0,
      CANCELLED: 0,
    }
    for (const ticket of tickets) {
      map[ticket.status] += 1
    }
    return map
  }, [tickets])

  const recent = tickets.slice(0, 5)

  if (loading) {
    return <LoadingState label="Loading dashboard…" />
  }

  return (
    <div className="page-stack">
      {error ? <Alert tone="error">{error}</Alert> : null}

      <section className="summary-grid" aria-label="Ticket summary">
        <article className="summary-card summary-total">
          <p className="summary-label">Total Tickets</p>
          <p className="summary-value">{tickets.length}</p>
        </article>
        {STATUS_ORDER.map((status) => (
          <article key={status} className={`summary-card summary-${status.toLowerCase()}`}>
            <p className="summary-label">{status.replace('_', ' ')}</p>
            <p className="summary-value">{counts[status]}</p>
          </article>
        ))}
      </section>

      <section className="panel">
        <div className="panel-header">
          <div>
            <h2>Quick actions</h2>
            <p className="muted">Create a ticket or review the full queue.</p>
          </div>
          <div className="action-row">
            <button type="button" className="btn btn-secondary" onClick={() => onNavigate('/tickets')}>
              View tickets
            </button>
            <button type="button" className="btn btn-primary" onClick={() => onNavigate('/tickets/new')}>
              Create ticket
            </button>
          </div>
        </div>
      </section>

      <section className="panel">
        <div className="panel-header">
          <div>
            <h2>Recent tickets</h2>
            <p className="muted">Newest tickets from the current queue.</p>
          </div>
        </div>
        {recent.length === 0 ? (
          <EmptyState
            title="No tickets yet"
            description="Create the first support ticket to populate the portal."
            action={
              <button type="button" className="btn btn-primary" onClick={() => onNavigate('/tickets/new')}>
                Create ticket
              </button>
            }
          />
        ) : (
          <ul className="recent-list">
            {recent.map((ticket) => (
              <li key={ticket.id}>
                <button
                  type="button"
                  className="recent-item"
                  onClick={() => onNavigate(`/tickets/${ticket.id}`)}
                >
                  <span className="recent-title">{ticket.title}</span>
                  <StatusBadge status={ticket.status} />
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
