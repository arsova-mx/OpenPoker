# Database and migrations

OpenPoker uses **PostgreSQL**. The schema is managed by **[Flyway](https://documentation.red-gate.com/flyway)** migrations in `backend/src/main/resources/db/migration`. Hibernate never creates or alters tables: it only validates that the entities match the schema (`spring.jpa.hibernate.ddl-auto=validate`). If they don't match, the backend refuses to start.

## Current migrations

| Version | File | What it does |
|---|---|---|
| V1 | `V1__baseline.sql` | Full schema as of the switch to Flyway, with readable names for every constraint and foreign key |
| V2 | `V2__foreign_key_indexes.sql` | Indexes on the foreign key columns used by every room event |

## Adding a schema change

1. Change the JPA entity.
2. Add a new file `V<next>__<short_description>.sql`, for example `V3__session_status.sql`. Use PostgreSQL SQL and lowercase `snake_case` names. Name constraints explicitly (`uk_<table>_<columns>`, `fk_<table>_<target>`, `idx_<table>_<columns>`) so future migrations can reference them.
3. Run `./mvnw verify`. The tests build the schema from the migrations (H2 in PostgreSQL mode) and validate it against the entities.
4. CI also starts the whole stack on real PostgreSQL (Docker Compose smoke test).

Rules:
- **Never edit a migration that is already on `main`**. Flyway stores its checksum, and databases that already applied it would refuse to start. Add a new migration instead.
- Keep each migration small and focused on one change.
- For data changes on large tables, prefer multiple steps (add nullable column, backfill, then add the constraint).

## Existing databases

Databases created before Flyway (with `ddl-auto=update`) are registered at V1 without running it (`spring.flyway.baseline-on-migrate=true`, `baseline-version=1`). They then receive V2 and later migrations. This assumes their schema matches V1. If a local development database drifted, the simplest fix is to drop it and let the backend recreate it.

## Local database

```bash
docker run -d --name openpoker-pg -p 5432:5432 \
  -e POSTGRES_DB=openpoker -e POSTGRES_USER=openpoker -e POSTGRES_PASSWORD=openpoker \
  postgres:17-alpine
```

Use `SPRING_PROFILES_ACTIVE=dev` to log the SQL that Hibernate executes.
