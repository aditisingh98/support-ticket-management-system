package com.support.ticket.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.support.ticket.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = false)
public record CreateTicketRequest(
        @NotBlank(message = "must not be blank")
        String title,

        @NotBlank(message = "must not be blank")
        String description,

        @NotNull(message = "must be one of LOW, MEDIUM, HIGH")
        Priority priority,

        String assignee
) {
}
