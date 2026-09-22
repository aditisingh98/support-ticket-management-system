package com.support.ticket.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.support.ticket.domain.TicketStatus;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = false)
public record UpdateTicketStatusRequest(
        @NotNull(message = "must be a valid ticket status")
        TicketStatus status
) {
}
