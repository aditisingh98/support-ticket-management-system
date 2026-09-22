---
description: Document real decisions; keep docs aligned with the implementation
alwaysApply: true
---

# Documentation

Document decisions and assumptions we have actually made (status model, API contracts, auth, ownership). Do not invent requirements, statuses, or endpoints to make the docs look complete.

## Where and what

- Durable decisions: `docs/decisions/` — short files with date, status, context, decision, consequences, and explicit assumptions.
- README points at those docs. Do not copy the same HTTP table into three places.

## Consistency

- If implementation and docs disagree, **code and tests win**; update the docs in the same change.
- When an important design decision changes (status set, transition matrix, error codes, resource paths), update the relevant decision doc and any API notes in that change.
- Chat history is not the source of truth. Specs, rules, and `docs/` in the repo are.
