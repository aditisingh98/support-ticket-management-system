import { useEffect, useState, type FormEvent } from 'react'
import { listTickets } from '../api/tickets'
import { userMessageForError } from '../api/client'
import type { Ticket, TicketStatus } from '../api/types'
import { Alert } from '../components/Alert'
import { EmptyState } from '../components/EmptyState'
import { LoadingState } from '../components/LoadingState'
import { PriorityBadge } from '../components/PriorityBadge'
import { StatusBadge } from '../components/StatusBadge'
import { formatDateTime, shortId } from '../lib/tickets'

interface TicketListPageProps {
  onNavigate: (path: string) => void
}

const STATUS_OPTIONS: Array<TicketStatus | ''> = [
  '',
  'OPEN',
  'IN_PROGRESS',
  'RESOLVED',
  'CLOSED',
  'CANCELLED',
]

export function TicketListPage({ onNavigate }: TicketListPageProps) {
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState<TicketStatus | ''>('')
  const [appliedKeyword, setAppliedKeyword] = useState('')
  const [appliedStatus, setAppliedStatus] = useState<TicketStatus | ''>('')
  const [tickets, setTickets] = useState<Ticket[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      setError(null)
      try {
        const data = await listTickets({
          keyword: appliedKeyword,
          status: appliedStatus,
        })
        if (!cancelled) {
          setTickets(data)
        }
      } catch (err) {
        if (!cancelled) {
          setError(userMessageForError(err))
          setTickets([])
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
  }, [appliedKeyword, appliedStatus])

  function applyFilters(event: FormEvent) {
    event.preventDefault()
    setAppliedKeyword(keyword)
    setAppliedStatus(status)
  }

  function clearFilters() {
    setKeyword('')
    setStatus('')
    setAppliedKeyword('')
    setAppliedStatus('')
  }

  const hasFilters = Boolean(appliedKeyword || appliedStatus)

  return (
    <div className="page-stack">
      <section className="panel">
        <div className="panel-header">
          <div>
            <h2>Support Tickets</h2>
            <p className="muted">
              Search and filter the ticket queue. Open a row to view details, comments, and status
              actions.
            </p>
          </div>
          <button type="button" className="btn btn-primary" onClick={() => onNavigate('/tickets/new')}>
            Create Ticket
          </button>
        </div>

        <form className="filter-bar" onSubmit={applyFilters}>
          <label className="field">
            <span>Keyword</span>
            <input
              type="search"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder="Search title or description"
              aria-label="Keyword search"
            />
          </label>
          <label className="field">
            <span>Status</span>
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value as TicketStatus | '')}
              aria-label="Status filter"
            >
              {STATUS_OPTIONS.map((option) => (
                <option key={option || 'all'} value={option}>
                  {option === '' ? 'All statuses' : option.replace('_', ' ')}
                </option>
              ))}
            </select>
          </label>
          <div className="filter-actions">
            <button type="submit" className="btn btn-secondary">
              Apply
            </button>
            <button type="button" className="btn btn-ghost" onClick={clearFilters} disabled={!hasFilters && !keyword && !status}>
              Clear
            </button>
          </div>
        </form>
      </section>

      {error ? <Alert tone="error">{error}</Alert> : null}

      <section className="panel">
        {loading ? (
          <LoadingState label="Loading tickets…" />
        ) : tickets.length === 0 ? (
          <EmptyState
            title={hasFilters ? 'No search results' : 'No tickets yet'}
            description={
              hasFilters
                ? 'Try a different keyword or status filter.'
                : 'Create a ticket to start managing support work.'
            }
            action={
              !hasFilters ? (
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={() => onNavigate('/tickets/new')}
                >
                  Create ticket
                </button>
              ) : undefined
            }
          />
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th scope="col">Ticket ID</th>
                  <th scope="col">Title</th>
                  <th scope="col">Priority</th>
                  <th scope="col">Status</th>
                  <th scope="col">Assignee</th>
                  <th scope="col">Created</th>
                  <th scope="col">Updated</th>
                  <th scope="col">Action</th>
                </tr>
              </thead>
              <tbody>
                {tickets.map((ticket) => (
                  <tr key={ticket.id}>
                    <td>
                      <code title={ticket.id}>{shortId(ticket.id)}</code>
                    </td>
                    <td className="cell-title">{ticket.title}</td>
                    <td>
                      <PriorityBadge priority={ticket.priority} />
                    </td>
                    <td>
                      <StatusBadge status={ticket.status} />
                    </td>
                    <td>{ticket.assignee ?? '—'}</td>
                    <td>{formatDateTime(ticket.createdAt)}</td>
                    <td>{formatDateTime(ticket.updatedAt)}</td>
                    <td>
                      <button
                        type="button"
                        className="btn btn-link"
                        onClick={() => onNavigate(`/tickets/${ticket.id}`)}
                      >
                        View
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}
