import type { TicketStatus } from '../api/types'

const NEXT_STATUSES: Record<TicketStatus, TicketStatus[]> = {
  OPEN: ['IN_PROGRESS', 'CANCELLED'],
  IN_PROGRESS: ['RESOLVED', 'CANCELLED'],
  RESOLVED: ['CLOSED'],
  CLOSED: [],
  CANCELLED: [],
}

export function nextStatuses(status: TicketStatus): TicketStatus[] {
  return NEXT_STATUSES[status] ?? []
}

export function statusLabel(status: TicketStatus): string {
  switch (status) {
    case 'IN_PROGRESS':
      return 'In Progress'
    case 'OPEN':
      return 'Open'
    case 'RESOLVED':
      return 'Resolved'
    case 'CLOSED':
      return 'Closed'
    case 'CANCELLED':
      return 'Cancelled'
    default:
      return status
  }
}

export function priorityLabel(priority: string): string {
  switch (priority) {
    case 'LOW':
      return 'Low'
    case 'MEDIUM':
      return 'Medium'
    case 'HIGH':
      return 'High'
    default:
      return priority
  }
}

export function formatDateTime(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

export function shortId(id: string): string {
  return id.slice(0, 8)
}
