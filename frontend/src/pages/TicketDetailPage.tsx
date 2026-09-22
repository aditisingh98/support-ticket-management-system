import { useEffect, useState, type FormEvent } from 'react'
import { ApiError, userMessageForError } from '../api/client'
import {
  addComment,
  getTicket,
  listComments,
  updateTicket,
  updateTicketStatus,
} from '../api/tickets'
import type { Comment, Priority, Ticket, TicketStatus } from '../api/types'
import { Alert } from '../components/Alert'
import { LoadingState } from '../components/LoadingState'
import { PriorityBadge } from '../components/PriorityBadge'
import { StatusBadge } from '../components/StatusBadge'
import { formatDateTime, nextStatuses, shortId, statusLabel } from '../lib/tickets'

interface TicketDetailPageProps {
  ticketId: string
  onNavigate: (path: string) => void
}

export function TicketDetailPage({ ticketId, onNavigate }: TicketDetailPageProps) {
  const [ticket, setTicket] = useState<Ticket | null>(null)
  const [comments, setComments] = useState<Comment[]>([])
  const [loading, setLoading] = useState(true)
  const [notFound, setNotFound] = useState(false)
  const [pageError, setPageError] = useState<string | null>(null)

  const [editing, setEditing] = useState(false)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [priority, setPriority] = useState<Priority>('MEDIUM')
  const [assignee, setAssignee] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [saveError, setSaveError] = useState<string | null>(null)
  const [saveSuccess, setSaveSuccess] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const [statusError, setStatusError] = useState<string | null>(null)
  const [statusSuccess, setStatusSuccess] = useState<string | null>(null)
  const [statusBusy, setStatusBusy] = useState(false)

  const [commentBody, setCommentBody] = useState('')
  const [commentError, setCommentError] = useState<string | null>(null)
  const [commentSuccess, setCommentSuccess] = useState<string | null>(null)
  const [commentBusy, setCommentBusy] = useState(false)

  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      setNotFound(false)
      setPageError(null)
      try {
        const [ticketData, commentData] = await Promise.all([
          getTicket(ticketId),
          listComments(ticketId),
        ])
        if (cancelled) {
          return
        }
        setTicket(ticketData)
        setComments(commentData)
        syncEditForm(ticketData)
      } catch (err) {
        if (cancelled) {
          return
        }
        if (err instanceof ApiError && err.status === 404) {
          setNotFound(true)
          setTicket(null)
        } else {
          setPageError(userMessageForError(err))
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
  }, [ticketId])

  function syncEditForm(data: Ticket) {
    setTitle(data.title)
    setDescription(data.description)
    setPriority(data.priority)
    setAssignee(data.assignee ?? '')
  }

  async function onSaveFields(event: FormEvent) {
    event.preventDefault()
    if (!ticket) {
      return
    }
    const localErrors: Record<string, string> = {}
    if (!title.trim()) {
      localErrors.title = 'must not be blank'
    }
    if (!description.trim()) {
      localErrors.description = 'must not be blank'
    }
    if (Object.keys(localErrors).length > 0) {
      setFieldErrors(localErrors)
      setSaveError('Please check the highlighted fields.')
      setSaveSuccess(null)
      return
    }

    setSaving(true)
    setSaveError(null)
    setSaveSuccess(null)
    setFieldErrors({})
    try {
      const updated = await updateTicket(ticket.id, {
        title: title.trim(),
        description: description.trim(),
        priority,
        assignee: assignee.trim() ? assignee.trim() : null,
      })
      setTicket(updated)
      syncEditForm(updated)
      setEditing(false)
      setSaveSuccess('Ticket details saved.')
    } catch (err) {
      if (err instanceof ApiError) {
        setFieldErrors(err.fieldErrors)
      }
      setSaveError(userMessageForError(err))
    } finally {
      setSaving(false)
    }
  }

  async function onChangeStatus(target: TicketStatus) {
    if (!ticket) {
      return
    }
    setStatusBusy(true)
    setStatusError(null)
    setStatusSuccess(null)
    try {
      const updated = await updateTicketStatus(ticket.id, target)
      setTicket(updated)
      setStatusSuccess(`Status updated to ${statusLabel(updated.status)}.`)
    } catch (err) {
      setStatusError(userMessageForError(err))
    } finally {
      setStatusBusy(false)
    }
  }

  async function onAddComment(event: FormEvent) {
    event.preventDefault()
    if (!ticket) {
      return
    }
    if (!commentBody.trim()) {
      setCommentError('Please check the highlighted fields.')
      setCommentSuccess(null)
      return
    }
    setCommentBusy(true)
    setCommentError(null)
    setCommentSuccess(null)
    try {
      const created = await addComment(ticket.id, commentBody.trim())
      setComments((prev) => [...prev, created])
      setCommentBody('')
      setCommentSuccess('Comment added.')
    } catch (err) {
      setCommentError(userMessageForError(err))
    } finally {
      setCommentBusy(false)
    }
  }

  if (loading) {
    return <LoadingState label="Loading ticket details…" />
  }

  if (notFound) {
    return (
      <div className="page-stack">
        <Alert tone="error" title="Ticket not found.">
          The requested ticket does not exist or may have been removed.
        </Alert>
        <button type="button" className="btn btn-secondary" onClick={() => onNavigate('/tickets')}>
          Back to tickets
        </button>
      </div>
    )
  }

  if (!ticket) {
    return (
      <div className="page-stack">
        {pageError ? <Alert tone="error">{pageError}</Alert> : null}
        <button type="button" className="btn btn-secondary" onClick={() => onNavigate('/tickets')}>
          Back to tickets
        </button>
      </div>
    )
  }

  const availableNext = nextStatuses(ticket.status)

  return (
    <div className="page-stack">
      <div className="action-row wrap">
        <button type="button" className="btn btn-ghost" onClick={() => onNavigate('/tickets')}>
          ← Back to tickets
        </button>
        {!editing ? (
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => {
              setEditing(true)
              setSaveError(null)
              setSaveSuccess(null)
              syncEditForm(ticket)
            }}
          >
            Edit ticket
          </button>
        ) : null}
      </div>

      <section className="panel">
        <div className="detail-header">
          <div>
            <p className="mono muted">ID {ticket.id}</p>
            <h2>{ticket.title}</h2>
          </div>
          <div className="badge-row">
            <StatusBadge status={ticket.status} />
            <PriorityBadge priority={ticket.priority} />
          </div>
        </div>

        <dl className="meta-grid">
          <div>
            <dt>Assignee</dt>
            <dd>{ticket.assignee ?? 'Unassigned'}</dd>
          </div>
          <div>
            <dt>Created</dt>
            <dd>{formatDateTime(ticket.createdAt)}</dd>
          </div>
          <div>
            <dt>Updated</dt>
            <dd>{formatDateTime(ticket.updatedAt)}</dd>
          </div>
          <div>
            <dt>Short ID</dt>
            <dd>
              <code>{shortId(ticket.id)}</code>
            </dd>
          </div>
        </dl>

        {!editing ? (
          <div className="description-block">
            <h3>Description</h3>
            <p className="preserve-lines">{ticket.description}</p>
          </div>
        ) : (
          <form className="form-grid" onSubmit={onSaveFields} noValidate>
            {saveError ? <Alert tone="error">{saveError}</Alert> : null}
            <label className="field">
              <span>Title</span>
              <input
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                disabled={saving}
                aria-invalid={Boolean(fieldErrors.title)}
              />
              {fieldErrors.title ? <span className="field-error">{fieldErrors.title}</span> : null}
            </label>
            <label className="field">
              <span>Description</span>
              <textarea
                rows={6}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                disabled={saving}
                aria-invalid={Boolean(fieldErrors.description)}
              />
              {fieldErrors.description ? (
                <span className="field-error">{fieldErrors.description}</span>
              ) : null}
            </label>
            <div className="form-row">
              <label className="field">
                <span>Priority</span>
                <select
                  value={priority}
                  onChange={(e) => setPriority(e.target.value as Priority)}
                  disabled={saving}
                >
                  <option value="LOW">Low</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="HIGH">High</option>
                </select>
              </label>
              <label className="field">
                <span>Assignee</span>
                <input
                  value={assignee}
                  onChange={(e) => setAssignee(e.target.value)}
                  disabled={saving}
                  placeholder="Leave blank to clear"
                />
              </label>
            </div>
            <p className="muted small">
              Status is not editable here. Use Change status below. Field updates never change
              status.
            </p>
            <div className="action-row">
              <button
                type="button"
                className="btn btn-ghost"
                disabled={saving}
                onClick={() => {
                  setEditing(false)
                  setSaveError(null)
                  syncEditForm(ticket)
                }}
              >
                Cancel
              </button>
              <button type="submit" className="btn btn-primary" disabled={saving}>
                {saving ? 'Saving…' : 'Save changes'}
              </button>
            </div>
          </form>
        )}

        {saveSuccess && !editing ? <Alert tone="success">{saveSuccess}</Alert> : null}
      </section>

      <section className="panel">
        <div className="panel-header">
          <div>
            <h2>Change status</h2>
            <p className="muted">
              Only allowed next statuses are shown. The backend still rejects illegal transitions.
            </p>
          </div>
        </div>
        {statusError ? <Alert tone="error">{statusError}</Alert> : null}
        {statusSuccess ? <Alert tone="success">{statusSuccess}</Alert> : null}
        {availableNext.length === 0 ? (
          <p className="muted">No further status transitions are available for this ticket.</p>
        ) : (
          <div className="action-row wrap">
            {availableNext.map((target) => (
              <button
                key={target}
                type="button"
                className="btn btn-secondary"
                disabled={statusBusy}
                onClick={() => void onChangeStatus(target)}
              >
                {statusBusy ? 'Updating…' : `Set ${statusLabel(target)}`}
              </button>
            ))}
          </div>
        )}
      </section>

      <section className="panel">
        <div className="panel-header">
          <div>
            <h2>Comments</h2>
            <p className="muted">Comments can be added for any status, including Closed and Cancelled.</p>
          </div>
        </div>

        {commentError ? <Alert tone="error">{commentError}</Alert> : null}
        {commentSuccess ? <Alert tone="success">{commentSuccess}</Alert> : null}

        {comments.length === 0 ? (
          <p className="muted">No comments yet.</p>
        ) : (
          <ul className="comment-list">
            {comments.map((comment) => (
              <li key={comment.id} className="comment-item">
                <p className="preserve-lines">{comment.body}</p>
                <p className="muted small">{formatDateTime(comment.createdAt)}</p>
              </li>
            ))}
          </ul>
        )}

        <form className="form-grid comment-form" onSubmit={onAddComment} noValidate>
          <label className="field">
            <span>Add comment</span>
            <textarea
              rows={3}
              value={commentBody}
              onChange={(e) => setCommentBody(e.target.value)}
              disabled={commentBusy}
              aria-invalid={Boolean(commentError && !commentBody.trim())}
            />
          </label>
          <div className="action-row">
            <button type="submit" className="btn btn-primary" disabled={commentBusy}>
              {commentBusy ? 'Adding…' : 'Add comment'}
            </button>
          </div>
        </form>
      </section>
    </div>
  )
}
