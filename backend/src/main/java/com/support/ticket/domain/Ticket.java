package com.support.ticket.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain ticket behaviour. Creation always starts as OPEN; status changes go through the transition policy.
 */
public final class Ticket {

    private final UUID id;
    private String title;
    private String description;
    private Priority priority;
    private String assignee;
    private TicketStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private Ticket(
            UUID id,
            String title,
            String description,
            Priority priority,
            String assignee,
            TicketStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.assignee = assignee;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Ticket create(String title, String description, Priority priority, String assignee, Clock clock) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(clock, "clock");

        String normalizedTitle = requireNonBlank(title, "title");
        String normalizedDescription = requireNonBlank(description, "description");
        String normalizedAssignee = normalizeAssignee(assignee);

        Instant now = clock.instant();
        return new Ticket(
                UUID.randomUUID(),
                normalizedTitle,
                normalizedDescription,
                priority,
                normalizedAssignee,
                TicketStatus.OPEN,
                now,
                now
        );
    }

    public void transitionTo(TicketStatus targetStatus, Clock clock) {
        Objects.requireNonNull(targetStatus, "targetStatus");
        Objects.requireNonNull(clock, "clock");
        TicketStatusTransitionPolicy.assertCanTransition(this.status, targetStatus);
        this.status = targetStatus;
        this.updatedAt = clock.instant();
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Priority getPriority() {
        return priority;
    }

    public String getAssignee() {
        return assignee;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeAssignee(String assignee) {
        if (assignee == null || assignee.isBlank()) {
            return null;
        }
        return assignee.trim();
    }
}
