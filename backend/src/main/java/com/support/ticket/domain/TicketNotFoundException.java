package com.support.ticket.domain;

public class TicketNotFoundException extends RuntimeException {

    private final java.util.UUID ticketId;

    public TicketNotFoundException(java.util.UUID ticketId) {
        super("Ticket not found: " + ticketId);
        this.ticketId = ticketId;
    }

    public java.util.UUID getTicketId() {
        return ticketId;
    }
}
