import type { TicketStatus } from '../api/types'
import { statusLabel } from '../lib/tickets'

interface StatusBadgeProps {
  status: TicketStatus
}

export function StatusBadge({ status }: StatusBadgeProps) {
  return (
    <span className={`badge badge-status badge-${status.toLowerCase()}`}>
      {statusLabel(status)}
    </span>
  )
}
