---
name: verify
description: Run the same checks as OpenPoker's CI (backend Maven tests, frontend lint and type-checked build, and optionally the Docker Compose smoke test) and report the real results. Use before opening or updating a pull request, after a change that touches backend/ or frontend/, or when asked to "verify", "run the checks" or "is CI going to pass".
---

# Verify like CI

CI (`.github/workflows/ci.yml`) runs three jobs. Reproduce the ones that cover what changed, from the repo root.

## 1. Decide the scope

```bash
git diff --name-only origin/main...HEAD; git status --short
```

- Anything under `backend/` → step 2.
- Anything under `frontend/` → step 3.
- `docker-compose.yml`, a `Dockerfile`, `nginx.conf.template`, `scripts/`, migrations, security or WebSocket
  config → step 4 too.
- If unsure, run everything.

## 2. Backend

```bash
cd backend && ./mvnw -B -ntp clean verify
```

On failure, find the first real cause (`grep -E "ERROR|Caused by|FAIL" ` over the output) before changing code.
`Migration V… failed` or `Schema-validation:` means a migration and an entity disagree: fix the migration or the
entity; never weaken `ddl-auto=validate` or disable Flyway to make tests pass.

If tests pass locally but fail in CI (or the reverse), suspect test class order before anything else: run
`./mvnw test -Dsurefire.runOrder=reversealphabetical` and `-Dsurefire.runOrder=random`.

CI also runs the same suite on PostgreSQL 17 (job "Backend tests on PostgreSQL"). Do it locally when the change
touches migrations, entities, repositories or native queries (needs Docker):

```bash
docker run -d --name openpoker-pg-tests -p 55432:5432 \
  -e POSTGRES_DB=openpoker_tests -e POSTGRES_USER=openpoker -e POSTGRES_PASSWORD=openpoker postgres:17-alpine
cd backend && ./mvnw -B -ntp verify \
  -Dspring.datasource.url=jdbc:postgresql://localhost:55432/openpoker_tests \
  -Dspring.datasource.username=openpoker -Dspring.datasource.password=openpoker \
  -Dspring.datasource.driver-class-name=org.postgresql.Driver \
  -Dspring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
docker rm -f openpoker-pg-tests
```

## 3. Frontend

```bash
cd frontend && npm ci && npm run lint && npm run build
```

`--max-warnings 0` means a warning fails the job.

## 4. Compose smoke test (needs Docker)

```bash
export POSTGRES_PASSWORD=ci-password JWT_SECRET="$(openssl rand -base64 48)"
docker compose up -d --build --wait --wait-timeout 300 && ./scripts/smoke-test.sh http://localhost:3000
docker compose logs --no-color | tail -100   # only if it failed
docker compose down -v
```

`down -v` deletes the stack's database volume. Before running it, check that the user isn't relying on data in
that local stack; if they might be, ask first.

## 5. Report

Give a short table: check, result (pass/fail/skipped and why), and for failures the key error lines. Never call a
check passing if you didn't run it or it failed.
