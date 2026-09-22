# Architecture — Support Ticket Management System

Working architecture for a small assignment. This document describes how the system is structured and which concerns own which rules. It is not a data model, API contract, UI flow, or test-strategy document; those are specified separately.

---

## 1. Purpose and scope

**Purpose:** Define a simple, implementable architecture for a support-ticket application that can create, list, view, update, comment on, search, and filter tickets, with backend-enforced status transitions, validation, meaningful API errors, and durable persistence.

**In scope for this architecture:**

- One Spring Boot backend and one React (Vite) frontend in a single repository
- Layering, ownership of business rules (especially the status state machine), persistence approach, error-handling approach, testing boundaries, configuration/secret handling, and repository layout

**Out of scope for this document:** concrete table schemas, OpenAPI field lists, screen wireframes, and detailed test plans (covered later in dedicated specs).

---

## 2. Architecture overview

The system is a **modular monolith** with a **React + Vite SPA**. For the **final demo**, Spring Boot serves the production frontend build as static resources on **one port (9000)** so the evaluator opens a single URL.

```
Browser
   │  http://localhost:9000  (SPA + /api/v1)
   ▼
Spring Boot (Java 21)  — REST API + static SPA assets
   │
   ▼
PostgreSQL (application persistence)

Tests: H2 where practical (backend integration)

Optional local UI iteration only: Vite on 9001 with /api proxy → 9000
(not the evaluator demo)
```

| Part | Role |
|------|------|
| **Frontend** | Present tickets, collect input, call the API with **relative** `/api/v1/...` paths, display errors. May hide illegal status options as UX only. |
| **Backend** | Validation, persistence, SPA static serving, and the **only** authority for status transitions and other business rules. |
| **Database** | Durable storage; data must survive application restart. |

No microservices, message buses, caches, API gateways, or CQRS. Complexity stays proportional to the assignment.

---

## 3. Backend architecture and layers

Classic Spring layering. Controllers stay thin; business rules live below HTTP.

| Layer | Responsibility | Must not |
|-------|----------------|----------|
| **API (controllers + request/response DTOs)** | Map `/api/v1/...`, bind and validate request DTOs (`@Valid`), call one service method, return response DTOs; `201` + `Location` on create | Status machine, JPA entities on the wire, assignment/transition rules |
| **Application (services)** | Use cases: create, list/search/filter, get, update fields, change status, add comment. `@Transactional` here. Map entity ↔ DTO | HTTP types; SQL in controllers |
| **Domain** | Status enum, transition policy (allowed edges only), domain exceptions | Persistence annotations on API types; HTTP concerns |
| **Persistence** | JPA entities, Spring Data repositories, load/save/query | “Is this transition allowed?” |
| **Exception handling** | Map domain/validation failures to one Problem Details JSON shape | Swallow errors or return `200` with an error body |

**Suggested package layout (single backend module):**

`api` → `application` → `domain` → `persistence`

**Use cases (one ticket service is enough):**

- Create ticket (server sets status to `OPEN`; client does not choose initial status)
- List with optional keyword and status filter
- Get by id (ticket fields; comments via dedicated list)
- Update fields (title, description, priority, assignee — not status)
- Transition status (backend-enforced matrix)
- Add comment

Prefer Java 21 records for DTOs, constructor injection, and enums for status (and for priority once the data model defines values).

---

## 4. Frontend architecture

A small Vite + React SPA. No Next.js, no global state library unless later work shows a clear need.

**Suggested structure:**

```
frontend/src/
  api/          HTTP client (tickets, comments, error parsing)
  pages/        list, create, detail
  components/   forms, comment UI, status control, error display
  types/        mirrors API DTOs
```

**Screens (details in UI-flow spec):**

1. **Dashboard** — summary counts from existing ticket list data (no new stats API)  
2. **List** — keyword + status filter, results, navigation to detail/create  
3. **Create** — ticket fields without a status picker  
4. **Detail** — fields, assignee, comments, status action, API errors  

The UI may offer only legal next statuses for usability. Rejection of illegal transitions remains a backend responsibility (`409` or equivalent per API contract).

---

## 5. Backend / frontend communication

Communication is **JSON over HTTP REST**, with versioned resource paths under `/api/v1/...`. No WebSockets, no RPC-style path verbs (e.g. `/updateStatus`).

**Capabilities the architecture must support** (exact paths and payloads are finalized in the API contract):

| Capability | Architectural intent |
|------------|----------------------|
| Create | `POST` tickets → success as create (`201` + `Location` per API standards) |
| List / search / filter | `GET` tickets with optional keyword and status |
| Details | `GET` ticket by id; comments via dedicated list endpoint |
| Field update | Partial update of title, description, priority, assignee — **not** status |
| Status change | Dedicated status operation, separate from field update |
| Comment | Create comment nested under a ticket |

**Final demo wiring:** Spring Boot serves the Vite production build from `classpath:/static` on port **9000**. The SPA calls relative `/api/v1/...` paths (same origin). Optional Vite-on-9001 with `/api` proxy is for UI-only iteration, not the evaluator demo.

---

## 6. Persistence approach

| Concern | Approach |
|---------|----------|
| **Application database** | PostgreSQL |
| **Test database** | H2 where practical |
| **Access** | Spring Data JPA; repositories load, save, and query only |
| **Durability** | Real DB for the running app — not memory-only storage |
| **Identifiers** | **UUID** for ticket and comment primary keys in the data model, unless a later concrete constraint shows a reason not to |
| **Minimal entities** | Ticket and Comment as related rows (comments are not a single blob). Exact columns belong in the data-model spec. |

**Schema evolution:** use a migration tool if/when schema versioning is needed. Choosing Flyway (or an alternative) is an **implementation decision**, not a product requirement.

No users table and no extra domain tables beyond what tickets and comments require.

---

## 7. State-machine ownership

**Owner:** domain layer, invoked by the ticket application service. Controllers, repositories, and the frontend do not own transition rules.

**Closed allowed transitions:**

| From | To |
|------|-----|
| `OPEN` | `IN_PROGRESS` |
| `OPEN` | `CANCELLED` |
| `IN_PROGRESS` | `RESOLVED` |
| `IN_PROGRESS` | `CANCELLED` |
| `RESOLVED` | `CLOSED` |

All other transitions are invalid, including skip-ahead, reopen, and same-status no-ops.

**Create rule:** new tickets are created as `OPEN`. The client must not set initial status.

**Runtime behaviour:**

1. Service loads the ticket.  
2. Domain transition policy accepts or rejects the attempted status.  
3. On accept, persist; on reject, raise a domain conflict exception, roll back the transaction, leave the ticket unchanged.  

The UI may filter buttons; filtering is not enforcement.

---

## 8. Error-handling approach

```
Request DTO + Jakarta Validation
        │ fail → 400 + field errors
        ▼
Service / domain
        │ not found → 404
        │ illegal transition → 409 (current, attempted, not allowed)
        │ other domain/validation failures → 400
        ▼
@RestControllerAdvice → one Problem Details shape (RFC 7807 preferred)
```

- Do not use `500` for expected domain failures.  
- Do not return stack traces, SQL, or persistence internals.  
- Do not implement `401`/`403` flows — authentication is out of scope.  
- Frontend surfaces `detail` and field errors to the user.

Illegal transitions reject the **whole** status operation; no partial status write.

---

## 9. Testing boundaries

Tests prove product behaviour, especially the status machine—not line coverage alone.

| Boundary | What it covers | Notes |
|----------|----------------|-------|
| **Domain / service unit** | Every allowed edge; every rejected pair (including same-status); create does not accept client status; field update does not change status | Prefer no full Spring context; drive transitions through domain/service APIs, not “set field and save” |
| **Backend API + DB integration** | Create, get, persist legal transition, `404`, invalid filter → client error, illegal transition → conflict + stable error shape | **Mandatory** for state-machine behaviour; Spring tests with H2 where practical |
| **Frontend verification** | UI flows against the API: list/create/detail, filters, status actions, and meaningful error display | Included in the overall test strategy; does not replace backend state-machine integration tests |

Controller-only smoke tests do not substitute for domain or integration coverage of transitions.

Detailed cases belong in a later test-strategy document; this architecture only sets the boundaries.

---

## 10. Configuration and secret handling

- Datasource URL, username, and password come from **environment variables / Spring externalized config**, never committed secrets.  
- Application config uses placeholders; local override files are gitignored.  
- Document **variable names** in README or `docs/`, never real passwords.  
- H2 settings belong to the **test** profile only.  
- Frontend may use a public API base URL; prefer the Vite proxy in development. No secrets in the frontend bundle.

---

## 11. Repository structure

```
backend/          Spring Boot application (serves API + SPA on port 9000 for demo)
frontend/         React + Vite sources (production build embedded into backend)
spec/             product and architecture specs
docs/             durable decision records, manual tests, AI evidence
.cursor/rules/    build conventions (layering, API, testing, docs, reviews)
README.md         how to run the single-port portal; pointers to docs
```

Chat history is not the long-term source of truth. Specs and decisions in the repo are.

---

## 12. Explicit out-of-scope items

Do **not** add the following unless the assignment is explicitly extended:

- Authentication, users, roles, permissions  
- Notifications, email, webhooks  
- Pagination, sorting beyond what a later API contract requires for the basic list  
- Attachments / file upload  
- SLA timers or escalation  
- Audit logging / event sourcing  
- Comment edit/delete, searching comment bodies (unless a later product decision says otherwise)  
- Reopen or skip-level status transitions; client-chosen create status  
- Microservices, Redis, Kafka, CQRS, API gateway, message buses  

---

## 13. Implementation decisions vs product requirements

### Product requirements / locked project interpretations

| Item | Status |
|------|--------|
| Java 21, Spring Boot, REST backend | Product / stack requirement |
| React + Vite frontend | Chosen stack for the assignment UI |
| PostgreSQL for the running app; H2 for tests where practical | Persistence requirement + practical test approach |
| Ticket capabilities: create, list, detail, field updates, assignee changes, comments, keyword search, status filter | Product |
| Backend validation and meaningful errors the UI can show | Product |
| Closed five-edge status machine; all other transitions invalid | Locked interpretation |
| Create always `OPEN`; client cannot set initial status | Locked interpretation |
| Backend owns the state machine | Product / project rule |
| Field updates separate from status changes at the API capability level | Architectural intent; concrete shape in API contract |
| UUID identifiers for tickets and comments | Chosen for the data model unless a later constraint forbids it |
| Frontend verification in the test strategy; backend state-machine integration tests mandatory | Testing boundary |

### Implementation / design decisions (not extra product features)

| Item | Notes |
|------|--------|
| Monorepo with `backend/` + `frontend/` | Layout convenience |
| Package split `api` / `application` / `domain` / `persistence` | Layering convenience |
| `/api/v1/...` versioning | Project API convention |
| **`PATCH /tickets/{id}/status` (or equivalent)** | **API design decision — finalize in the API contract** |
| Embedding comments in ticket detail `GET` | Likely API design choice; finalize in API contract |
| Flyway (or other migration tool) | **Implementation decision, not a product requirement** |
| Vite production build embedded into Spring Boot static resources; SPA fallback controller | Final single-port demo on 9000 |
| Optional Vite `:9001` `/api` proxy | Local UI iteration only |
| RFC 7807 Problem Details, `409` for illegal transitions | Project API standards; adopt unless API contract says otherwise |
| Spring Data JPA | Persistence technology choice within the stack |

---

## Resolved in later specs

The following were left open at architecture time and are now settled in dedicated specs (do not re-open casually):

| Topic | Settled in |
|-------|------------|
| REST paths, DTOs, errors | `spec/api-contract.md` |
| Priority, assignee, comments, timestamps | `spec/data-model.md` |
| Status matrix and enforcement | `spec/state-machine.md` |
| Screens and navigation (incl. dashboard) | `spec/ui-flow.md` |
| Automated + manual test coverage | `spec/test-strategy.md`, `docs/manual-test-cases.md` |
| Product requirements summary | `spec/requirements.md` |
