# Support Ticket Management System

AI-assisted, spec-driven support ticket portal.

Development followed:

Requirements → Specification → Plan / Tasks → **Implementation** → Testing → Review → Fix

Product specs live under `spec/`.

---

## Final demo (one application)

The evaluator runs **one** Spring Boot process. The React/Vite UI is built and packaged into that process.

| What | Value |
|------|--------|
| Application URL | **http://localhost:9000** |
| API base path | `/api/v1` (example: `http://localhost:9000/api/v1/tickets`) |
| Database | PostgreSQL (`support_tickets`) |

There is **no** separate production frontend server and **no** 8000-series ports for the final demo.

Optional: during UI development only, Vite can run on port `9001` with an `/api` proxy. The final demo does **not** use that.

---

## Prerequisites

- **Java 21**
- **Maven** 3.9+
- **Node.js** 20+ and **npm**
- **PostgreSQL** 14+

---

## Database setup

1. Create the database:

```sql
CREATE DATABASE support_tickets;
```

PowerShell example (enter your password when prompted):

```powershell
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" -U postgres -h localhost -c "CREATE DATABASE support_tickets;"
```

2. Set datasource environment variables. **Do not commit real passwords.**

| Variable | Purpose | Example |
|----------|---------|---------|
| `SPRING_DATASOURCE_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/support_tickets` |
| `SPRING_DATASOURCE_USERNAME` | DB user | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | DB password | *(your local password only)* |

PowerShell:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/support_tickets"
$env:SPRING_DATASOURCE_USERNAME = "postgres"
$env:SPRING_DATASOURCE_PASSWORD = "YOUR_LOCAL_PASSWORD"
```

See `.env.example` for placeholders. Optional: copy `backend/src/main/resources/application-local.yml.example` to gitignored `application-local.yml` and start with `--spring.profiles.active=local`.

---

## Build steps

### 1. Frontend production build

```powershell
cd frontend
npm install
npm run build
```

This writes assets to `frontend/dist/`. The SPA calls the API with **relative** paths such as `/api/v1/tickets` (no hardcoded localhost backend URL).

### 2. Backend package (embeds the frontend)

```powershell
cd backend
mvn -DskipTests package
```

When `frontend/dist/index.html` exists, Maven copies `frontend/dist` into the Spring Boot classpath (`static/`) so the UI is served from the same process on port **9000**.

---

## Run steps

With the same datasource environment variables set:

```powershell
cd backend
java -jar target/ticket-management-0.0.1-SNAPSHOT.jar
```

Or, after a frontend build:

```powershell
cd backend
mvn spring-boot:run
```

Prefer `npm run build` → `mvn -DskipTests package` → `java -jar …` for a reliable demo.

Open:

**http://localhost:9000**

Flyway applies migrations under `backend/src/main/resources/db/migration` on startup. Tickets and comments persist in PostgreSQL across restarts.

---

## Useful commands

| Goal | Command |
|------|---------|
| Backend tests | `cd backend` then `mvn test` |
| Backend package | `cd backend` then `mvn -DskipTests package` |
| Frontend install | `cd frontend` then `npm install` |
| Frontend build | `cd frontend` then `npm run build` |
| Optional UI-only Vite | `cd frontend` then `npm run dev` (proxies `/api` → `:9000`) |

### Database notes

- **Application:** PostgreSQL (`support_tickets`)
- **Tests:** H2 in-memory (`MODE=PostgreSQL`)
- **Flyway:** creates `ticket` and `comment` tables
- Hibernate `ddl-auto: validate` (no schema auto-create in the running app)

---

## Demo flow

1. Open **http://localhost:9000**
2. Create a ticket (status starts as **OPEN**; no status field on create)
3. View ticket details
4. Search by keyword and filter by status on **Tickets**
5. Edit title / description / priority / assignee (status unchanged)
6. Add a comment
7. Change status using only the offered next statuses
8. Valid transitions: `OPEN → IN_PROGRESS`, `IN_PROGRESS → RESOLVED`, `RESOLVED → CLOSED`, `OPEN → CANCELLED`, `IN_PROGRESS → CANCELLED`
9. Invalid transitions are rejected by the backend with **409** and a clear message

Full step-by-step checklist: **`docs/manual-test-cases.md`**

---

## Specs

- `spec/requirements.md`
- `spec/architecture.md`
- `spec/data-model.md`
- `spec/api-contract.md`
- `spec/state-machine.md`
- `spec/ui-flow.md`
- `spec/test-strategy.md`

## Docs

- Manual demo: `docs/manual-test-cases.md`
- Prompt history: `docs/prompt-history.md`
- AI review findings: `docs/ai-review-findings.md` (detail log: `docs/ai-review.md`)
- AI tooling notes: `docs/ai-tooling.md`
- SpecStory sessions: `.specstory/history/`
