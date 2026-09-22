---
description: How to generate meaningful automated tests for this project
alwaysApply: false
---

# Generate tests

When adding or extending tests:

1. Follow `spec/test-strategy.md` and `.cursor/rules/testing.md`.
2. Cover the full status matrix: five allowed edges and all invalid pairs (domain unit + representative/API HTTP coverage).
3. Include create validation (blank title/description, invalid priority, client `status`), field PATCH cannot change status, comments on terminal tickets, search/filter AND semantics, invalid status filter → 400, 404/409 Problem Details.
4. Prefer domain and MockMvc integration tests over superficial controller-only stubs.
5. Use H2 test config already in `src/test/resources`; do not commit real DB passwords.
6. Do not invent product features in tests (auth, pagination, ETag).
7. Assert illegal transitions leave persisted status unchanged.
