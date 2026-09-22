export type TicketStatus =
  | 'OPEN'
  | 'IN_PROGRESS'
  | 'RESOLVED'
  | 'CLOSED'
  | 'CANCELLED'

export type Priority = 'LOW' | 'MEDIUM' | 'HIGH'

export interface Ticket {
  id: string
  title: string
  description: string
  priority: Priority
  assignee: string | null
  status: TicketStatus
  createdAt: string
  updatedAt: string
}

export interface Comment {
  id: string
  ticketId: string
  body: string
  createdAt: string
}

export interface FieldError {
  field: string
  message: string
}

export interface ProblemDetails {
  type?: string
  title?: string
  status: number
  detail?: string
  instance?: string
  errors?: FieldError[]
  currentStatus?: string
  attemptedStatus?: string
}

export interface CreateTicketPayload {
  title: string
  description: string
  priority: Priority
  assignee?: string | null
}

export interface UpdateTicketPayload {
  title?: string
  description?: string
  priority?: Priority
  assignee?: string | null
}
