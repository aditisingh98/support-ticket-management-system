---
description: Test ticket business behaviour, including every status transition
alwaysApply: true
---

# Testing

Test behaviour that matters to the product, not line coverage. A passing suite with untested status rules is not done.

## Unit vs integration

- Unit-test domain/service rules (status, assignment, validation) without starting the full Spring context when possible.
- Add integration tests for important HTTP and database flows (create ticket, fetch by id, persist a transition, unique/not-found).
- Do not treat controller smoke tests as a substitute for domain tests.

## Ticket status transitions

- Explicitly test **every allowed** transition and **every rejected** transition for the agreed status model.
- Drive transitions through the service/domain API. Do not set entity fields in the test and save to “prove” a transition.
- Cover actors once roles exist (who may transition). Cover required side effects (assignee, resolution note, timestamps) when they are part of the rule.

## Validation and errors

- Assert rejected input (blank subject, invalid body) and domain errors (unknown id, illegal transition).
- API tests assert HTTP status **and** the stable error shape. Domain tests assert the exception or result type.
- Name tests after the rule (`shouldRejectTransitionFromClosedToInProgress`), not `testUpdate1`.
