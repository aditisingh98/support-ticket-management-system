package com.support.ticket.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketStatusTransitionPolicyTest {

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @CsvSource({
            "OPEN, IN_PROGRESS",
            "IN_PROGRESS, RESOLVED",
            "RESOLVED, CLOSED",
            "OPEN, CANCELLED",
            "IN_PROGRESS, CANCELLED"
    })
    void shouldAllowValidTransitions(TicketStatus from, TicketStatus to) {
        assertTrue(TicketStatusTransitionPolicy.canTransition(from, to));
        assertDoesNotThrow(() -> TicketStatusTransitionPolicy.assertCanTransition(from, to));
    }

    @ParameterizedTest(name = "{0} -> {1} is rejected")
    @CsvSource({
            "OPEN, OPEN",
            "OPEN, RESOLVED",
            "OPEN, CLOSED",
            "IN_PROGRESS, OPEN",
            "IN_PROGRESS, IN_PROGRESS",
            "IN_PROGRESS, CLOSED",
            "RESOLVED, OPEN",
            "RESOLVED, IN_PROGRESS",
            "RESOLVED, RESOLVED",
            "RESOLVED, CANCELLED",
            "CLOSED, OPEN",
            "CLOSED, IN_PROGRESS",
            "CLOSED, RESOLVED",
            "CLOSED, CLOSED",
            "CLOSED, CANCELLED",
            "CANCELLED, OPEN",
            "CANCELLED, IN_PROGRESS",
            "CANCELLED, RESOLVED",
            "CANCELLED, CLOSED",
            "CANCELLED, CANCELLED"
    })
    void shouldRejectInvalidTransitions(TicketStatus from, TicketStatus to) {
        IllegalTicketTransitionException exception = assertThrows(
                IllegalTicketTransitionException.class,
                () -> TicketStatusTransitionPolicy.assertCanTransition(from, to)
        );

        assertEquals(from, exception.getCurrentStatus());
        assertEquals(to, exception.getAttemptedStatus());
        assertTrue(exception.getMessage().contains(from.name()));
        assertTrue(exception.getMessage().contains(to.name()));
        assertTrue(exception.getMessage().toLowerCase().contains("not allowed"));
    }

    @ParameterizedTest(name = "same-status {0} -> {0} is rejected")
    @EnumSource(TicketStatus.class)
    void shouldRejectSameStatusTransitions(TicketStatus status) {
        assertThrows(
                IllegalTicketTransitionException.class,
                () -> TicketStatusTransitionPolicy.assertCanTransition(status, status)
        );
    }

    @Test
    void closedMatrixHasExactlyFiveAllowedEdges() {
        int allowedCount = 0;
        for (TicketStatus from : TicketStatus.values()) {
            for (TicketStatus to : TicketStatus.values()) {
                if (TicketStatusTransitionPolicy.canTransition(from, to)) {
                    allowedCount++;
                }
            }
        }
        assertEquals(5, allowedCount);
    }
}
