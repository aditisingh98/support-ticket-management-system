package com.support.ticket.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Domain-owned closed transition matrix for ticket status.
 * Controllers, repositories, and the frontend do not own these rules.
 */
public final class TicketStatusTransitionPolicy {

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED = buildAllowedTransitions();

    private TicketStatusTransitionPolicy() {
    }

    public static boolean canTransition(TicketStatus from, TicketStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public static void assertCanTransition(TicketStatus from, TicketStatus to) {
        if (!canTransition(from, to)) {
            throw new IllegalTicketTransitionException(from, to);
        }
    }

    private static Map<TicketStatus, Set<TicketStatus>> buildAllowedTransitions() {
        Map<TicketStatus, Set<TicketStatus>> allowed = new EnumMap<>(TicketStatus.class);
        allowed.put(TicketStatus.OPEN, EnumSet.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED));
        allowed.put(TicketStatus.IN_PROGRESS, EnumSet.of(TicketStatus.RESOLVED, TicketStatus.CANCELLED));
        allowed.put(TicketStatus.RESOLVED, EnumSet.of(TicketStatus.CLOSED));
        allowed.put(TicketStatus.CLOSED, EnumSet.noneOf(TicketStatus.class));
        allowed.put(TicketStatus.CANCELLED, EnumSet.noneOf(TicketStatus.class));
        return Map.copyOf(allowed);
    }
}
