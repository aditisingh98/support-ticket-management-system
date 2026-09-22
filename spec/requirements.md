# Requirements — Support Ticket Management System

Product requirements derived from the original assignment and locked project decisions already recorded in this repository. This document does **not** invent additional product scope.

---

## 1. Purpose

Build a Support Ticket Management System that lets operators create, list, search, filter, view, update, comment on, and transition support tickets, with backend-enforced status rules, validation, meaningful errors, and durable persistence.

---

## 2. Functional requirements

| # | Requirement |
|---|-------------|
| R1 | Create a ticket with title, description, priority, and optional assignee |
| R2 | A newly created ticket always starts as `OPEN`; the client must not choose initial status |
| R3 | List tickets |
| R4 | View ticket details |
| R5 | Update ticket fields: title, description, priority, assignee |
| R6 | Status must not change through the normal field update |
| R7 | Change status only via a dedicated status operation |
| R8 | Add comments to a ticket |
| R9 | List comments where the UI needs them |
| R10 | Keyword search (case-insensitive) against title and description only |
| R11 | Filter by a single status |
| R12 | Keyword and status filter may be combined with AND |
| R13 | Backend validates inputs and returns meaningful errors |
| R14 | Persist tickets and comments so data survives application restart |

---

## 3. Ticket status state machine (locked)

**Allowed transitions only:**

| From | To |
|------|-----|
| `OPEN` | `IN_PROGRESS` |
| `IN_PROGRESS` | `RESOLVED` |
| `RESOLVED` | `CLOSED` |
| `OPEN` | `CANCELLED` |
| `IN_PROGRESS` | `CANCELLED` |

Every other transition is invalid (including same-status, skip-ahead, reopen, and any exit from `CLOSED` / `CANCELLED`). Invalid transitions must be rejected by the backend and must not change ticket state.

---

## 4. Field and comment rules (locked)

| Area | Rule |
|------|------|
| Create | `title`, `description`, `priority` required (non-blank); `assignee` optional; `status` not accepted |
| Priority | `LOW` \| `MEDIUM` \| `HIGH` |
| Field update | Only title, description, priority, assignee; allowed even when `CLOSED` or `CANCELLED` |
| Comments | `body` required and non-blank; allowed on any existing ticket including terminal statuses; no edit/delete |
| Auth | No authentication, users, roles, or JWT |

---

## 5. Error behaviour (locked)

| Situation | HTTP |
|-----------|------|
| Validation / malformed / invalid status filter / disallowed fields | `400` |
| Ticket not found | `404` |
| Illegal status transition | `409` |

Meaningful JSON error responses (Problem Details). No `401` / `403` implementation.

---

## 6. Technology requirements

| Layer | Choice |
|-------|--------|
| Backend | Java 21, Spring Boot, Spring Data JPA, REST, Jakarta Bean Validation |
| Application DB | PostgreSQL |
| Tests | H2 where appropriate |
| Frontend | React + Vite |
| Final demo | Single Spring Boot process on port **9000** serving the built SPA and `/api/v1` |

---

## 7. Delivery / demo requirements

- Final portal URL: **http://localhost:9000**
- API base: **http://localhost:9000/api/v1**
- Frontend calls the API with relative paths (e.g. `/api/v1/tickets`)
- Do not require separate frontend and backend demo servers for the evaluator
- Do not use 8000-series ports for the final application

---

## 8. Explicit non-requirements

Do **not** add: authentication, users table, roles, JWT, notifications, attachments, SLA, pagination, audit log, Redis, Kafka, microservices, CQRS, ETag, comment edit/delete, reopen/skip transitions, or other infrastructure beyond what is needed for the above.

---

## 9. Spec-driven process

Work proceeds as:

**Requirement → Specification → Plan / Tasks → Implementation → Testing → Review → Fix**

Detailed behaviour lives in:

- `spec/architecture.md`
- `spec/data-model.md`
- `spec/api-contract.md`
- `spec/state-machine.md`
- `spec/ui-flow.md`
- `spec/test-strategy.md`
