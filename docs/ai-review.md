# AI Engineering Review

Records cases where an AI-generated suggestion was incorrect, unnecessary, or conflicted with project specifications, and what was done instead.

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
