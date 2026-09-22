package com.support.ticket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class SchemaMigrationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatesTicketAndCommentTables() {
        Integer ticketTables = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE lower(table_name) = 'ticket'",
                Integer.class
        );
        Integer commentTables = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE lower(table_name) = 'comment'",
                Integer.class
        );

        assertEquals(1, ticketTables);
        assertEquals(1, commentTables);

        Integer ticketFk = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE lower(table_name) = 'comment'
                  AND constraint_type = 'FOREIGN KEY'
                """,
                Integer.class
        );
        assertTrue(ticketFk != null && ticketFk >= 1);
    }
}
