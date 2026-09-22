---
description: REST, status codes, validation, and error shape for ticket APIs
alwaysApply: true
---

# API standards

REST, JSON, versioned resource paths (`/api/v1/tickets`, `/api/v1/tickets/{id}`). No RPC-style verbs in paths.

Controllers do not contain business logic. They validate the request DTO, call the service, and return a response DTO.

## Contracts

- Request and response bodies are DTOs. Do not serialize JPA entities.
- Breaking JSON field or status-code changes require a spec/docs update in the same change.

## HTTP status codes

| Situation | Status |
|---|---|
| GET success | 200 |
| POST create | 201 + `Location` |
| PUT/PATCH success | 200 (204 only if no body is specified) |
| DELETE success | 204 |
| Validation or malformed JSON | 400 |
| Unauthenticated | 401 |
| Authenticated but not allowed | 403 |
| Ticket or nested resource missing | 404 |
| Illegal status transition or conflicting update | 409 |

Do not use 500 for expected domain failures.

## Validation and errors

- Validate request DTOs with Jakarta Validation (`@Valid`). Cross-field and status-machine rules stay in the service.
- One error shape for all APIs (prefer RFC 7807: `type`, `title`, `status`, `detail`, `instance`). Validation failures include field + message.
- Illegal transition: 409; `detail` includes current status, attempted status, and that it is not allowed.
- Never put stack traces, SQL, or persistence internals in responses.
