---
description: Java 21 and Spring Boot conventions for the ticket backend
alwaysApply: true
---

# Java 21 + Spring Boot

Use Java 21 and Spring Boot. Prefer records, sealed types, and pattern matching where they make domain code clearer. Do not add other JVM languages or web frameworks.

## Layers

- Controllers stay thin: map HTTP, bind and validate the request DTO, call one service/use-case method, return a response DTO.
- Do not put ticket status machines, assignment rules, SLA, or persistence in controllers.
- Business rules live in the service/domain layer. The backend owns those rules; the UI must not define them.
- Repositories load and save only. No “is this transition allowed?” checks in query methods.

## DTOs and persistence

- HTTP boundaries use DTOs (records preferred). Never expose JPA entities, lazy collections, or persistence annotations on API types.
- Map entity ↔ DTO in the application layer, not as ad-hoc field copies scattered through the controller.

## Injection, transactions, exceptions

- Prefer constructor injection. Dependencies are `final`. Do not use field `@Autowired`.
- `@Transactional` belongs on the service/use-case, not the controller.
- Use meaningful exceptions (`TicketNotFoundException`, `IllegalTicketTransitionException`, validation failures). Map them to HTTP in one place (exception handler). Do not swallow errors or return 200 with an error body.
- Unknown ticket → not found. Illegal status transition → conflict/domain error, never a silent no-op.

## Specs

- Implement only behaviour that is specified. If code and a spec disagree, stop and align them; do not invent extra product rules in the implementation.
