package com.support.ticket.domain;

public class IllegalTicketTransitionException extends RuntimeException {

    private final TicketStatus currentStatus;
    private final TicketStatus attemptedStatus;

    public IllegalTicketTransitionException(TicketStatus currentStatus, TicketStatus attemptedStatus) {
        super("Transition from %s to %s is not allowed.".formatted(currentStatus, attemptedStatus));
        this.currentStatus = currentStatus;
        this.attemptedStatus = attemptedStatus;
    }

    public TicketStatus getCurrentStatus() {
        return currentStatus;
    }

    public TicketStatus getAttemptedStatus() {
        return attemptedStatus;
    }
}
