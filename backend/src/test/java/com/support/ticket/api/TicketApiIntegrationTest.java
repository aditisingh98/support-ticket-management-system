package com.support.ticket.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.support.ticket.domain.TicketStatus;
import com.support.ticket.persistence.CommentRepository;
import com.support.ticket.persistence.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TicketApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CommentRepository commentRepository;

    @BeforeEach
    void cleanDatabase() {
        commentRepository.deleteAll();
        ticketRepository.deleteAll();
    }

    @Test
    void createTicketStartsAsOpenAndPersists() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Cannot reset password",
                                  "description": "Reset link returns 500",
                                  "priority": "HIGH",
                                  "assignee": "alex"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/tickets/")))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.title").value("Cannot reset password"))
                .andExpect(jsonPath("$.assignee").value("alex"))
                .andReturn();

        UUID id = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(get("/api/v1/tickets/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    void createTicketRejectsClientStatus() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Title",
                                  "description": "Description",
                                  "priority": "LOW",
                                  "status": "CLOSED"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTicketRejectsBlankDescription() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Title",
                                  "description": "   ",
                                  "priority": "MEDIUM"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("description"));
    }

    @Test
    void createTicketRejectsInvalidPriority() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Title",
                                  "description": "Description",
                                  "priority": "URGENT"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createTicketRejectsBlankTitle() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "   ",
                                  "description": "Description",
                                  "priority": "MEDIUM"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("title"));
    }

    @Test
    void getUnknownTicketReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail", containsString("Ticket not found")));
    }

    @Test
    void listSearchMatchesDescriptionCaseInsensitive() throws Exception {
        createTicket("Network issue", "VPN DROP on login", "HIGH");
        createTicket("Other", "Unrelated text", "LOW");

        mockMvc.perform(get("/api/v1/tickets").param("keyword", "vpn drop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Network issue"));
    }

    @Test
    void listSearchAndFilterCombineWithAnd() throws Exception {
        createTicket("Password reset fails", "Link returns 500", "HIGH");
        createTicket("Login timeout", "Session ends early", "MEDIUM");

        mockMvc.perform(get("/api/v1/tickets").param("keyword", "password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Password reset fails"));

        mockMvc.perform(get("/api/v1/tickets").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/v1/tickets")
                        .param("keyword", "password")
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/v1/tickets")
                        .param("keyword", "password")
                        .param("status", "CLOSED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void invalidStatusFilterReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/tickets").param("status", "NOPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("status must be one of")));
    }

    @Test
    void updateTitleDescriptionPriorityAndAssigneeIndependently() throws Exception {
        UUID id = createTicket("Title", "Description", "LOW");

        mockMvc.perform(patch("/api/v1/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New title\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New title"))
                .andExpect(jsonPath("$.status").value("OPEN"));

        mockMvc.perform(patch("/api/v1/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"New description\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("New description"));

        mockMvc.perform(patch("/api/v1/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":\"HIGH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("HIGH"));

        mockMvc.perform(patch("/api/v1/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignee\":\"sam\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee").value("sam"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void fieldUpdateDoesNotChangeStatusAndAllowsTerminalTickets() throws Exception {
        UUID id = createTicket("Title", "Description", "LOW");
        transition(id, "CANCELLED");

        mockMvc.perform(patch("/api/v1/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated title",
                                  "assignee": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.assignee").isEmpty())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void fieldUpdateRejectsStatusField() throws Exception {
        UUID id = createTicket("Title", "Description", "LOW");

        mockMvc.perform(patch("/api/v1/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "IN_PROGRESS"
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/tickets/" + id))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "OPEN, IN_PROGRESS",
            "IN_PROGRESS, RESOLVED",
            "RESOLVED, CLOSED",
            "OPEN, CANCELLED",
            "IN_PROGRESS, CANCELLED"
    })
    void validTransitionsSucceed(TicketStatus from, TicketStatus to) throws Exception {
        UUID id = ticketInStatus(from);

        mockMvc.perform(patch("/api/v1/tickets/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + to.name() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(to.name()));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "OPEN, OPEN",
            "OPEN, RESOLVED",
            "OPEN, CLOSED",
            "IN_PROGRESS, OPEN",
            "IN_PROGRESS, IN_PROGRESS",
            "IN_PROGRESS, CLOSED",
            "RESOLVED, OPEN",
            "RESOLVED, IN_PROGRESS",
            "RESOLVED, RESOLVED",
            "RESOLVED, CANCELLED",
            "CLOSED, OPEN",
            "CLOSED, IN_PROGRESS",
            "CLOSED, RESOLVED",
            "CLOSED, CLOSED",
            "CLOSED, CANCELLED",
            "CANCELLED, OPEN",
            "CANCELLED, IN_PROGRESS",
            "CANCELLED, RESOLVED",
            "CANCELLED, CLOSED",
            "CANCELLED, CANCELLED"
    })
    void invalidTransitionsReturn409AndLeaveStatusUnchanged(TicketStatus from, TicketStatus to) throws Exception {
        UUID id = ticketInStatus(from);

        mockMvc.perform(patch("/api/v1/tickets/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + to.name() + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.currentStatus").value(from.name()))
                .andExpect(jsonPath("$.attemptedStatus").value(to.name()))
                .andExpect(jsonPath("$.detail", containsString("not allowed")));

        mockMvc.perform(get("/api/v1/tickets/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(from.name()));
    }

    @Test
    void commentsCanBeAddedOnCancelledTickets() throws Exception {
        UUID id = createTicket("Title", "Description", "MEDIUM");
        transition(id, "CANCELLED");

        mockMvc.perform(post("/api/v1/tickets/" + id + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Note on cancelled ticket.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("Note on cancelled ticket."));
    }

    @Test
    void commentsCanBeAddedAndListedIncludingOnClosedTickets() throws Exception {
        UUID id = createTicket("Title", "Description", "MEDIUM");
        transition(id, "IN_PROGRESS");
        transition(id, "RESOLVED");
        transition(id, "CLOSED");

        mockMvc.perform(post("/api/v1/tickets/" + id + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Customer confirmed.\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/tickets/" + id + "/comments"))
                .andExpect(jsonPath("$.body").value("Customer confirmed."))
                .andExpect(jsonPath("$.ticketId").value(id.toString()));

        mockMvc.perform(get("/api/v1/tickets/" + id + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].body").value("Customer confirmed."));
    }

    @Test
    void blankCommentBodyReturns400() throws Exception {
        UUID id = createTicket("Title", "Description", "LOW");

        mockMvc.perform(post("/api/v1/tickets/" + id + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    private UUID createTicket(String title, String description, String priority) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "%s",
                                  "priority": "%s"
                                }
                                """.formatted(title, description, priority)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private void transition(UUID id, String status) throws Exception {
        mockMvc.perform(patch("/api/v1/tickets/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().isOk());
    }

    private UUID ticketInStatus(TicketStatus status) throws Exception {
        UUID id = createTicket("Title", "Description", "MEDIUM");
        switch (status) {
            case OPEN -> {
            }
            case IN_PROGRESS -> transition(id, "IN_PROGRESS");
            case RESOLVED -> {
                transition(id, "IN_PROGRESS");
                transition(id, "RESOLVED");
            }
            case CLOSED -> {
                transition(id, "IN_PROGRESS");
                transition(id, "RESOLVED");
                transition(id, "CLOSED");
            }
            case CANCELLED -> transition(id, "CANCELLED");
        }
        assertEquals(status.name(),
                objectMapper.readTree(
                        mockMvc.perform(get("/api/v1/tickets/" + id)).andReturn().getResponse().getContentAsString()
                ).get("status").asText());
        return id;
    }
}
