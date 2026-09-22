---
description: How to review product specifications for consistency
alwaysApply: false
---

# Review spec

When reviewing or editing files under `spec/`:

1. Treat locked decisions (five-edge state machine, create=`OPEN`, no client status on create, field PATCH ≠ status) as binding.
2. If two specs conflict, resolve toward locked decisions and record the correction; do not invent a sixth transition or auth.
3. Specs must describe the **implemented** system — do not claim features that do not exist.
4. Keep product requirements separate from implementation choices (Flyway, H2, Vite proxy).
5. Cross-check `requirements`, `architecture`, `data-model`, `api-contract`, `state-machine`, `ui-flow`, and `test-strategy` for the same enums, paths, and error codes.
6. Do not silently add pagination, ETag, users, or reopen paths “for completeness.”
