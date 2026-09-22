package com.support.ticket.application;

import com.support.ticket.api.dto.CommentResponse;
import com.support.ticket.api.dto.CreateCommentRequest;
import com.support.ticket.api.dto.CreateTicketRequest;
import com.support.ticket.api.dto.TicketResponse;
import com.support.ticket.api.dto.UpdateTicketRequest;
import com.support.ticket.api.dto.UpdateTicketStatusRequest;
import com.support.ticket.domain.Ticket;
import com.support.ticket.domain.TicketNotFoundException;
import com.support.ticket.domain.TicketStatus;
import com.support.ticket.persistence.CommentEntity;
import com.support.ticket.persistence.CommentRepository;
import com.support.ticket.persistence.TicketEntity;
import com.support.ticket.persistence.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final Clock clock;

    public TicketService(
            TicketRepository ticketRepository,
            CommentRepository commentRepository,
            Clock clock
    ) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.clock = clock;
    }

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        Ticket ticket = Ticket.create(
                request.title(),
                request.description(),
                request.priority(),
                request.assignee(),
                clock
        );
        TicketEntity saved = ticketRepository.save(TicketMapper.toEntity(ticket));
        return TicketMapper.toTicketResponse(saved);
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicket(UUID ticketId) {
        return TicketMapper.toTicketResponse(requireTicket(ticketId));
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listTickets(String keyword, TicketStatus status) {
        String normalizedKeyword = normalizeKeyword(keyword);
        List<TicketEntity> tickets;
        if (normalizedKeyword == null && status == null) {
            tickets = ticketRepository.findAllByOrderByCreatedAtDesc();
        } else if (normalizedKeyword == null) {
            tickets = ticketRepository.findByStatusOrderByCreatedAtDesc(status);
        } else if (status == null) {
            tickets = ticketRepository.searchByKeyword(normalizedKeyword);
        } else {
            tickets = ticketRepository.searchByKeywordAndStatus(normalizedKeyword, status);
        }
        return tickets.stream()
                .map(TicketMapper::toTicketResponse)
                .toList();
    }

    @Transactional
    public TicketResponse updateTicketFields(UUID ticketId, UpdateTicketRequest request) {
        if (!request.hasAnyUpdatableField()) {
            throw new IllegalArgumentException("At least one of title, description, priority, or assignee must be provided");
        }
        TicketEntity entity = requireTicket(ticketId);
        Ticket ticket = TicketMapper.toDomain(entity);
        ticket.updateFields(
                request.getTitle(),
                request.isTitlePresent(),
                request.getDescription(),
                request.isDescriptionPresent(),
                request.getPriority(),
                request.isPriorityPresent(),
                request.getAssignee(),
                request.isAssigneePresent(),
                clock
        );
        TicketMapper.applyDomain(ticket, entity);
        return TicketMapper.toTicketResponse(entity);
    }

    @Transactional
    public TicketResponse transitionStatus(UUID ticketId, UpdateTicketStatusRequest request) {
        TicketEntity entity = requireTicket(ticketId);
        Ticket ticket = TicketMapper.toDomain(entity);
        ticket.transitionTo(request.status(), clock);
        TicketMapper.applyDomain(ticket, entity);
        return TicketMapper.toTicketResponse(entity);
    }

    @Transactional
    public CommentResponse addComment(UUID ticketId, CreateCommentRequest request) {
        TicketEntity ticket = requireTicket(ticketId);
        String body = request.body().trim();
        CommentEntity comment = new CommentEntity(UUID.randomUUID(), ticket, body, clock.instant());
        CommentEntity saved = commentRepository.save(comment);
        return TicketMapper.toCommentResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(UUID ticketId) {
        requireTicket(ticketId);
        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(TicketMapper::toCommentResponse)
                .toList();
    }

    private TicketEntity requireTicket(UUID ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
    }

    private static String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }
}
