# Test Strategy — Support Ticket Management System

How the Support Ticket Management System is verified. Source of truth: the assignment plus `spec/requirements.md`, `spec/architecture.md`, `spec/data-model.md`, `spec/api-contract.md`, `spec/state-machine.md`, and `spec/ui-flow.md`.

This document describes **what** to test and **where**. It does not claim automated test runs or fabricated results.

---

## 1. Goals

- Prove product behaviour, especially the closed status state machine
- Cover create/update/comment/search/filter and meaningful error responses
- Keep tests meaningful (domain + API/persistence), not controller-only smoke tests
- Support manual demonstration via `docs/manual-test-cases.md`

---

## 2. Test layers

| Layer | Location | Purpose |
|-------|----------|---------|
| **Domain unit** | `backend/src/test/java/.../domain/` | Transition matrix; create always `OPEN`; field update does not change status |
| **API + DB integration** | `backend/src/test/java/.../api/`, schema/SPA tests | HTTP contract, H2 persistence, Problem Details, SPA forwarding |
| **Manual** | `docs/manual-test-cases.md` | End-to-end portal demo on PostgreSQL at port 9000 |

Frontend automated UI tests are **not** required for this assignment. UI verification is manual against the running portal.

---

## 3. Required automated cases

### 3.1 Create

| Case | Expectation |
|------|-------------|
| Valid create | `201`, status `OPEN`, `Location` header, fields persisted |
| Missing / blank title | `400` |
| Missing / blank description | `400` |
| Invalid priority | `400` |
| Client provides `status` | `400`; no ticket from illegal payload |

### 3.2 State machine — valid

| Transition | Expectation |
|------------|-------------|
| `OPEN → IN_PROGRESS` | `200`; status updated |
| `IN_PROGRESS → RESOLVED` | `200` |
| `RESOLVED → CLOSED` | `200` |
| `OPEN → CANCELLED` | `200` |
| `IN_PROGRESS → CANCELLED` | `200` |

### 3.3 State machine — invalid

All of the following must return **`409`**, leave status unchanged, and include current/attempted status in the error where applicable:

`OPEN→OPEN`, `OPEN→RESOLVED`, `OPEN→CLOSED`,  
`IN_PROGRESS→OPEN`, `IN_PROGRESS→IN_PROGRESS`, `IN_PROGRESS→CLOSED`,  
`RESOLVED→OPEN`, `RESOLVED→IN_PROGRESS`, `RESOLVED→RESOLVED`, `RESOLVED→CANCELLED`,  
`CLOSED→OPEN`, `CLOSED→IN_PROGRESS`, `CLOSED→RESOLVED`, `CLOSED→CLOSED`, `CLOSED→CANCELLED`,  
`CANCELLED→OPEN`, `CANCELLED→IN_PROGRESS`, `CANCELLED→RESOLVED`, `CANCELLED→CLOSED`, `CANCELLED→CANCELLED`.

Domain unit tests cover the full matrix; API integration tests cover the same invalid pairs over HTTP.

### 3.4 Other API cases

| Case | Expectation |
|------|-------------|
| Get ticket | `200` with ticket fields |
| Ticket not found | `404` Problem Details |
| Update title / description / priority / assignee | `200`; status unchanged |
| Status via field `PATCH` | `400`; status unchanged |
| Add comment | `201` |
| Comment on `CLOSED` | `201` |
| Comment on `CANCELLED` | `201` |
| Search title | Match |
| Search description | Match |
| Case-insensitive search | Match regardless of case |
| Search + status filter | AND semantics |
| Invalid status filter | `400` |
| Meaningful `400` / `404` / `409` bodies | Problem Details shape |
| Persistence across requests | Create then get / list sees stored data |
| Schema migration | Flyway creates ticket/comment tables |
| SPA serving | Client routes forward to `index.html`; `/api/v1` remains JSON |
| No committed secrets | Config uses env placeholders; `.gitignore` excludes local secrets |

---

## 4. Tools and profiles

| Concern | Approach |
|---------|----------|
| Framework | JUnit 5, Spring Boot Test, MockMvc |
| Test DB | H2 in-memory (`MODE=PostgreSQL`) via `src/test/resources/application.yml` |
| App DB | PostgreSQL (manual / demo only; not required for automated suite) |

**Do not** treat H2-green search SQL as automatic proof of PostgreSQL-specific dialect quirks; keep repository queries portable where practical (`spec/data-model.md` §10).

---

## 5. Manual testing

Evaluator / demo flow is documented in **`docs/manual-test-cases.md`**.

That document covers: start app → open **http://localhost:9000** → create → list → detail → field updates → comment → search/filter → valid transitions → invalid transition error → cancel paths → restart and persistence.

---

## 6. Out of scope for this strategy

- Load / performance testing
- Security penetration testing (no auth product)
- Browser automation frameworks unless later added
- Fabricated CI pass/fail reports

---

## 7. Running tests (for maintainers)

```powershell
cd backend
mvn test
```

Automated tests are written and maintained in the repository. Whether they are executed in a given evaluation pass is up to the evaluator; this strategy does not substitute for the manual portal demo.
