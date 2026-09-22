# Manual Test Cases — Support Ticket Management System

Step-by-step demo flow for the single-port portal. Do not invent extra business scenarios beyond this list.

**Portal:** http://localhost:9000  
**API base:** http://localhost:9000/api/v1

Prerequisites: PostgreSQL running, `support_tickets` database created, datasource env vars set, frontend built, Spring Boot started (see `README.md`).

---

## Flow

### 1. Start application

1. Ensure PostgreSQL is up and `support_tickets` exists.
2. Set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.
3. Build frontend (`npm run build` in `frontend/`).
4. Package/start backend (`mvn -DskipTests package` then `java -jar …`, or `mvn spring-boot:run`).

### 2. Open portal

Open **http://localhost:9000**  
Expect: Support Ticket portal UI (dashboard), not a raw JSON page.

### 3. Create ticket

1. Go to Create Ticket.
2. Enter title, description, priority; optional assignee.
3. Confirm there is **no** status field.
4. Submit.

Expect: ticket created; status is **OPEN**.

### 4. Verify it appears in list

Open Tickets list. Expect the new ticket with status and priority badges.

### 5. Open ticket

Open the ticket detail page. Expect fields, status, timestamps, comments section.

### 6. Update title

Edit title and save. Expect updated title; status still **OPEN**.

### 7. Update description

Edit description and save. Expect updated description.

### 8. Update priority

Change priority and save. Expect new priority badge.

### 9. Update assignee

Set or clear assignee and save. Expect assignee updated.

### 10. Add comment

Add a non-blank comment. Expect it appears in the comment list.

### 11. Search ticket

On Tickets list, search a keyword from the title (or description). Expect matching ticket(s).

### 12. Filter by OPEN

Filter status = `OPEN`. Expect only OPEN tickets (combinable with keyword).

### 13. Move OPEN → IN_PROGRESS

On detail, choose **IN_PROGRESS**. Expect status updates; no illegal options offered for UX.

### 14. Move IN_PROGRESS → RESOLVED

Transition to **RESOLVED**. Expect success.

### 15. Move RESOLVED → CLOSED

Transition to **CLOSED**. Expect success; no further transition controls.

### 16. Try invalid transition and verify meaningful error

Attempt an illegal transition if possible (e.g. API call `PATCH /api/v1/tickets/{id}/status` with `"status":"OPEN"` on a CLOSED ticket, or any bypass of the UI).

Expect: **409** with a clear Problem Details message (current vs attempted status). Ticket status unchanged.

### 17. Create another ticket

Create a second ticket (starts **OPEN**).

### 18. Test OPEN → CANCELLED

On the second ticket, transition to **CANCELLED**. Expect success.

### 19. Test IN_PROGRESS → CANCELLED

1. Create a third ticket.
2. Move **OPEN → IN_PROGRESS**.
3. Move **IN_PROGRESS → CANCELLED**.

Expect success. Optionally add a comment while CANCELLED (allowed).

### 20. Restart application

Stop Spring Boot, start it again with the same database configuration.

### 21. Verify data still exists

Open **http://localhost:9000** again. Expect previously created tickets and comments still present in PostgreSQL.

---

## Quick API checks (optional)

| Check | Call |
|-------|------|
| List | `GET http://localhost:9000/api/v1/tickets` |
| Invalid filter | `GET …/tickets?status=NOPE` → 400 |
| Not found | `GET …/tickets/{random-uuid}` → 404 |
