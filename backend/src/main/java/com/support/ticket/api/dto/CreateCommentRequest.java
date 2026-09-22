package com.support.ticket.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = false)
public record CreateCommentRequest(
        @NotBlank(message = "must not be blank")
        String body
) {
}
