# Prompt History

Concise record of how this project was driven from requirements through implementation. Full chat transcripts live under `.specstory/history/` (do not delete prior sessions).

## Development sequence

Requirement → Specification → Plan / Tasks → Implementation → Testing → Review → Fix

---

## Sessions

### Requirements analysis & ambiguity resolution

- Clarified product scope, stack (Spring Boot + React/Vite + PostgreSQL), and non-goals (no auth, Kafka, Redis, microservices, attachments, SLA, pagination).
- SpecStory: `.specstory/history/2026-09-22_06-20-16Z-repository-file-visibility.md`

### Architecture

- Modular monolith layering (`api` / `application` / `domain` / `persistence`); backend owns the state machine.
- SpecStory: `…_07-59-17Z-support-ticket-architecture.md`, `…_08-02-48Z-ticket-system-architecture-spec.md`

### Data model, API contract, state machine, UI flow

- Locked five-edge matrix, create-as-OPEN, dedicated status operation, field PATCH must not change status, search/filter rules, Problem Details errors.
- SpecStory: `…_09-26-21Z-support-ticket-state-machine.md` and architecture/API sessions above.
- Specs: `spec/data-model.md`, `spec/api-contract.md`, `spec/state-machine.md`, `spec/ui-flow.md`

### Implementation

- Domain policy, Flyway schema, REST API, React portal UI, PostgreSQL runtime + H2 tests.
- SpecStory: `…_09-30-48Z-we-are-now-entering.md`, `…_11-02-45Z-support-ticket-management-demo.md`

### Single-port portal / final demo

- One runtime on **http://localhost:9000** serving React UI + Spring Boot API.
- Embedded Vite build into Spring Boot static resources; SPA fallback; relative `/api/v1` calls.
- SpecStory: `…_12-15-25Z-support-ticket-portal.md` and later final-implementation sessions under `.specstory/history/`

### Testing strategy

- Domain + API integration tests for create validation, full transition matrix, search/filter, comments, errors, SPA serving.
- Documented in `spec/test-strategy.md` and `docs/manual-test-cases.md`.

### Review & fixes

- AI mistakes captured in `docs/ai-review.md` and summarised in `docs/ai-review-findings.md` (ports, single-portal packaging, SPA/`/api` boundary, PostgreSQL search null binding, etc.).
- Spec/docs aligned to the implemented single-port system (`spec/requirements.md`, architecture/UI updates).

---

## Related docs

- Specs: `spec/` (all seven files)
- Manual demo: `docs/manual-test-cases.md`
- AI mistakes: `docs/ai-review-findings.md`, `docs/ai-review.md`
- Tooling notes: `docs/ai-tooling.md`
- Run instructions: `README.md`
