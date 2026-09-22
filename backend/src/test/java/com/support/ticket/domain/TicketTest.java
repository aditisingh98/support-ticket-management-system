package com.support.ticket.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TicketTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-22T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void createAlwaysStartsAsOpen() {
        Ticket ticket = Ticket.create("Cannot login", "Password reset fails", Priority.HIGH, "alex", CLOCK);

        assertEquals(TicketStatus.OPEN, ticket.getStatus());
        assertEquals("Cannot login", ticket.getTitle());
        assertEquals("Password reset fails", ticket.getDescription());
        assertEquals(Priority.HIGH, ticket.getPriority());
        assertEquals("alex", ticket.getAssignee());
        assertEquals(CLOCK.instant(), ticket.getCreatedAt());
        assertEquals(CLOCK.instant(), ticket.getUpdatedAt());
    }

    @Test
    void createOmitsAssigneeWhenBlank() {
        Ticket ticket = Ticket.create("Title", "Description", Priority.LOW, "   ", CLOCK);

        assertEquals(TicketStatus.OPEN, ticket.getStatus());
        assertNull(ticket.getAssignee());
    }

    @Test
    void createRejectsBlankTitle() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Ticket.create("  ", "Description", Priority.MEDIUM, null, CLOCK)
        );
    }

    @Test
    void createRejectsBlankDescription() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Ticket.create("Title", "", Priority.MEDIUM, null, CLOCK)
        );
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "OPEN, IN_PROGRESS",
            "IN_PROGRESS, RESOLVED",
            "RESOLVED, CLOSED",
            "OPEN, CANCELLED",
            "IN_PROGRESS, CANCELLED"
    })
    void transitionAppliesAllowedEdges(TicketStatus from, TicketStatus to) {
        Ticket ticket = ticketInStatus(from);
        Clock later = Clock.fixed(Instant.parse("2026-09-22T11:00:00Z"), ZoneOffset.UTC);

        ticket.transitionTo(to, later);

        assertEquals(to, ticket.getStatus());
        assertEquals(later.instant(), ticket.getUpdatedAt());
        assertEquals(CLOCK.instant(), ticket.getCreatedAt());
    }

    @Test
    void transitionRejectsIllegalEdgeWithoutChangingStatus() {
        Ticket ticket = ticketInStatus(TicketStatus.OPEN);

        IllegalTicketTransitionException exception = assertThrows(
                IllegalTicketTransitionException.class,
                () -> ticket.transitionTo(TicketStatus.CLOSED, CLOCK)
        );

        assertEquals(TicketStatus.OPEN, ticket.getStatus());
        assertEquals(TicketStatus.OPEN, exception.getCurrentStatus());
        assertEquals(TicketStatus.CLOSED, exception.getAttemptedStatus());
    }

    private static Ticket ticketInStatus(TicketStatus status) {
        Ticket ticket = Ticket.create("Title", "Description", Priority.MEDIUM, null, CLOCK);
        switch (status) {
            case OPEN -> {
                // already OPEN
            }
            case IN_PROGRESS -> ticket.transitionTo(TicketStatus.IN_PROGRESS, CLOCK);
            case RESOLVED -> {
                ticket.transitionTo(TicketStatus.IN_PROGRESS, CLOCK);
                ticket.transitionTo(TicketStatus.RESOLVED, CLOCK);
            }
            case CLOSED -> {
                ticket.transitionTo(TicketStatus.IN_PROGRESS, CLOCK);
                ticket.transitionTo(TicketStatus.RESOLVED, CLOCK);
                ticket.transitionTo(TicketStatus.CLOSED, CLOCK);
            }
            case CANCELLED -> ticket.transitionTo(TicketStatus.CANCELLED, CLOCK);
        }
        return ticket;
    }
}
