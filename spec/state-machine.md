# State Machine — Support Ticket Management System

Formal ticket-status state machine for the Support Ticket Management System. Source of truth: the locked five-edge transition decision recorded in `spec/architecture.md`, `spec/data-model.md`, and `spec/api-contract.md`.

This document is **not** application code, an API catalogue, a data schema, a UI flow, or a full test-strategy document. It defines status behaviour only.

---

## 1. Purpose and scope

**Purpose:** Specify every legal and illegal ticket-status transition, how transitions are requested and rejected, and which layer owns enforcement.

**In scope:**

- The five `TicketStatus` values
- The complete allowed transition set (exactly five edges)
- Invalid transitions (including same-status and terminal-state exits)
- Creation always starting as `OPEN`
- Separation of field update from status change
- Backend-owned enforcement and transactional failure behaviour
- Required tests for allowed and invalid transitions
- Explicit absence of automatic transitions, reopening, skipping, and post-terminal moves

**Out of scope for this document (and not introduced here):**

- Assignee, priority, title, or description as transition guards
- Roles, permissions, or approval workflows
- Automatic transitions, timers, or escalations
- Audit / status-history tables or event sourcing
- Comment lifecycle rules beyond noting they do not change status
- Changing the five allowed transitions

---

## 2. Ticket states

| State | Meaning (product) | Terminal? |
|-------|-------------------|-----------|
| `OPEN` | Newly created / not yet started | No |
| `IN_PROGRESS` | Work is underway | No |
| `RESOLVED` | Work completed; awaiting close | No |
| `CLOSED` | Ticket finished | **Yes** |
| `CANCELLED` | Ticket abandoned | **Yes** |

These are the only valid `TicketStatus` values. No additional statuses exist.

**Terminal states:** `CLOSED` and `CANCELLED` have **no** outbound transitions. Once a ticket reaches either state, its status cannot change again through any operation.

---

## 3. Complete transition matrix

Rows are **current** status; columns are **attempted** status. `Y` = allowed; blank = invalid.

| Current \ Target | `OPEN` | `IN_PROGRESS` | `RESOLVED` | `CLOSED` | `CANCELLED` |
|------------------|--------|---------------|------------|----------|-------------|
| `OPEN` | | Y | | | Y |
| `IN_PROGRESS` | | | Y | | Y |
| `RESOLVED` | | | | Y | |
| `CLOSED` | | | | | |
| `CANCELLED` | | | | | |

Same-status cells (diagonal) are intentionally blank: a request to transition to the ticket’s current status is **invalid**.

---

## 4. Allowed transitions

Exactly these five edges are valid. No others.

| # | From | To |
|---|------|----|
| 1 | `OPEN` | `IN_PROGRESS` |
| 2 | `IN_PROGRESS` | `RESOLVED` |
| 3 | `RESOLVED` | `CLOSED` |
| 4 | `OPEN` | `CANCELLED` |
| 5 | `IN_PROGRESS` | `CANCELLED` |

There are **no guards** on these edges (for example, assignee is **not** required to enter `IN_PROGRESS`). Legality depends only on `(currentStatus, targetStatus)`.

---

## 5. Invalid transitions

Every pair not listed in §4 is invalid. That includes, without exception:

### 5.1 Same-status (no-ops are not allowed)

| From | To |
|------|----|
| `OPEN` | `OPEN` |
| `IN_PROGRESS` | `IN_PROGRESS` |
| `RESOLVED` | `RESOLVED` |
| `CLOSED` | `CLOSED` |
| `CANCELLED` | `CANCELLED` |

### 5.2 Skip-ahead / shortcut

| From | To |
|------|----|
| `OPEN` | `RESOLVED` |
| `OPEN` | `CLOSED` |
| `IN_PROGRESS` | `CLOSED` |

### 5.3 Reopen / backward

| From | To |
|------|----|
| `IN_PROGRESS` | `OPEN` |
| `RESOLVED` | `OPEN` |
| `RESOLVED` | `IN_PROGRESS` |
| `CLOSED` | `OPEN` |
| `CLOSED` | `IN_PROGRESS` |
| `CLOSED` | `RESOLVED` |
| `CLOSED` | `CANCELLED` |
| `CANCELLED` | `OPEN` |
| `CANCELLED` | `IN_PROGRESS` |
| `CANCELLED` | `RESOLVED` |
| `CANCELLED` | `CLOSED` |

### 5.4 Other forbidden edges

| From | To |
|------|----|
| `RESOLVED` | `CANCELLED` |

### 5.5 Exhaustive count

With five statuses there are `5 × 5 = 25` ordered pairs. Exactly **5** are allowed; the remaining **20** are invalid. Implementations and tests must treat all 20 as rejected.

---

## 6. Transition request / response behaviour

Status changes happen **only** through the dedicated status operation defined by the API contract:

**`PATCH /api/v1/tickets/{ticketId}/status`**

Request body:

```json
{
  "status": "<target TicketStatus>"
}
```

### Behaviour on a legal transition

1. Backend loads the ticket by id.
2. Domain transition policy accepts `(currentStatus, targetStatus)`.
3. Persist the new `status` and refresh `updatedAt`.
4. Leave `title`, `description`, `priority`, and `assignee` unchanged.
5. Return HTTP **`200`** with the full ticket representation after the transition.

### Behaviour that is not a status transition

| Operation | Status effect |
|-----------|---------------|
| `POST /api/v1/tickets` | Server sets `OPEN`; client must not send `status` |
| `PATCH /api/v1/tickets/{ticketId}` (field update) | **Must not** change `status`; sending `status` is a **`400`** validation/unsupported-field error |
| Comment create / list | Does not change ticket `status` |

The normal field `PATCH` is never a status channel.

---

## 7. Error behaviour for invalid transitions

When the ticket exists, the request body contains a valid enum `status` value, and `(currentStatus, targetStatus)` is not one of the five allowed edges:

| Concern | Rule |
|---------|------|
| HTTP status | **`409 Conflict`** |
| Body | RFC 7807 Problem Details (`application/problem+json`) |
| `detail` | Must state that the transition is not allowed and identify **current** and **attempted** status |
| Extension fields | `currentStatus` and `attemptedStatus` as defined in the API contract |
| Persistence | Ticket `status` and `updatedAt` **unchanged** |

### Related non-409 cases (not illegal-edge conflicts)

| Situation | HTTP | Notes |
|-----------|------|-------|
| Missing / unknown ticket id | `404` | Not a transition decision |
| Missing `status`, unknown enum string, malformed JSON, invalid UUID | `400` | Validation / malformed request |
| `status` (or other disallowed fields) on field `PATCH` | `400` | Wrong operation; not evaluated as a transition |
| Client sends `status` on create | `400` | Disallowed field on create |

Do not return `200` for an illegal transition. Do not silently leave status unchanged without an error. Do not use `500` for expected illegal-transition failures.

---

## 8. State-machine invariants

These invariants always hold:

1. **Closed set:** A ticket’s `status` is always exactly one of `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`.
2. **Create invariant:** Immediately after successful create, `status == OPEN`.
3. **Edge invariant:** After any successful status operation, the change corresponds to exactly one of the five allowed edges.
4. **No same-status success:** A successful response never leaves status equal to the pre-request status as the result of a status operation that “succeeded”; same-status is always `409`.
5. **Terminal invariant:** From `CLOSED` or `CANCELLED`, no status operation can succeed.
6. **Single channel:** `status` changes only via the dedicated status operation (or server-side create defaulting to `OPEN`). Field update never mutates `status`.
7. **No collateral field mutation:** A pure status transition does not alter `title`, `description`, `priority`, or `assignee`.
8. **Backend authority:** Enforcement lives in the domain/application layer. Controllers, repositories, and the frontend do not define or replace the matrix.
9. **No extra rules:** Transition legality does not depend on assignee, priority, comments, roles, or approvals.

---

## 9. Creation behaviour

| Rule | Detail |
|------|--------|
| Initial status | Always `OPEN` |
| Who sets it | Backend service on create |
| Client | Must **not** provide `status` (or `id`, `createdAt`, `updatedAt`) |
| Field `PATCH` | Cannot set or change `status` |
| First legal moves from create | Only `OPEN → IN_PROGRESS` or `OPEN → CANCELLED` |

Create does not accept an initial status. There is no create-time transition matrix beyond “force `OPEN`”.

---

## 10. Backend enforcement responsibility

| Layer | Responsibility for the state machine |
|-------|--------------------------------------|
| **Domain** | Owns the transition policy (allowed edges only); raises a domain conflict when illegal |
| **Application (service)** | Loads ticket, invokes domain policy, persists on accept, rolls back on reject; `@Transactional` around the use case |
| **API (controllers)** | Bind/validate DTOs; call the transition use case; map exceptions to Problem Details — **no** matrix logic |
| **Persistence** | Load/save only — **no** “is this transition allowed?” in repositories |
| **Frontend** | May hide illegal next statuses for UX only; **never** the enforcement mechanism |

The backend is the **authoritative** owner of this state machine. UX filtering does not replace `409` rejection of illegal edges.

---

## 11. Transactional behaviour when a transition fails

For `PATCH .../status`:

1. Begin a transaction in the application service.
2. Load the current ticket (or fail with not-found outside / before transition logic).
3. Evaluate `(currentStatus, targetStatus)` in the domain.
4. **If illegal:** raise a domain conflict exception; **roll back** the transaction; leave `status` and `updatedAt` (and all other fields) exactly as before the request.
5. **If legal:** write the new `status`, refresh `updatedAt`, commit.

Illegal transitions reject the **whole** status operation. There is no partial status write.

Concurrent callers: serialization is via normal database transaction / row locking as described in the API contract. The first successful commit wins; the second re-evaluates against the updated current status and either succeeds on a still-legal edge or receives `409`. Never silently ignore an illegal transition.

---

## 12. Required tests for every allowed transition

Drive transitions through the **domain / service API** (and, for HTTP/DB proof, through the dedicated status endpoint). Do **not** prove a transition by setting the entity field and saving.

| # | Test | Arrange | Act | Assert |
|---|------|---------|-----|--------|
| A1 | `OPEN → IN_PROGRESS` | Ticket in `OPEN` | Transition to `IN_PROGRESS` | Success; status `IN_PROGRESS`; `updatedAt` refreshed; other fields unchanged |
| A2 | `IN_PROGRESS → RESOLVED` | Ticket in `IN_PROGRESS` | Transition to `RESOLVED` | Success; status `RESOLVED`; fields other than status/`updatedAt` unchanged |
| A3 | `RESOLVED → CLOSED` | Ticket in `RESOLVED` | Transition to `CLOSED` | Success; status `CLOSED` |
| A4 | `OPEN → CANCELLED` | Ticket in `OPEN` | Transition to `CANCELLED` | Success; status `CANCELLED` |
| A5 | `IN_PROGRESS → CANCELLED` | Ticket in `IN_PROGRESS` | Transition to `CANCELLED` | Success; status `CANCELLED` |

Also required (creation / channel separation, related to the machine):

| # | Test | Assert |
|---|------|--------|
| C1 | Create ticket | Resulting status is `OPEN`; client-supplied `status` rejected |
| C2 | Field `PATCH` with `status` | `400`; ticket status unchanged |
| C3 | Field `PATCH` without `status` | Fields update; status unchanged |

Integration coverage of at least one successful persisted transition and one illegal-transition `409` with stable Problem Details shape is mandatory (per architecture testing boundaries).

---

## 13. Required tests for invalid transitions

### 13.1 Complete coverage expectation

Every one of the **20** invalid ordered pairs must be rejected (domain/service unit tests). HTTP/integration tests must cover representative illegal edges with `409` and stable error shape, including current and attempted status in the error.

### 13.2 Representative invalid cases (minimum named cases)

| # | From | To | Why it matters |
|---|------|----|----------------|
| I1 | `OPEN` | `OPEN` | Same-status not idempotent success |
| I2 | `OPEN` | `RESOLVED` | Skip-ahead |
| I3 | `OPEN` | `CLOSED` | Skip-ahead to terminal |
| I4 | `IN_PROGRESS` | `OPEN` | Reopen / backward |
| I5 | `IN_PROGRESS` | `CLOSED` | Skip `RESOLVED` |
| I6 | `IN_PROGRESS` | `IN_PROGRESS` | Same-status |
| I7 | `RESOLVED` | `OPEN` | Reopen |
| I8 | `RESOLVED` | `IN_PROGRESS` | Backward |
| I9 | `RESOLVED` | `CANCELLED` | Forbidden lateral / cancel after resolve |
| I10 | `CLOSED` | `OPEN` | Terminal exit / reopen |
| I11 | `CLOSED` | `IN_PROGRESS` | Terminal exit |
| I12 | `CLOSED` | `CLOSED` | Same-status on terminal |
| I13 | `CANCELLED` | `OPEN` | Terminal exit / reopen |
| I14 | `CANCELLED` | `IN_PROGRESS` | Terminal exit |
| I15 | `CANCELLED` | `CANCELLED` | Same-status on terminal |

### 13.3 Complete invalid set (must all fail)

All of the following must reject (status and timestamps unchanged):

`OPEN→OPEN`, `OPEN→RESOLVED`, `OPEN→CLOSED`,  
`IN_PROGRESS→OPEN`, `IN_PROGRESS→IN_PROGRESS`, `IN_PROGRESS→CLOSED`,  
`RESOLVED→OPEN`, `RESOLVED→IN_PROGRESS`, `RESOLVED→RESOLVED`, `RESOLVED→CANCELLED`,  
`CLOSED→OPEN`, `CLOSED→IN_PROGRESS`, `CLOSED→RESOLVED`, `CLOSED→CLOSED`, `CLOSED→CANCELLED`,  
`CANCELLED→OPEN`, `CANCELLED→IN_PROGRESS`, `CANCELLED→RESOLVED`, `CANCELLED→CLOSED`, `CANCELLED→CANCELLED`.

For each: assert conflict/domain failure (HTTP `409` at the API), unchanged persisted `status`, and error detail identifying current and attempted status.

---

## 14. Examples of valid and invalid requests

### 14.1 Valid — `OPEN → IN_PROGRESS`

**Request:** `PATCH /api/v1/tickets/{ticketId}/status`

```json
{
  "status": "IN_PROGRESS"
}
```

**Result:** `200`; ticket `status` is `IN_PROGRESS`; `updatedAt` newer than before.

### 14.2 Valid — `RESOLVED → CLOSED`

```json
{
  "status": "CLOSED"
}
```

**Result:** `200`; ticket `status` is `CLOSED`.

### 14.3 Valid — `OPEN → CANCELLED`

```json
{
  "status": "CANCELLED"
}
```

**Result:** `200`; ticket `status` is `CANCELLED`.

### 14.4 Invalid — skip-ahead `OPEN → CLOSED`

```json
{
  "status": "CLOSED"
}
```

**Result:** `409`; ticket remains `OPEN`. Example Problem Details shape (aligned with API contract):

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Transition from OPEN to CLOSED is not allowed.",
  "instance": "/api/v1/tickets/{ticketId}/status",
  "currentStatus": "OPEN",
  "attemptedStatus": "CLOSED"
}
```

### 14.5 Invalid — same-status `IN_PROGRESS → IN_PROGRESS`

```json
{
  "status": "IN_PROGRESS"
}
```

**Result:** `409`; status remains `IN_PROGRESS`.

### 14.6 Invalid — terminal exit `CLOSED → OPEN`

```json
{
  "status": "OPEN"
}
```

**Result:** `409`; status remains `CLOSED`.

### 14.7 Invalid channel — field PATCH attempting status

**Request:** `PATCH /api/v1/tickets/{ticketId}`

```json
{
  "status": "IN_PROGRESS"
}
```

**Result:** `400` (disallowed field for field update); not evaluated as a state-machine edge; status unchanged.

### 14.8 Invalid create — client-supplied status

**Request:** `POST /api/v1/tickets` with a `status` property in the body.

**Result:** `400`; if create had been allowed without that field, status would be `OPEN`—with the field present, the request is rejected and no ticket is created from that illegal payload.

---

## 15. Explicit non-features of this state machine

The following **do not exist** in this product’s status model:

| Non-feature | Statement |
|-------------|-----------|
| **Automatic transitions** | No timer, scheduler, webhook, or side effect moves status without an explicit dedicated status request |
| **Reopening** | No transition from `CLOSED` or `CANCELLED` (or from later states back to earlier ones) |
| **Skipping states** | No `OPEN → RESOLVED`, `OPEN → CLOSED`, or `IN_PROGRESS → CLOSED` |
| **Terminal-state transitions** | No outbound edges from `CLOSED` or `CANCELLED` |
| **Same-status success** | Retrying the current status is illegal (`409`), not idempotent success |
| **Guards** | No “assignee required”, “comment required”, or similar preconditions |
| **Roles / permissions** | No actor-based transition rules |
| **Approval workflows** | No multi-step approval to change status |
| **Audit / history requirements** | No mandated status-history table or audit trail for transitions |

---

## Alignment with other specs

| Spec | Relationship |
|------|----------------|
| `spec/architecture.md` §7 | Same five edges; domain ownership; reject → conflict + rollback |
| `spec/data-model.md` §4.1 / §6 | Same enum and transitions; create `OPEN`; field update must not change status |
| `spec/api-contract.md` §7 / §11 | Dedicated `PATCH .../status`; `409` + Problem Details for illegal edges |

If this document and another locked spec ever disagree on the five edges or create/`OPEN` rules, **stop and resolve the contradiction explicitly**—do not silently invent a sixth edge or a reopen path.
)
