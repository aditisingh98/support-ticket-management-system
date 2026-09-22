package com.support.ticket.application;

import com.support.ticket.api.dto.CommentResponse;
import com.support.ticket.api.dto.TicketResponse;
import com.support.ticket.domain.Ticket;
import com.support.ticket.persistence.CommentEntity;
import com.support.ticket.persistence.TicketEntity;

final class TicketMapper {

    private TicketMapper() {
    }

    static TicketEntity toEntity(Ticket ticket) {
        return new TicketEntity(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getAssignee(),
                ticket.getStatus(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    static Ticket toDomain(TicketEntity entity) {
        return Ticket.reconstitute(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getPriority(),
                entity.getAssignee(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    static void applyDomain(Ticket ticket, TicketEntity entity) {
        entity.setTitle(ticket.getTitle());
        entity.setDescription(ticket.getDescription());
        entity.setPriority(ticket.getPriority());
        entity.setAssignee(ticket.getAssignee());
        entity.setStatus(ticket.getStatus());
        entity.setUpdatedAt(ticket.getUpdatedAt());
    }

    static TicketResponse toTicketResponse(TicketEntity entity) {
        return new TicketResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getPriority(),
                entity.getAssignee(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    static TicketResponse toTicketResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getAssignee(),
                ticket.getStatus(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    static CommentResponse toCommentResponse(CommentEntity entity) {
        return new CommentResponse(
                entity.getId(),
                entity.getTicket().getId(),
                entity.getBody(),
                entity.getCreatedAt()
        );
    }
}
