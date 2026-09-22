package com.support.ticket.api.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID ticketId,
        String body,
        Instant createdAt
) {
}
