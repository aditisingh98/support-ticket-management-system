import { useState, type FormEvent } from 'react'
import { ApiError, userMessageForError } from '../api/client'
import { createTicket } from '../api/tickets'
import type { Priority } from '../api/types'
import { Alert } from '../components/Alert'

interface CreateTicketPageProps {
  onNavigate: (path: string) => void
}

export function CreateTicketPage({ onNavigate }: CreateTicketPageProps) {
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [priority, setPriority] = useState<Priority>('MEDIUM')
  const [assignee, setAssignee] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    const localErrors: Record<string, string> = {}
    if (!title.trim()) {
      localErrors.title = 'must not be blank'
    }
    if (!description.trim()) {
      localErrors.description = 'must not be blank'
    }
    if (Object.keys(localErrors).length > 0) {
      setFieldErrors(localErrors)
      setFormError('Please check the highlighted fields.')
      return
    }

    setSaving(true)
    setFieldErrors({})
    setFormError(null)
    try {
      const trimmedAssignee = assignee.trim()
      const created = await createTicket({
        title: title.trim(),
        description: description.trim(),
        priority,
        ...(trimmedAssignee ? { assignee: trimmedAssignee } : {}),
      })
      onNavigate(`/tickets/${created.id}`)
    } catch (err) {
      if (err instanceof ApiError) {
        setFieldErrors(err.fieldErrors)
        setFormError(userMessageForError(err))
      } else {
        setFormError(userMessageForError(err))
      }
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="page-stack">
      <section className="panel form-panel">
        <div className="panel-header">
          <div>
            <h2>Create Ticket</h2>
            <p className="muted">
              New tickets are created with status <strong>OPEN</strong>. Status is assigned by the
              server and cannot be chosen here.
            </p>
          </div>
        </div>

        {formError ? <Alert tone="error">{formError}</Alert> : null}

        <form className="form-grid" onSubmit={onSubmit} noValidate>
          <label className="field">
            <span>Title</span>
            <input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              aria-invalid={Boolean(fieldErrors.title)}
              disabled={saving}
            />
            {fieldErrors.title ? <span className="field-error">{fieldErrors.title}</span> : null}
          </label>

          <label className="field">
            <span>Description</span>
            <textarea
              rows={6}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              aria-invalid={Boolean(fieldErrors.description)}
              disabled={saving}
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
                aria-invalid={Boolean(fieldErrors.priority)}
                disabled={saving}
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
              </select>
              {fieldErrors.priority ? (
                <span className="field-error">{fieldErrors.priority}</span>
              ) : null}
            </label>

            <label className="field">
              <span>Assignee (optional)</span>
              <input
                value={assignee}
                onChange={(e) => setAssignee(e.target.value)}
                placeholder="e.g. alex"
                aria-invalid={Boolean(fieldErrors.assignee)}
                disabled={saving}
              />
              {fieldErrors.assignee ? (
                <span className="field-error">{fieldErrors.assignee}</span>
              ) : null}
            </label>
          </div>

          <div className="action-row">
            <button
              type="button"
              className="btn btn-ghost"
              onClick={() => onNavigate('/tickets')}
              disabled={saving}
            >
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Creating…' : 'Create ticket'}
            </button>
          </div>
        </form>
      </section>
    </div>
  )
}
