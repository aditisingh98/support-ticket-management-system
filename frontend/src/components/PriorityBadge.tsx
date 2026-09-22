import type { Priority } from '../api/types'
import { priorityLabel } from '../lib/tickets'

interface PriorityBadgeProps {
  priority: Priority
}

export function PriorityBadge({ priority }: PriorityBadgeProps) {
  return (
    <span className={`badge badge-priority badge-${priority.toLowerCase()}`}>
      {priorityLabel(priority)}
    </span>
  )
}
