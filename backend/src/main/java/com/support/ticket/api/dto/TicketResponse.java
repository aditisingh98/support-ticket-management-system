package com.support.ticket.api.dto;

import com.support.ticket.domain.Priority;
import com.support.ticket.domain.TicketStatus;

import java.time.Instant;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        String title,
        String description,
        Priority priority,
        String assignee,
        TicketStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
