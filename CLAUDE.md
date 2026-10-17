# OpenPoker

Open-source, self-hostable, real-time planning poker (pre-release v0.3, heading to an MVP launch). The long-term
goal is an "agentic agility" framework where humans and AI agents estimate together: read
[docs/VISION.md](docs/VISION.md) before designing data models or APIs.

More specific guidance loads automatically when you work in a subdirectory:
[backend/CLAUDE.md](backend/CLAUDE.md) and [frontend/CLAUDE.md](frontend/CLAUDE.md).

## Repository map

| Path | What it is |
|---|---|
| `backend/` | Spring Boot 4, Java 21, Maven wrapper. REST under `/api`, STOMP over WebSocket at `/ws-native` |
| `frontend/` | React 18 + Vite 5 + TypeScript SPA, Tailwind 4, shadcn/ui, Zustand, `@stomp/stompjs` |
| `docs/api.md` | REST and STOMP contract. Update it in the same PR when you change an endpoint, topic or payload |
| `docs/database.md` | Flyway rules and migration list |
| `docs/deployment.md` | Hosting design for the public instance and for self-hosters |
| `docker-compose.yml` | PostgreSQL 17 + backend + nginx-served frontend (nginx proxies `/api` and `/ws-native`) |
| `scripts/smoke-test.sh` | End-to-end smoke test that CI runs against the Compose stack |

## Commands

```bash
# Backend (needs Java 21 and a PostgreSQL on localhost:5432, see README)
cd backend && ./mvnw verify                              # all tests (H2 in PostgreSQL mode)
cd backend && ./mvnw test -Dtest=GuestAccessIntegrationTest   # one class (Class#method for one test)
cd backend && JWT_SECRET="$(openssl rand -base64 48)" ./mvnw spring-boot:run

# Frontend
cd frontend && npm ci
cd frontend && npm run lint      # ESLint, --max-warnings 0: any warning fails CI
cd frontend && npm run build     # tsc -b (strict) + vite build
cd frontend && npm run dev       # http://localhost:3000, proxies /api and /ws-native to :8080

# Full stack, as CI does (needs POSTGRES_PASSWORD and JWT_SECRET in .env)
docker compose up -d --build --wait && ./scripts/smoke-test.sh http://localhost:3000
```

Before saying a change is done, run the checks for the side you touched (the `/verify` skill runs the CI set).
Report failures with their output; never describe a red build as passing.

## Invariants (do not break these)

- **PostgreSQL only.** The driver is `org.postgresql:postgresql`; SQL, native queries and migrations are
  PostgreSQL. Do not reintroduce MySQL (driver, `flyway-mysql`, port 3306, `root/root` defaults).
- **Flyway owns the schema.** `spring.jpa.hibernate.ddl-auto=validate` in every profile, including tests;
  `spring.flyway.enabled` stays on. Spring Boot 4 needs `spring-boot-starter-flyway` +
  `flyway-database-postgresql` (with `flyway-core` alone, migrations silently don't run). Never edit a migration
  that is already on `main`: add `V<next>__<desc>.sql`.
  See [docs/database.md](docs/database.md) and the `/db-migration` skill. A project hook blocks edits to merged
  migrations.
- **Secrets never enter git or chat.** `JWT_SECRET` and DB passwords come from the environment. Do not read `.env`
  files. `.env.example` is the documented template.
- **The backend is a stateful singleton.** The STOMP broker, `WebSocketSessionRegistry`, the rate limiter and the
  ticket timers live in memory. Run exactly one backend instance per database. Do not design features that assume
  horizontal scaling or scale-to-zero without first moving that state out (e.g. a broker relay).
- **Authorization is per session.** Every REST endpoint or STOMP handler that touches session data must check
  membership through `SessionAccessService` (details in backend/CLAUDE.md). New `/api/**` routes are authenticated
  by default; keep it that way.
- **Keep the agent roadmap open.** Keep vote rounds instead of deleting them, leave room for a participant type
  beyond `USER`/`GUEST` (`AGENT`) and for a vote `rationale`. Agent features themselves belong to epic #68.

## Workflow

- Branch from `main` (`feat/...`, `fix/...`, `docs/...`, `chore/...`). PR titles follow Conventional Commits.
- A PR links its issue (`Closes #123`) and must have green CI (backend tests on H2 and on PostgreSQL, frontend
  lint/build, Compose smoke test) before merge. Do not merge, or ask anyone to merge, a red PR.
- Keep PRs focused. If you find an unrelated bug, note it or open an issue instead of fixing it in passing.
- Issues use labels `area:backend|frontend|infra` and `priority:P0..P2`, grouped in milestones.

## Language

- The team talks in **Spanish**; answer in Spanish when the user writes in Spanish.
- Code identifiers, commit messages, PR titles, `README.md` and `docs/` are **English** (public audience).
- Existing code comments, log lines, error messages and UI strings are Spanish. Match the file you are editing.
  The UI has no i18n yet (planned), so new UI copy is Spanish for now.

## Known debt (do not copy these patterns)

Tracked in issues #61-#67: controllers that use repositories directly, inline `@Data` DTOs, plain-string error
bodies, duplicated broadcast code, dead pages (`frontend/src/pages/SessionPage.jsx`,
`frontend/src/api/services/websocket.js`, `backend/src/main/resources/templates/*.html`) and the unused
`frontend/nginx.conf` (the image uses `nginx.conf.template`). Follow the patterns described in the subdirectory
CLAUDE.md files instead.
