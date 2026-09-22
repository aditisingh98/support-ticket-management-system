---
description: How to review application code against specs and layering
alwaysApply: false
---

# Review code

When reviewing backend or frontend changes:

1. Compare behaviour to `spec/requirements.md`, `spec/api-contract.md`, `spec/state-machine.md`, and `spec/data-model.md`.
2. Controllers must stay thin; status transitions belong in domain/application, not controllers or repositories.
3. Reject scope creep: auth, pagination, ETag, Redis, Kafka, attachments, SLA, audit, microservices.
4. Prefer relative `/api/v1` paths in the SPA; final demo is single-port `9000`.
5. Flag hardcoded secrets, permissive CORS, or committed credentials.
6. Illegal transitions must yield conflict/409 and leave state unchanged.
7. Suggest the smallest fix; do not redesign the API casually.
