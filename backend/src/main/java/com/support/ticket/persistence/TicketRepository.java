package com.support.ticket.persistence;

import com.support.ticket.domain.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<TicketEntity, UUID> {

    List<TicketEntity> findAllByOrderByCreatedAtDesc();

    List<TicketEntity> findByStatusOrderByCreatedAtDesc(TicketStatus status);

    @Query("""
            SELECT t FROM TicketEntity t
            WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY t.createdAt DESC
            """)
    List<TicketEntity> searchByKeyword(@Param("keyword") String keyword);

    @Query("""
            SELECT t FROM TicketEntity t
            WHERE t.status = :status
              AND (
                   LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY t.createdAt DESC
            """)
    List<TicketEntity> searchByKeywordAndStatus(
            @Param("keyword") String keyword,
            @Param("status") TicketStatus status
    );
}
