# AI Review Findings

Evidence that AI output was reviewed and not blindly accepted. Examples are taken from real work recorded in `docs/ai-review.md` and `.specstory/history/`. This file does **not** invent fake mistakes.

For the fuller session log, see `docs/ai-review.md`.

---

## Finding 1 — Two-process demo treated as the final architecture

| | |
|--|--|
| **What AI suggested** | Keep a separate Vite runtime (port `9001`) as the way to “run the app,” with Spring Boot only as the API on `9000`. |
| **Why wrong** | The final demo contract requires **one** runtime and **one** URL: `http://localhost:9000`. A second production frontend server fails the evaluator workflow. |
| **What changed** | Vite production build is copied into Spring Boot `classpath:/static`; `SpaForwardController` serves SPA routes; README demo path is build → package → open `:9000` only. Vite `:9001` remains optional for UI iteration. |
| **Why better** | Matches the assignment’s single-portal requirement; API stays same-origin via relative `/api/v1` paths. |

**Evidence:** `docs/ai-review.md` (Final demo session); `spec/architecture.md` single-port overview; `.specstory/history/` portal sessions.

---

## Finding 2 — Incorrect / unstable local port defaults (8000-series)

| | |
|--|--|
| **What AI suggested** | Earlier implementation guidance used ports such as `8000` / `8001` (and generic Spring defaults like `8080`). |
| **Why wrong** | Later locked demo rules forbid 8000-series ports for the final application; the portal must be **9000**. |
| **What changed** | `application.yml` uses `server.port: 9000`; Vite optional port is `9001`; README and docs updated accordingly. |
| **Why better** | Aligns configuration with the evaluator’s expected URL and avoids conflicting with the “no 8000-series” constraint. |

**Evidence:** `.specstory/history/2026-09-22_09-30-48Z-we-are-now-entering.md` (early 8000 guidance) vs later portal sessions requiring 9000; `docs/ai-review.md` item 7.

---

## Finding 3 — Catch-all static handler risking `/api` interception

| | |
|--|--|
| **What AI suggested** | Register a catch-all `/**` resource handler with SPA `index.html` fallback (common Spring SPA snippet). |
| **Why wrong** | A catch-all static resolver can compete with REST mappings and return HTML/`404` for `/api/**` instead of Problem Details JSON. |
| **What changed** | SPA fallback is an explicit `@Controller` forward for `/`, `/dashboard`, and `/tickets/**` only; `/api/v1/**` stays on `TicketController`. |
| **Why better** | Keeps API and UI routing boundaries explicit; illegal API calls still return meaningful JSON errors. |

**Evidence:** `docs/ai-review.md` item 10; `SpaForwardController.java`.

---

## Finding 4 — Null keyword search broke on PostgreSQL

| | |
|--|--|
| **What AI suggested** | One JPQL query with optional `:keyword IS NULL OR LOWER(...) LIKE ...`. |
| **Why wrong** | Against PostgreSQL, a null `:keyword` was bound as `bytea`, causing `lower(bytea)` failures and HTTP 500 on list/search. H2 tests could still pass. |
| **What changed** | Split into four repository paths (all / status-only / keyword-only / keyword+status) so null keyword is never passed into `LOWER`/`LIKE`. |
| **Why better** | Matches `spec/data-model.md` guidance that H2-green ≠ PostgreSQL-safe; list/search works on the real application database. |

**Evidence:** `docs/ai-review.md` item 8; `TicketRepository` / `TicketService` list paths.
