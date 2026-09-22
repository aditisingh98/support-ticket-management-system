# AI Engineering Review

Records cases where an AI-generated suggestion was incorrect, unnecessary, or conflicted with project specifications, and what was done instead.

**Evaluator summary:** see `docs/ai-review-findings.md` for the concise required findings.

## Session: Implementation foundation (2026-09-22)

### 1. Spring Initializr Boot version pinning

- **What the AI suggested:** Generate the backend from `start.spring.io` with Spring Boot `3.4.10`.
- **Why it was incorrect:** The live Initializr metadata no longer offers that Boot line (current defaults are Spring Boot 4.x). Blindly pinning a removed Initializr version produced a `400 Bad Request` and would have blocked scaffolding.
- **What was changed:** Created the Maven/`pom.xml` and project layout manually, pinning a known Maven Central parent (`3.4.10`) appropriate for Java 21 + the assignment stack, instead of trusting Initializr’s default.
- **Justification:** `spec/architecture.md` requires Java 21 + Spring Boot but does not require Initializr; implement only what the specs need.

### 2. Avoid inventing length limits

- **What the AI suggested (common default):** Add arbitrary `@Size(max = …)` / `VARCHAR(n)` limits on title, description, and comment body.
- **Why it was incorrect/unnecessary:** `spec/data-model.md` explicitly sets the validation floor to non-blank only and prefers `TEXT` to avoid invented limits.
- **What was changed:** Used `TEXT` columns and non-blank validation only; no maximum lengths.
- **Justification:** `spec/data-model.md` §§5, 9, 11.

### 3. State machine placement

- **What the AI suggested (common default):** Encode transition checks in a controller or JPA `@PreUpdate` / repository helper.
- **Why it was incorrect:** Architecture and state-machine specs assign ownership to the domain layer; controllers and repositories must not own the matrix.
- **What was changed:** Implemented `TicketStatusTransitionPolicy` and `Ticket.transitionTo(...)` in `domain/`, with `IllegalTicketTransitionException`. Persistence entities only store state.
- **Justification:** `spec/architecture.md` §7, `spec/state-machine.md` §10, `.cursor/rules/java-springboot.md`.

### 4. TIMESTAMPTZ alias vs H2 tests

- **What the AI suggested:** Use PostgreSQL `TIMESTAMPTZ` in the Flyway migration exactly as in the illustrative schema.
- **Why it was incorrect for dual-DB tests:** H2 rejected `TIMESTAMPTZ` (`Unknown data type`) even with `MODE=PostgreSQL`, breaking the schema migration integration test.
- **What was changed:** Migration uses SQL-standard `TIMESTAMP WITH TIME ZONE` (PostgreSQL synonym of `TIMESTAMPTZ`) so application PostgreSQL and H2 tests share one migration.
- **Justification:** `spec/data-model.md` §9–§10 (prefer unambiguous instants; keep schema portable for H2 where practical).

### 5. Database password handling

- **What the AI might have suggested:** Hard-code a local PostgreSQL password in `application.yml` or invent a password to “make startup pass.”
- **Why it was incorrect:** Secrets must not be committed; guessing or inventing host credentials is unsafe and outside the product specs.
- **What was changed:** Required `SPRING_DATASOURCE_PASSWORD` via environment / gitignored local config; documented setup in README; did not commit credentials.
- **Justification:** `spec/architecture.md` §10; workspace secrets rules; implementation scope §10.

No further meaningful AI mistakes were identified in this session beyond the items above.

## Session: Backend REST API (2026-09-22)

### 6. Integration-test cleanup ignored comment FK

- **What the AI suggested:** Clear tickets between API tests with `ticketRepository.deleteAll()` only.
- **Why it was incorrect:** After comment tests insert rows, deleting tickets first violates the `comment.ticket_id` foreign key and broke subsequent tests.
- **What was changed:** Delete comments before tickets in `@BeforeEach`.
- **Justification:** `spec/data-model.md` FK from comment → ticket; tests must respect persistence constraints.

### 7. Ports updated to 9000/9001 (not inventing alternate defaults)

- **What a stale scaffold might keep:** Ports `8000`/`8001` from the previous slice.
- **Why that was wrong for this session:** The new local demo contract requires backend `9000` and frontend `9001` (and forbids `8080`/`3000`/`8000`/`8001`).
- **What was changed:** `application.yml`, Vite config/proxy, README, and scaffold copy updated to `9000`/`9001`.
- **Justification:** This session’s local-ports requirement; architecture still uses Vite proxy to the backend.

### 8. Null keyword search broke on PostgreSQL

- **What the AI suggested:** One JPQL query with optional `:keyword IS NULL OR LOWER(...) LIKE ...` for list/search/filter.
- **Why it was incorrect:** Against PostgreSQL, a null `:keyword` was bound as `bytea`, so `lower(...)` failed (`function lower(bytea) does not exist`) and list/search returned HTTP 500. H2 tests had still passed.
- **What was changed:** Split into four repository paths (all / status-only / keyword-only / keyword+status) so null keyword is never passed into `LOWER`/`LIKE`.
- **Justification:** `spec/data-model.md` §10 (H2 green ≠ PostgreSQL-safe); `spec/api-contract.md` list/search behaviour.

No further meaningful AI mistakes were identified in this session beyond the items above.

## Session: Final demo / single-app portal (2026-09-22)

### 9. Two-process demo treated as the final architecture

- **What the AI / prior slice suggested:** Keep a separate Vite runtime on port `9001` as the way to “run the app,” with Spring Boot only on `9000`.
- **Why it was incorrect:** The final demo contract requires **one** runtime application and **one** port (`http://localhost:9000`). A second production/runtime frontend server contradicts the evaluator workflow.
- **What was changed:** Vite production build is copied into Spring Boot `classpath:/static`; `SpaForwardController` serves SPA routes; README demo path is build frontend → package backend → open `:9000` only. Vite `:9001` remains optional for UI iteration, not the demo.
- **Engineering principle:** Prefer the deployment shape the assignment evaluates; do not leave a convenient local split as the production story.

### 10. Catch-all static resource handler intercepting `/api`

- **What the AI suggested:** Register a `/**` `ResourceHandler` with an SPA `index.html` fallback resolver (common Spring SPA snippet).
- **Why it was risky:** A catch-all static handler can compete with REST mappings and return HTML/`404` for `/api/**` instead of Problem Details JSON when resolution fails.
- **What was changed:** Removed the catch-all resource handler. SPA fallback is an explicit `@Controller` forward for `/`, `/dashboard`, and `/tickets/**` only; `/api/v1/**` stays on `TicketController`.
- **Engineering principle:** Keep API and UI routing boundaries explicit; never let a static fallback own `/api`.

### 11. Dashboard as a new backend endpoint

- **What a generated design might add:** Dedicated `/api/v1/stats` or aggregate endpoints for dashboard cards.
- **Why it was incorrect / out of scope:** Specs forbid inventing API surface for analytics; the demo asks for UI summaries only from existing ticket data.
- **What was changed:** Dashboard loads `GET /api/v1/tickets` and counts statuses in the UI. No new backend features.
- **Engineering principle:** Do not expand the API contract for presentation convenience; reuse existing operations.

No further meaningful AI mistakes were identified in this session beyond the items above.
