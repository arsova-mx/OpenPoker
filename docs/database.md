# Database and migrations

OpenPoker uses **PostgreSQL**. The schema is managed by **[Flyway](https://documentation.red-gate.com/flyway)** migrations in `backend/src/main/resources/db/migration`. Hibernate never creates or alters tables: it only validates that the entities match the schema (`spring.jpa.hibernate.ddl-auto=validate`). If they don't match, the backend refuses to start.

## Current migrations

| Version | File | What it does |
|---|---|---|
| V1 | `V1__baseline.sql` | Full schema as of the switch to Flyway, with readable names for every constraint and foreign key |
| V2 | `V2__foreign_key_indexes.sql` | Indexes on the foreign key columns used by every room event |
| V3 | `V3__cierre_logico_y_rondas.sql` | Soft close and vote rounds (#40): session `status`/`closed_at`, participant `left_at`, ticket `created_at`/`position`/`current_round`/`finished_at`, `votes.round`, history indexes, and drops the unused `ticket.estimated_value` |
| V4 | `V4__add_github_metadata_to_ticket.sql` | GitHub Issues link on tickets (`external_source`, `external_id`, `external_url`, `external_repo`) |
| V5 | `V5__one_vote_per_participant_and_round.sql` | Drops V1's one-vote-per-ticket constraint, which blocked voting in a second round |
| V6 | `V6__instants_as_timestamptz.sql` | Stores the `Instant` columns from V3 as `timestamp with time zone` |

## Adding a schema change

1. Change the JPA entity.
2. Add a new file `V<next>__<short_description>.sql`, for example `V7__session_tags.sql`. Use PostgreSQL SQL and lowercase `snake_case` names. Name constraints explicitly (`uk_<table>_<columns>`, `fk_<table>_<target>`, `idx_<table>_<columns>`) so future migrations can reference them.
3. Run `./mvnw verify`. The tests build the schema from the migrations (H2 in PostgreSQL mode) and validate it against the entities.
4. Run the same suite on real PostgreSQL (next section) when the migration does more than add plain columns. CI also starts the whole stack on real PostgreSQL (Docker Compose smoke test).

Rules:
- **Never edit a migration that is already on `main`**. Flyway stores its checksum, and databases that already applied it would refuse to start. Add a new migration instead.
- Keep each migration small and focused on one change.
- For data changes on large tables, prefer multiple steps (add nullable column, backfill, then add the constraint).
- Map `java.time.Instant` to `timestamp(6) with time zone` and `java.sql.Timestamp` to `timestamp(6)`.
- Never switch `ddl-auto` to `update` or `create-drop`, and never disable Flyway, to get past an error. A failing migration or validation is the signal to fix the migration or the entity.

### Spring Boot 4 and Flyway

Since Spring Boot 4, Flyway's auto-configuration lives in its own module. The backend needs both
`spring-boot-starter-flyway` and `org.flywaydb:flyway-database-postgresql`. With only `flyway-core` on the classpath,
Flyway silently doesn't run, and Hibernate's validation then fails because the tables don't exist.

### Running the tests on real PostgreSQL

H2 in PostgreSQL mode is fast but not PostgreSQL: it accepts some type mismatches and syntax that PostgreSQL treats
differently. To run the whole suite on PostgreSQL 17:

```bash
docker run -d --name openpoker-pg-tests -p 55432:5432 \
  -e POSTGRES_DB=openpoker_tests -e POSTGRES_USER=openpoker -e POSTGRES_PASSWORD=openpoker postgres:17-alpine

cd backend && ./mvnw verify \
  -Dspring.datasource.url=jdbc:postgresql://localhost:55432/openpoker_tests \
  -Dspring.datasource.username=openpoker -Dspring.datasource.password=openpoker \
  -Dspring.datasource.driver-class-name=org.postgresql.Driver \
  -Dspring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

docker rm -f openpoker-pg-tests
```

## Existing databases

Databases created before Flyway (with `ddl-auto=update`) are registered at V1 without running it (`spring.flyway.baseline-on-migrate=true`, `baseline-version=1`). They then receive V2 and later migrations. This assumes their schema matches V1. If a local development database drifted, the simplest fix is to drop it and let the backend recreate it.

## Local database

```bash
docker run -d --name openpoker-pg -p 5432:5432 \
  -e POSTGRES_DB=openpoker -e POSTGRES_USER=openpoker -e POSTGRES_PASSWORD=openpoker \
  postgres:17-alpine
```

Use `SPRING_PROFILES_ACTIVE=dev` to log the SQL that Hibernate executes.
