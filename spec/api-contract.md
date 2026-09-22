# API Contract — Support Ticket Management System

HTTP JSON contract for the Support Ticket Management System. Source of truth: the assignment, `spec/architecture.md`, and `spec/data-model.md`.

This document is **not** application code, OpenAPI generation output, or UI flow.

---

## 1. API conventions

| Convention | Value |
|------------|--------|
| **Base path** | `/api/v1` |
| **Content type** | `application/json` for request and response bodies (except empty bodies) |
| **Error content type** | `application/problem+json` (RFC 7807 Problem Details) |
| **Character encoding** | UTF-8 |
| **JSON field names** | `camelCase` |
| **UUID representation** | Canonical string, e.g. `"550e8400-e29b-41d4-a716-446655440000"` |
| **Timestamp representation** | ISO-8601 instant in UTC with `Z`, e.g. `"2026-09-22T08:15:30.123Z"` |
| **Enum representation** | Uppercase strings as defined (`OPEN`, `LOW`, …) |
| **Auth** | None. Do not return `401` / `403` for this product. |
| **Unknown JSON properties** | Rejected with `400` (not silently ignored). |

**Resource style:** nouns and sub-resources only. No RPC path verbs (e.g. `/updateStatus`).

**Create success:** `201 Created` with `Location` pointing at the created resource.

---

## 2. Endpoint catalogue

| # | Method | URL | Purpose |
|---|--------|-----|---------|
| 1 | `POST` | `/api/v1/tickets` | Create ticket |
| 2 | `GET` | `/api/v1/tickets/{ticketId}` | Get ticket by id |
| 3 | `GET` | `/api/v1/tickets` | List / search / filter tickets |
| 4 | `PATCH` | `/api/v1/tickets/{ticketId}` | Update ticket fields (not status) |
| 5 | `PATCH` | `/api/v1/tickets/{ticketId}/status` | Dedicated status transition |
| 6 | `POST` | `/api/v1/tickets/{ticketId}/comments` | Add comment |
| 7 | `GET` | `/api/v1/tickets/{ticketId}/comments` | List comments for a ticket |

No endpoints for delete ticket, edit/delete comment, users, auth, attachments, tags, SLA, notifications, or audit history.

---

## Shared response shapes

### Ticket (`TicketResponse`)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Cannot reset password",
  "description": "Reset link returns 500",
  "priority": "HIGH",
  "assignee": "alex",
  "status": "OPEN",
  "createdAt": "2026-09-22T08:15:30.123Z",
  "updatedAt": "2026-09-22T08:15:30.123Z"
}
```

`assignee` is either a string or `null`.

### Comment (`CommentResponse`)

```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "ticketId": "550e8400-e29b-41d4-a716-446655440000",
  "body": "Customer confirmed the steps.",
  "createdAt": "2026-09-22T09:00:00.000Z"
}
```

**Get Ticket** returns ticket fields only (no embedded comments). Comments are retrieved via **List Comments**. This avoids duplicating the dedicated list operation. (Architecture noted embedding as a possible design; this contract chooses separation.)

---

## 3. Create Ticket

**`POST /api/v1/tickets`**

**Purpose:** Create a ticket. Server sets `status` to `OPEN`. Client must not send `status`, `id`, `createdAt`, or `updatedAt`.

### Path / query parameters

None.

### Request body

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `title` | string | yes | Non-blank after trim |
| `description` | string | yes | Non-blank after trim |
| `priority` | string | yes | `LOW` \| `MEDIUM` \| `HIGH` |
| `assignee` | string \| null | no | Optional string (not a user reference). Omit or `null` → stored as `null`. Non-blank values are trimmed before storage; blank or whitespace-only → `null`. No user entity or authentication. |

```json
{
  "title": "Cannot reset password",
  "description": "Reset link returns 500",
  "priority": "HIGH",
  "assignee": "alex"
}
```

### Response

| Status | Meaning | Body |
|--------|---------|------|
| `201` | Created | `TicketResponse`; `Location: /api/v1/tickets/{id}` |
| `400` | Malformed JSON, unknown/disallowed fields (e.g. `status`), or validation failure | Problem Details |
| `415` | Unsupported media type | Problem Details (optional if framework default) |

**Validation errors:** blank `title` / `description`; missing required fields; invalid `priority`.

**Domain errors:** none beyond validation (create always yields `OPEN`).

---

## 4. Get Ticket

**`GET /api/v1/tickets/{ticketId}`**

**Purpose:** Return one ticket by id (fields only; no comments array).

### Path parameters

| Name | Type | Required |
|------|------|----------|
| `ticketId` | UUID | yes |

### Query / body

None.

### Response

| Status | Meaning | Body |
|--------|---------|------|
| `200` | Found | `TicketResponse` |
| `400` | `ticketId` not a valid UUID | Problem Details |
| `404` | No ticket with that id | Problem Details |

---

## 5. List / Search / Filter Tickets

**`GET /api/v1/tickets`**

**Purpose:** List tickets, optionally filtered by keyword and/or a single status.

### Query parameters

| Name | Type | Required | Notes |
|------|------|----------|-------|
| `keyword` | string | no | Case-insensitive; matches if present in **title** or **description** (substring). Empty/omitted → no keyword constraint |
| `status` | string | no | Exactly one of `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED` |

### Search semantics

| Rule | Behaviour |
|------|-----------|
| Case | Matching is case-insensitive |
| Fields | `keyword` applies to `title` **and** `description` (OR within those fields) |
| Comments | Comment bodies are **not** searched |
| Status | Equality on ticket `status` when `status` is provided |
| Combined | When both are provided: keyword match **AND** status equality |
| Neither | Return all tickets (unpaginated; see §12) |

### Response

| Status | Meaning | Body |
|--------|---------|------|
| `200` | Success (including empty list) | JSON array of `TicketResponse` |
| `400` | `status` present but not a valid enum value | Problem Details |

**Design choice:** invalid `status` query → **`400`**, not an empty list.

**Keyword matching (API design choice):** case-insensitive substring/contains matching on `title` and `description`; when both `keyword` and `status` are provided, combine with **AND**.

**Sort order:** not specified by the assignment. **API design choice:** order by `createdAt` descending (newest first). Treat as a design choice, not a product requirement—changeable without changing domain rules.

**Example:** `GET /api/v1/tickets?keyword=password&status=OPEN`

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "title": "Cannot reset password",
    "description": "Reset link returns 500",
    "priority": "HIGH",
    "assignee": "alex",
    "status": "OPEN",
    "createdAt": "2026-09-22T08:15:30.123Z",
    "updatedAt": "2026-09-22T08:15:30.123Z"
  }
]
```

---

## 6. Update Ticket fields

**`PATCH /api/v1/tickets/{ticketId}`**

**Purpose:** Partially update allowed fields. **Must not** change `status`. Allowed when ticket is `CLOSED` or `CANCELLED`.

### Path parameters

| Name | Type | Required |
|------|------|----------|
| `ticketId` | UUID | yes |

### Request body

At least one allowed field must be present. Omitted fields remain unchanged.

| Field | Type | Notes |
|-------|------|-------|
| `title` | string | If present: non-blank after trim |
| `description` | string | If present: non-blank after trim |
| `priority` | string | If present: `LOW` \| `MEDIUM` \| `HIGH` |
| `assignee` | string \| null | Optional string (not a user reference). If present: `null` clears assignee; non-blank values are trimmed before storage; blank or whitespace-only → `null`. No user entity or authentication. |

**Disallowed in body:** `id`, `status`, `createdAt`, `updatedAt` (and any other unknown product fields). Sending them → **`400`** (unsupported / invalid request for this operation).

```json
{
  "title": "Cannot reset password (urgent)",
  "assignee": null
}
```

### Response

| Status | Meaning | Body |
|--------|---------|------|
| `200` | Updated | Full `TicketResponse` after update |
| `400` | Malformed JSON; invalid UUID; validation failure; disallowed fields present; empty patch (no allowed fields) | Problem Details |
| `404` | Ticket not found | Problem Details |

**Domain note:** status is unchanged. `updatedAt` is refreshed on success.

---

## 7. Dedicated Status Transition

**`PATCH /api/v1/tickets/{ticketId}/status`**

**Purpose:** Transition ticket status according to the closed matrix. Only legal edges succeed.

### Path parameters

| Name | Type | Required |
|------|------|----------|
| `ticketId` | UUID | yes |

### Request body

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `status` | string | yes | Target status; must be a legal next status from current |

```json
{
  "status": "IN_PROGRESS"
}
```

### Allowed transitions

| Current | Target |
|---------|--------|
| `OPEN` | `IN_PROGRESS` |
| `OPEN` | `CANCELLED` |
| `IN_PROGRESS` | `RESOLVED` |
| `IN_PROGRESS` | `CANCELLED` |
| `RESOLVED` | `CLOSED` |

All other pairs are invalid, including same-status (e.g. `OPEN` → `OPEN`).

### Response

| Status | Meaning | Body |
|--------|---------|------|
| `200` | Transition applied | Full `TicketResponse` with new `status` and refreshed `updatedAt` |
| `400` | Malformed JSON; invalid UUID; missing/invalid `status` enum value | Problem Details |
| `404` | Ticket not found | Problem Details |
| `409` | Illegal transition for current status | Problem Details; `detail` must include current status, attempted status, and that it is not allowed |

**Field columns** (`title`, `description`, `priority`, `assignee`) are unchanged by this operation.

---

## 8. Add Comment

**`POST /api/v1/tickets/{ticketId}/comments`**

**Purpose:** Append a comment to an existing ticket. Allowed for **any** status, including `CLOSED` and `CANCELLED`. Comments cannot be edited or deleted via API.

### Path parameters

| Name | Type | Required |
|------|------|----------|
| `ticketId` | UUID | yes |

### Request body

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `body` | string | yes | Non-blank after trim |

```json
{
  "body": "Customer confirmed the steps."
}
```

### Response

| Status | Meaning | Body |
|--------|---------|------|
| `201` | Created | `CommentResponse`; `Location: /api/v1/tickets/{ticketId}/comments` |
| `400` | Malformed JSON; invalid UUID; blank `body`; disallowed fields | Problem Details |
| `404` | Ticket not found | Problem Details |

Per data model default: adding a comment does **not** bump ticket `updatedAt`.

---

## 9. List Comments

**`GET /api/v1/tickets/{ticketId}/comments`**

**Purpose:** List all comments for a ticket.

### Path parameters

| Name | Type | Required |
|------|------|----------|
| `ticketId` | UUID | yes |

### Response

| Status | Meaning | Body |
|--------|---------|------|
| `200` | Success (including empty array) | JSON array of `CommentResponse` |
| `400` | Invalid UUID | Problem Details |
| `404` | Ticket not found | Problem Details |

**Sort order (API design choice):** `createdAt` ascending (oldest first).

```json
[
  {
    "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
    "ticketId": "550e8400-e29b-41d4-a716-446655440000",
    "body": "Customer confirmed the steps.",
    "createdAt": "2026-09-22T09:00:00.000Z"
  }
]
```

---

## 10. Request / response JSON examples

### Create → 201

**Request:** `POST /api/v1/tickets`

```json
{
  "title": "Login timeout",
  "description": "Session ends after one minute",
  "priority": "MEDIUM"
}
```

**Response:**

```json
{
  "id": "11111111-1111-1111-1111-111111111111",
  "title": "Login timeout",
  "description": "Session ends after one minute",
  "priority": "MEDIUM",
  "assignee": null,
  "status": "OPEN",
  "createdAt": "2026-09-22T10:00:00.000Z",
  "updatedAt": "2026-09-22T10:00:00.000Z"
}
```

### Illegal transition → 409

**Request:** `PATCH /api/v1/tickets/11111111-1111-1111-1111-111111111111/status`

```json
{
  "status": "CLOSED"
}
```

**Response** (ticket still `OPEN`):

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Transition from OPEN to CLOSED is not allowed.",
  "instance": "/api/v1/tickets/11111111-1111-1111-1111-111111111111/status",
  "currentStatus": "OPEN",
  "attemptedStatus": "CLOSED"
}
```

### Validation failure → 400

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed.",
  "instance": "/api/v1/tickets",
  "errors": [
    {
      "field": "title",
      "message": "must not be blank"
    },
    {
      "field": "priority",
      "message": "must be one of LOW, MEDIUM, HIGH"
    }
  ]
}
```

---

## 11. Error response format

One consistent Problem Details structure (`application/problem+json`):

| Field | Required | Meaning |
|-------|----------|---------|
| `type` | yes | URI identifying the problem class; `about:blank` is acceptable for this assignment |
| `title` | yes | Short summary |
| `status` | yes | HTTP status code |
| `detail` | yes | Human-readable explanation |
| `instance` | recommended | Request path that failed |
| `errors` | for validation | Array of `{ "field", "message" }` |
| `currentStatus` / `attemptedStatus` | for illegal transitions | Echo domain context |

Never include stack traces, SQL, or persistence internals.

### Error categories (no auth)

| Category | HTTP | When |
|----------|------|------|
| **Malformed request** | `400` | Invalid JSON, wrong types, invalid UUID in path |
| **Validation failure** | `400` | Blank fields, invalid enum, disallowed fields on an operation, empty field patch |
| **Resource not found** | `404` | Unknown ticket id (and thus nested comment routes for that ticket) |
| **Invalid status transition** | `409` | Target status illegal from current status (including same-status) |
| **Unsupported operation** | `405` | HTTP method not allowed on the resource (e.g. `DELETE /api/v1/tickets/{id}`, `PUT` if not defined) |
| **Unsupported operation (body)** | `400` | Attempt to change `status` (or other immutable fields) via field-update `PATCH` |

Do **not** invent `401` / `403` responses.

---

## 12. Pagination

The assignment does **not** require pagination.

**Decision:** **Omit pagination.** List endpoints return a JSON array of all matching resources.

Do not add `page`, `size`, `cursor`, or envelope wrappers unless a later product decision introduces them.

---

## 13. Idempotency considerations

No client `Idempotency-Key` mechanism is required.

Relevant natural behaviour only:

| Operation | Notes |
|-----------|--------|
| `POST` create ticket / comment | Not idempotent; each call creates a new resource |
| `PATCH` fields | Repeating the same patch yields the same field values; `updatedAt` may change on each successful write |
| `PATCH` status | Same-status retry is **not** treated as success; it is an **illegal transition** → `409` |

Do not invent broader idempotency product requirements.

---

## 14. Concurrency considerations for status transitions

No distributed locks, Redis, or messaging are introduced.

**Expected behaviour:**

1. Each transition runs in a backend transaction: load current ticket status → validate edge → persist new status (or fail).
2. If two clients race on the same ticket:
   - The database transaction isolation / row lock used by the implementation serializes the updates.
   - The first successful commit wins its transition.
   - The second sees the **updated** current status and either applies a still-legal edge or receives **`409`** if its attempted target is no longer legal.
3. Never silently ignore an illegal transition.
4. Do not require ETags/`If-Match` for this assignment (optional future enhancement; not part of this contract).

---

## 15. Search semantics (summary)

- **`keyword`:** case-insensitive substring match against **title** and **description** (match if either field contains the keyword).
- **Does not** search comment bodies.
- **`status`:** single enum value; invalid value → `400`.
- **Combined:** keyword constraint AND status constraint when both supplied.
- **No pagination** on results.

---

## 16. Assignment requirements vs project decisions vs API design choices

### Assignment requirements (behaviour)

- Create, list, view, update fields, change status, comments, keyword search, status filter
- Backend validation and meaningful errors
- Persistence of tickets/comments

### Project decisions (locked in architecture / data model)

- UUID ids; priority `LOW`/`MEDIUM`/`HIGH`; optional string assignee
- Create always `OPEN`; client cannot set initial status
- Closed five-edge transition matrix; dedicated status operation; field update cannot change status
- Field updates allowed on `CLOSED` / `CANCELLED`
- Comments append-only; allowed on any ticket status
- Search/filter rules as in data model
- No auth, users, roles, attachments, tags, SLA, notifications, audit, soft delete
- RFC 7807-style errors; `409` for illegal transitions (project API standards)

### API design choices (this contract)

| Choice | Decision |
|--------|----------|
| Base path | `/api/v1` |
| Status transition URL | `PATCH /api/v1/tickets/{ticketId}/status` with `{ "status": "..." }` |
| Field update | `PATCH /api/v1/tickets/{ticketId}` |
| Get ticket vs comments | Ticket GET without embedded comments; dedicated `GET .../comments` |
| Invalid status query | `400` |
| List ticket sort | `createdAt` descending |
| Comment list sort | `createdAt` ascending |
| Pagination | **Omitted** |
| Disallowed fields on field PATCH | `400` |
| Unsupported HTTP methods | `405` |
| Assignee write rule | Optional string; trim non-blank; blank/whitespace-only → `null`; no user entity or authentication |
| Unknown JSON properties | Rejected with `400` |
| Keyword search | Case-insensitive substring/contains on title and description; combinable with single status filter using AND |
| Concurrent transitions | Transactional check of current status; no distributed locking |

### Open decisions (not silently required)

| Topic | Notes |
|-------|--------|
| Exact `type` URIs for Problem Details | `about:blank` is acceptable; custom URIs optional |
| Whether whitespace-only `keyword` is ignored or matched literally | Recommend treat as no keyword (same as empty) |
| Maximum result size without pagination | Unbounded lists are acceptable for this assignment size; revisit only if volume becomes a real constraint |
| ETag / optimistic concurrency tokens | Out of scope unless later required |

---

## Explicitly out of this API

`DELETE` tickets; comment `PATCH`/`DELETE`; user/auth/role APIs; attachment upload; tags; SLA; notifications; audit/history APIs; bulk operations; webhooks.
