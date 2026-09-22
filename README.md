# Support Ticket Management System

AI-assisted, spec-driven support ticket application.

Product specs live under `spec/`. Implementation follows:

Requirements → Architecture → Data Model → API Contract → State Machine → UI Flow → **Implementation** → Testing → Review → Fix

## Local ports

| Process | URL |
|---------|-----|
| Backend (Spring Boot) | http://localhost:8000 |
| Frontend (Vite) | http://localhost:8001 |

API base path (when implemented): `http://localhost:8000/api/v1`

During frontend development, Vite proxies `/api` requests to `http://localhost:8000`.

---

## Run Locally

### Prerequisites

- **Java 21**
- **Maven** 3.9+ (or use the Maven Wrapper once generated: `./mvnw`)
- **Node.js** 20+ and **npm**
- **PostgreSQL** 14+ (application database)

### Configure PostgreSQL

1. Create a local database, for example:

```sql
CREATE DATABASE support_tickets;
```

2. Provide connection settings without committing secrets. Prefer environment variables:

| Variable | Purpose | Example (not a real secret) |
|----------|---------|-----------------------------|
| `SPRING_DATASOURCE_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/support_tickets` |
| `SPRING_DATASOURCE_USERNAME` | DB user | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | DB password | *(required for SCRAM auth; set locally; do not commit)* |

PowerShell example (replace with your local password; do not commit it):

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/support_tickets"
$env:SPRING_DATASOURCE_USERNAME = "postgres"
$env:SPRING_DATASOURCE_PASSWORD = "YOUR_LOCAL_PASSWORD"
```

Defaults in `backend/src/main/resources/application.yml` use localhost PostgreSQL on database `support_tickets` with username `postgres`. The password has no usable default — set `SPRING_DATASOURCE_PASSWORD` (or a gitignored local profile file) before starting the backend.

Optional local file approach:

1. Copy `backend/src/main/resources/application-local.yml.example` to `backend/src/main/resources/application-local.yml`
2. Fill in your password
3. Start with profile `local`:

```powershell
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

`application-local.yml` is gitignored and must never be committed.

### Start the backend

```powershell
cd backend
mvn spring-boot:run
```

Backend:

- URL: http://localhost:8000
- Database: PostgreSQL
- Schema: Flyway applies migrations from `backend/src/main/resources/db/migration` on startup (`ddl-auto: validate`)

### Start the frontend

```powershell
cd frontend
npm install
npm run dev
```

Frontend:

- URL: http://localhost:8001
- Vite proxy: `/api` → `http://localhost:8000`

### Useful commands

| Goal | Command |
|------|---------|
| Backend tests | `cd backend` then `mvn test` |
| Backend build | `cd backend` then `mvn -DskipTests package` |
| Frontend install | `cd frontend` then `npm install` |
| Frontend build | `cd frontend` then `npm run build` |

### Database and migrations

- **Application:** PostgreSQL
- **Tests:** H2 in-memory (`MODE=PostgreSQL`) via `backend/src/test/resources/application.yml`
- **Flyway:** enabled for the application and for tests; migration `V1__create_ticket_and_comment.sql` creates `ticket` and `comment`
- Hibernate does **not** auto-create schema in the running app (`validate` only)

### Current implementation slice

This session delivers:

- Backend domain model, persistence entities/repositories, Flyway schema, status state machine
- Domain/state-machine tests and a schema migration smoke test
- Frontend Vite + React + TypeScript scaffold on port 8001 with API proxy

Not yet implemented: REST controllers/endpoints, ticket UI screens, authentication.

## Specs

- `spec/architecture.md`
- `spec/data-model.md`
- `spec/api-contract.md`
- `spec/state-machine.md`
- `spec/ui-flow.md`

## AI engineering review

See `docs/ai-review.md` for corrections made when generated suggestions conflicted with specs or were unnecessary.
