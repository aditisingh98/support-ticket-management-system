# Data Model — Support Ticket Management System

Logical and physical persistence model for tickets and comments. Source of truth: the original assignment plus agreed project decisions recorded here and in `spec/architecture.md`.

This document is **not** an API contract, UI flow, or application code.

---

## 1. Ticket entity

| Field | Logical type | Required | Nullable | Default | Constraints / notes |
|-------|--------------|----------|----------|---------|---------------------|
| `id` | UUID | yes (server) | no | server-generated on create | Primary key |
| `title` | string | yes | no | none | Non-blank after trim for create and field update |
| `description` | string | yes | no | none | Non-blank after trim for create and field update |
| `priority` | `Priority` enum | yes | no | none | Must be `LOW`, `MEDIUM`, or `HIGH` |
| `assignee` | string | no | yes | `null` when omitted | Optional free-text; not a user reference |
| `status` | `TicketStatus` enum | yes (server) | no | `OPEN` on create | Client must not supply on create; only changed via dedicated transition |
| `createdAt` | timestamp | yes (server) | no | set on insert | Immutable after create |
| `updatedAt` | timestamp | yes (server) | no | set on insert; refreshed on field update and status transition | See §8 |

**Not part of this entity:** user/owner ids, tags, category, SLA fields, soft-delete flags, audit columns beyond `createdAt` / `updatedAt`, attachments, or status-history rows.

---

## 2. Comment entity

| Field | Logical type | Required | Nullable | Constraints / notes |
|-------|--------------|----------|----------|---------------------|
| `id` | UUID | yes (server) | no | Primary key |
| `ticketId` | UUID | yes | no | Foreign key to Ticket; comment belongs to exactly one ticket |
| `body` | string | yes | no | Non-blank after trim |
| `createdAt` | timestamp | yes (server) | no | Set on insert; immutable |

**Lifecycle:** comments are create-only. No edit, no delete, no `updatedAt`.

**Not part of this entity:** author/user id, visibility flags, soft delete, attachments.

---

## 3. Relationship: Ticket ↔ Comment

```
Ticket (1) ──────── (0..n) Comment
```

- One ticket has zero or more comments.
- Each comment references exactly one ticket.
- Comments are persisted as **separate rows**, not a JSON/text blob on the ticket.
- Deleting tickets is **out of scope** for the product; no cascade-delete product rule is required. Physical FK behaviour is an implementation choice if delete is never exposed (see §11).

---

## 4. Enum / value definitions

### 4.1 `TicketStatus`

| Value |
|-------|
| `OPEN` |
| `IN_PROGRESS` |
| `RESOLVED` |
| `CLOSED` |
| `CANCELLED` |

**Allowed transitions (only):**

| From | To |
|------|-----|
| `OPEN` | `IN_PROGRESS` |
| `OPEN` | `CANCELLED` |
| `IN_PROGRESS` | `RESOLVED` |
| `IN_PROGRESS` | `CANCELLED` |
| `RESOLVED` | `CLOSED` |

Every other pair is invalid for a status-transition operation, including same-status (`OPEN` → `OPEN`, etc.).

Transition legality is enforced in the **domain / application layer**, not by inventing a transition-history table or database trigger graph.

### 4.2 `Priority`

| Value |
|-------|
| `LOW` |
| `MEDIUM` |
| `HIGH` |

No other priority values are valid.

---

## 5. Database constraints

| Concern | Rule |
|---------|------|
| **Primary keys** | `ticket.id` UUID PK; `comment.id` UUID PK |
| **Foreign keys** | `comment.ticket_id` → `ticket.id` (required) |
| **Nullable** | Only `ticket.assignee` is nullable among product fields |
| **Not null** | All other ticket and comment columns listed above |
| **Enum / value constraints** | Persist only allowed `TicketStatus` and `Priority` values (CHECK constraint and/or typed enum storage — implementation choice) |
| **Uniqueness** | No product unique constraint beyond primary keys (titles may duplicate) |
| **Soft delete** | None |

Non-blank validation for `title`, `description`, and `comment.body` is an **application validation** rule. Whether the database also rejects empty strings with CHECK constraints is an implementation choice; do not treat DB CHECKs for blankness as a separate product feature.

---

## 6. Lifecycle rules relevant to persistence

### 6.1 Ticket creation

- Persist a new ticket with server-generated `id`.
- Set `status = OPEN` in the service; do not accept client-supplied status.
- Require non-blank `title` and `description`, and a valid `priority`.
- `assignee` may be omitted → store `null`.
- Set `createdAt` and `updatedAt` to the creation time.

### 6.2 Status transition persistence

- Load ticket by id; if missing → not found (API concern, not a schema concern).
- Apply domain transition rules; if illegal → reject and **do not** write a new status.
- On success: persist the new `status` and refresh `updatedAt`.
- Field columns (`title`, `description`, `priority`, `assignee`) are unchanged by a pure status transition.

### 6.3 Comment persistence

- Ticket must exist; then insert a comment row with server-generated `id`, `ticket_id`, non-blank `body`, and `createdAt`.
- Allowed for **any** existing ticket status, including `CLOSED` and `CANCELLED`.
- No update or delete of comment rows.

### 6.4 Field-update behaviour

- Updatable fields: `title`, `description`, `priority`, `assignee` only.
- **Must not** change `status` through the field-update path.
- Field updates remain allowed when status is `CLOSED` or `CANCELLED`.
- On successful field update: persist changed fields and refresh `updatedAt`.
- `createdAt` never changes after insert.

---

## 7. Search / filter persistence considerations

| Requirement | Persistence implication |
|-------------|-------------------------|
| Keyword search is case-insensitive | Query title and description with case-insensitive matching (e.g. PostgreSQL `ILIKE`, or equivalent portable approach) |
| Search applies to **title** and **description** | Do **not** search comment bodies unless a later product decision adds that |
| Status filter accepts **one** status | Equality filter on `ticket.status` |
| Search and status filter may be combined | AND semantics: match keyword (if present) **and** status (if present) |

Indexes to speed search/filter are **implementation choices**, not product requirements. No full-text search engine is required.

Invalid status filter values are an API/validation concern (reject vs empty list is settled in the API contract); the data model only stores the five enum values.

---

## 8. Timestamp behaviour

| Event | `createdAt` (ticket) | `updatedAt` (ticket) | `createdAt` (comment) |
|-------|----------------------|----------------------|------------------------|
| Ticket create | set | set (same instant) | — |
| Field update | unchanged | set to update time | — |
| Status transition | unchanged | set to transition time | — |
| Comment add | unchanged | **unchanged** (unless a later decision says otherwise) | set |
| Comment edit/delete | — | — | not applicable |

Timezone representation (`TIMESTAMPTZ` vs local timestamp) is an implementation choice; prefer storing an unambiguous instant for the running app.

---

## 9. Proposed PostgreSQL schema

Illustrative physical design aligned to the logical model. Column type choices marked as proposals where alternatives remain valid.

```sql
CREATE TABLE ticket (
    id          UUID PRIMARY KEY,
    title       TEXT NOT NULL,
    description TEXT NOT NULL,
    priority    VARCHAR(16) NOT NULL,
    assignee    TEXT NULL,
    status      VARCHAR(32) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT ticket_priority_chk CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT ticket_status_chk CHECK (status IN (
        'OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'CANCELLED'
    ))
);

CREATE TABLE comment (
    id         UUID PRIMARY KEY,
    ticket_id  UUID NOT NULL REFERENCES ticket (id),
    body       TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

-- Optional indexes (implementation choice, not required by product):
-- CREATE INDEX idx_ticket_status ON ticket (status);
-- CREATE INDEX idx_comment_ticket_id ON comment (ticket_id);
```

**Notes:**

- `TEXT` avoids inventing unagreed length limits.
- `VARCHAR` + `CHECK` for enums is a common portable approach; native PostgreSQL `ENUM` types are an alternative implementation choice.
- No `ON DELETE CASCADE` is mandated; ticket deletion is out of product scope.
- Schema migration tooling (e.g. Flyway) remains an implementation decision per architecture.

---

## 10. H2 testing considerations

H2 is used where practical for backend integration tests. Be aware of differences that can make tests green while PostgreSQL behaves differently:

| Area | Risk |
|------|------|
| **Case-insensitive search** | PostgreSQL `ILIKE` is not identical to H2 defaults. Prefer a portable approach in tests/app (e.g. `LOWER(column) LIKE LOWER(...)`) or document H2 mode/compatibility settings explicitly. |
| **UUID type** | Confirm H2 UUID mapping matches the JPA type used against PostgreSQL. |
| **Timestamps / time zones** | `TIMESTAMPTZ` semantics differ; assert on logical behaviour, not driver-specific string formats. |
| **CHECK / enum storage** | H2 accepts CHECK constraints, but native PG ENUM types would not port cleanly—prefer VARCHAR+CHECK or application-only enum validation for dual-DB simplicity. |
| **SQL dialect** | Avoid PostgreSQL-only functions in repository queries unless tests use an equivalent path or Testcontainers PostgreSQL later. |

Passing transition and search tests on H2 does not prove PostgreSQL-specific SQL. Keep queries as portable as practical for this assignment.

---

## 11. Requirements vs project decisions vs implementation choices

### Requirements (from assignment)

- Persist tickets with title, description, priority, assignee, status
- Persist comments related to tickets
- Keyword search and status filtering
- Backend-enforced status transitions
- Validation and durable storage (PostgreSQL / H2 in the stack)

### Project decisions (agreed interpretations / locked model)

- UUID ids for ticket and comment
- Priority enum: `LOW` | `MEDIUM` | `HIGH`
- Assignee: optional string (no User entity)
- Create always `OPEN`; client cannot set initial status
- Closed five-edge status matrix; same-status transitions invalid
- Field update must not change status; dedicated transition operation
- Field updates allowed on `CLOSED` / `CANCELLED`
- Comments: body + created timestamp; no edit/delete
- Comments allowed on any existing ticket including terminal statuses
- Search: case-insensitive on title and description; single status filter; combinable
- Validation floor: non-blank title, description, comment body; valid priority; optional assignee
- No length limits beyond non-blank
- No User, auth, roles, attachments, tags, SLA, audit log, notifications, soft delete

### Implementation choices (not silent requirements)

| Choice | Status |
|--------|--------|
| `TEXT` vs bounded `VARCHAR` | Prefer `TEXT` to avoid invented limits; either is storage detail |
| `TIMESTAMPTZ` vs `TIMESTAMP` | Prefer instant (`TIMESTAMPTZ`); confirm in implementation |
| UUID generation (DB `gen_random_uuid()` vs application) | Either |
| Enum storage: VARCHAR+CHECK vs PG ENUM vs JPA `@Enumerated` only | Prefer portable VARCHAR+CHECK or app validation for H2 parity |
| Indexes on `status` / `ticket_id` / expression indexes for search | Optional |
| Whether empty assignee string is normalized to `null` | Recommend normalize to `null`; not yet a hard product rule |
| Whether adding a comment bumps ticket `updatedAt` | Default in this doc: **no**; change only if product asks |
| FK `ON DELETE` action | Irrelevant while ticket delete is out of scope |
| Flyway / Liquibase / Hibernate `ddl-auto` for schema | Implementation / ops choice |
| Search SQL shape (`ILIKE` vs `LOWER`/`LIKE`) | Choose for PostgreSQL + H2 portability |

---

## Explicitly absent from this model

Do not add: users, authors, roles, attachments, tags, categories, SLA, priority history, status transition history tables, audit logs, notifications, soft-delete columns, or comment update metadata.
