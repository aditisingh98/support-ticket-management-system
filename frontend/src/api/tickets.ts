import { apiRequest } from './client'
import type {
  Comment,
  CreateTicketPayload,
  Ticket,
  TicketStatus,
  UpdateTicketPayload,
} from './types'

const BASE = '/api/v1/tickets'

export function listTickets(params?: {
  keyword?: string
  status?: TicketStatus | ''
}): Promise<Ticket[]> {
  const query = new URLSearchParams()
  if (params?.keyword?.trim()) {
    query.set('keyword', params.keyword.trim())
  }
  if (params?.status) {
    query.set('status', params.status)
  }
  const suffix = query.toString() ? `?${query.toString()}` : ''
  return apiRequest<Ticket[]>(`${BASE}${suffix}`)
}

export function getTicket(ticketId: string): Promise<Ticket> {
  return apiRequest<Ticket>(`${BASE}/${ticketId}`)
}

export function createTicket(payload: CreateTicketPayload): Promise<Ticket> {
  return apiRequest<Ticket>(BASE, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateTicket(
  ticketId: string,
  payload: UpdateTicketPayload,
): Promise<Ticket> {
  return apiRequest<Ticket>(`${BASE}/${ticketId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function updateTicketStatus(
  ticketId: string,
  status: TicketStatus,
): Promise<Ticket> {
  return apiRequest<Ticket>(`${BASE}/${ticketId}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  })
}

export function listComments(ticketId: string): Promise<Comment[]> {
  return apiRequest<Comment[]>(`${BASE}/${ticketId}/comments`)
}

export function addComment(ticketId: string, body: string): Promise<Comment> {
  return apiRequest<Comment>(`${BASE}/${ticketId}/comments`, {
    method: 'POST',
    body: JSON.stringify({ body }),
  })
}
