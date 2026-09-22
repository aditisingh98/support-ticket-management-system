package com.support.ticket.api;

import com.support.ticket.api.dto.CommentResponse;
import com.support.ticket.api.dto.CreateCommentRequest;
import com.support.ticket.api.dto.CreateTicketRequest;
import com.support.ticket.api.dto.TicketResponse;
import com.support.ticket.api.dto.UpdateTicketRequest;
import com.support.ticket.api.dto.UpdateTicketStatusRequest;
import com.support.ticket.application.TicketService;
import com.support.ticket.domain.TicketStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        TicketResponse created = ticketService.createTicket(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{ticketId}")
    public TicketResponse getTicket(@PathVariable UUID ticketId) {
        return ticketService.getTicket(ticketId);
    }

    @GetMapping
    public List<TicketResponse> listTickets(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        TicketStatus parsedStatus = parseStatus(status);
        return ticketService.listTickets(keyword, parsedStatus);
    }

    @PatchMapping("/{ticketId}")
    public TicketResponse updateTicket(
            @PathVariable UUID ticketId,
            @Valid @RequestBody UpdateTicketRequest request
    ) {
        return ticketService.updateTicketFields(ticketId, request);
    }

    @PatchMapping("/{ticketId}/status")
    public TicketResponse updateStatus(
            @PathVariable UUID ticketId,
            @Valid @RequestBody UpdateTicketStatusRequest request
    ) {
        return ticketService.transitionStatus(ticketId, request);
    }

    @PostMapping("/{ticketId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable UUID ticketId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        CommentResponse created = ticketService.addComment(ticketId, request);
        URI location = URI.create("/api/v1/tickets/" + ticketId + "/comments");
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, location.toString())
                .body(created);
    }

    @GetMapping("/{ticketId}/comments")
    public List<CommentResponse> listComments(@PathVariable UUID ticketId) {
        return ticketService.listComments(ticketId);
    }

    private static TicketStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return TicketStatus.valueOf(status.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "status must be one of OPEN, IN_PROGRESS, RESOLVED, CLOSED, CANCELLED"
            );
        }
    }
}
