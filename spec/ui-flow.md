# UI Flow — Support Ticket Management System

UI behaviour for the React + Vite SPA. Source of truth: the assignment plus `spec/architecture.md`, `spec/data-model.md`, `spec/api-contract.md`, and `spec/state-machine.md`.

This document is **not** visual design, component library choice, CSS, application source code, or a full test strategy. It defines screens, navigation, user actions, and how the UI talks to the API.

---

## 1. Purpose and scope

**Purpose:** Specify the minimal UI required to create, list, search/filter, view, update, comment on, and transition tickets, while showing meaningful API errors.

**In scope:** three pages (list, create, detail), status-transition UX aligned to the closed matrix, loading/empty/error behaviour, and API action mapping.

**Out of scope:** authentication, users/roles, notifications, attachments, dashboards, pagination, advanced filters, comment search, audit history, and any screen beyond the three below.

---

## 2. Pages and navigation

### 2.1 Routes (minimal)

| Route | Page | Purpose |
|-------|------|---------|
| `/` | Ticket List | List, search, filter; entry to create and detail |
| `/tickets/new` | Create Ticket | Create form |
| `/tickets/:ticketId` | Ticket Details | View/edit fields, comments, status actions |

No other product routes. Deep-linking to an unknown `ticketId` uses the detail page’s not-found behaviour (§7).

### 2.2 Navigation actions

| From | Action | To |
|------|--------|----|
| List | Activate a ticket row/item | Detail (`/tickets/:ticketId`) |
| List | “Create ticket” control | Create (`/tickets/new`) |
| Create | Cancel / “Back to list” | List (`/`) |
| Create | Successful create | Detail of the new ticket (`/tickets/{id}`) |
| Detail | “Back to list” | List (`/`) |

No login redirect. No role-based route guards.

---

## 3. Ticket List

**Route:** `/`

### 3.1 Display

Show each ticket’s identifying summary for scanning and navigation. Minimum fields:

- `title`
- `status`
- `priority`
- `assignee` (empty/cleared presentation when `null`)

Optional display of `createdAt` is allowed because the list response already includes it. Do not invent columns that are not on `TicketResponse`.

Each row/item navigates to Ticket Details.

### 3.2 Search and filter

| Control | Behaviour |
|---------|-----------|
| **Keyword** | Text input; sent as `keyword` on list fetch |
| **Status** | Single-select: empty (“All”) or exactly one of `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED` → `status` query |
| **Combined** | Both may be set; UI sends both params (API AND semantics) |
| **Clear** | Clear keyword and/or status and re-fetch |

Trigger strategy is a UI design choice: explicit Apply/Search, or change with a short debounce. Do not search comment bodies.

### 3.3 Other actions

- Control to navigate to Create Ticket.
- Re-fetching the list when returning from create/detail is acceptable; no push/WebSocket updates.

---

## 4. Create Ticket

**Route:** `/tickets/new`

### 4.1 Form fields

| Field | UI control | Required | Notes |
|-------|------------|----------|-------|
| Title | text | yes | Non-blank before submit (usability) |
| Description | textarea | yes | Non-blank before submit |
| Priority | select: `LOW` \| `MEDIUM` \| `HIGH` | yes | No other values |
| Assignee | text | no | Optional free-text; blank → omit or send `null` |
| **Status** | **none** | — | **Do not render a status field or picker** |

### 4.2 OPEN indication

Clearly state that a newly created ticket **starts as `OPEN`** (server-assigned). Example: “New tickets are created with status OPEN.” Informational only; the client must not send `status`.

### 4.3 Submit behaviour

1. Optional client-side blank checks on title/description; require a priority selection.
2. Call `POST /api/v1/tickets` with `title`, `description`, `priority`, and optional `assignee` only.
3. On `201`: navigate to `/tickets/{id}` using the created id from the response body (or `Location`).
4. On `400`: show Problem Details field errors and/or `detail` on the form; remain on Create.
5. Never send `id`, `status`, `createdAt`, or `updatedAt`.

---

## 5. Ticket Details

**Route:** `/tickets/:ticketId`

### 5.1 Load

On enter:

1. `GET /api/v1/tickets/{ticketId}` — ticket fields only (API contract does **not** embed comments).
2. `GET /api/v1/tickets/{ticketId}/comments` — comments.

If ticket GET returns `404`, show not-found (§7); skip or ignore comments fetch for that case.

### 5.2 Display

Show all ticket fields from `TicketResponse`: `id`, `title`, `description`, `priority`, `assignee`, `status`, `createdAt`, `updatedAt`.

Show comments oldest-first (`createdAt` ascending). Empty list → “No comments yet” (not an error).

### 5.3 Field update (not status)

Editable: `title`, `description`, `priority`, `assignee` only.

| Rule | Behaviour |
|------|-----------|
| Status | Read-only display of current status; **not** in the field-edit form |
| Terminal tickets | Field editing remains available for `CLOSED` and `CANCELLED` |
| Submit | `PATCH /api/v1/tickets/{ticketId}` with only changed allowed fields |
| Clear assignee | Send `"assignee": null` (or map blank to `null` per API) |
| Success | Replace displayed ticket with response; comments unchanged unless separately refreshed |
| Failure | Show errors (§7); keep last successful data visible |

Never include `status`, `id`, `createdAt`, or `updatedAt` in the field PATCH body.

### 5.4 Add comment

- Available for **any** status, including `CLOSED` and `CANCELLED`.
- Input: `body` only.
- Submit: `POST /api/v1/tickets/{ticketId}/comments`.
- On `201`: append returned comment (or re-fetch comments); clear input.
- No edit or delete comment UI.

---

## 6. Status Transition UI

Status actions are **separate** from field editing.

### 6.1 Valid next-status actions (UX only)

Show only legal next statuses as normal actions, derived from current `status`:

| Current | Actions shown |
|---------|---------------|
| `OPEN` | `IN_PROGRESS`, `CANCELLED` |
| `IN_PROGRESS` | `RESOLVED`, `CANCELLED` |
| `RESOLVED` | `CLOSED` |
| `CLOSED` | **none** |
| `CANCELLED` | **none** |

For terminal states, show current status with no transition controls (or a short note that no further transitions are available). Do not offer reopen, skip-ahead, or same-status actions as normal controls.

### 6.2 Request behaviour

Each action calls:

`PATCH /api/v1/tickets/{ticketId}/status` with `{ "status": "<target>" }`.

On `200`: update displayed ticket from the response (new `status`, refreshed `updatedAt`); leave field values otherwise as returned.

### 6.3 Backend remains source of truth

- Filtering buttons is **UX guidance only**.
- The UI must still handle and display a backend **`409`** if an invalid transition is attempted (race, stale UI, or any bypass).
- Do **not** treat hidden buttons as enforcement.
- Do not add assignee/priority guards before enabling a transition.

---

## 7. Error handling

Parse API errors as RFC 7807 Problem Details when present (`detail`, optional `errors[]`, optional `currentStatus` / `attemptedStatus`).

| Situation | UI behaviour |
|-----------|--------------|
| **`400` validation** | Show `detail`; map `errors[].field` + `message` next to the relevant form fields when possible (create, field update, comment) |
| **`404` ticket not found** | On detail (or any ticket-scoped action): dedicated not-found message; offer link back to list. Do not show an empty editable form as if the ticket exists |
| **`409` invalid status transition** | Show conflict message using `detail`, and prefer including current vs attempted status (`currentStatus` / `attemptedStatus` when present). Do not change displayed status until a successful transition |
| **Unexpected server errors** (`5xx` or unparseable error body) | Generic failure message (e.g. “Something went wrong. Please try again.”); do not show stack traces or raw HTML |
| **Network failures** | Distinct offline/unreachable message (e.g. “Unable to reach the server.”); keep local form input so the user can retry |

Do not invent `401` / `403` UI flows.

---

## 8. Loading and empty states

| Situation | Behaviour |
|-----------|-----------|
| Loading ticket list | Show a simple loading indicator; disable or ignore duplicate concurrent fetches if practical |
| No search/filter results | Empty list message (e.g. “No tickets match.”), not an error |
| Empty list with no filters | Empty message (e.g. “No tickets yet.”) plus path to Create |
| Loading ticket details | Loading indicator for ticket (and comments) until both succeed or ticket fails |
| Ticket not found | Not-found state (§7); no field/status/comment editors |
| Submitting create | Disable submit; show in-progress state; on failure re-enable |
| Submitting field update | Disable save; show in-progress; on success refresh ticket display |
| Submitting comment | Disable send; show in-progress; on success clear body |
| Submitting status transition | Disable transition actions while request in flight; on `409` re-enable and show conflict |

No skeleton-design system is required—simple text/spinner is enough.

---

## 9. API interaction map

| UI action | API operation |
|-----------|---------------|
| Load / refresh list (optional `keyword`, `status`) | `GET /api/v1/tickets` |
| Open create form | (none) |
| Submit create | `POST /api/v1/tickets` |
| Open detail | `GET /api/v1/tickets/{ticketId}` |
| Load comments on detail | `GET /api/v1/tickets/{ticketId}/comments` |
| Save field edits | `PATCH /api/v1/tickets/{ticketId}` |
| Transition status | `PATCH /api/v1/tickets/{ticketId}/status` |
| Add comment | `POST /api/v1/tickets/{ticketId}/comments` |

Dev wiring may use the Vite proxy to the backend (architecture). The UI must not hard-code secrets.

---

## 10. UI validation vs backend validation

| Layer | Role | Examples |
|-------|------|----------|
| **Client usability validation** | Convenience only; may block obviously empty submits | Blank title/description/comment body; priority must be one of the three options before create/update |
| **Authoritative backend validation** | Final say on field rules and rejected payloads | Non-blank after trim; valid enums; disallowed fields (`status` on create or field PATCH) → `400` with Problem Details |
| **Backend state-machine enforcement** | Final say on status edges | Only dedicated status PATCH; illegal edge → `409`; UI button filtering is not enforcement |

If client validation and backend disagree, **backend wins**. The UI must always surface backend errors for create, update, comment, and transition.

---

## 11. Explicitly out of scope

Do not add to the UI:

- Authentication, login, session, or user profiles
- User management, roles, or permissions
- Notifications, email, or toast-driven workflows beyond simple inline/page errors
- Attachments / file upload
- Dashboards, charts, or analytics
- Pagination or infinite scroll requirements
- Advanced filtering (multi-status, date ranges, assignee pickers from a user directory)
- Searching comment bodies
- Audit / status history timelines
- Comment edit/delete
- Ticket delete
- Extra screens (settings, admin, reports)

---

## Alignment notes

| Spec | How this UI flow aligns |
|------|-------------------------|
| Architecture | Three screens (list / create / detail); frontend presents and calls API; may hide illegal statuses as UX only |
| Data model | Priority and status enums; optional assignee string; comments append-only; field updates allowed on terminal statuses |
| API contract | Separate ticket GET and comments GET; dedicated status PATCH; field PATCH cannot change status; Problem Details errors |
| State machine | Same five edges for shown actions; terminal states have no outgoing UI actions; backend still rejects illegal transitions |

If this document and a locked spec disagree on routes-vs-endpoints mapping or the five edges, **stop and resolve explicitly**—do not invent endpoints or transitions in the UI layer.
