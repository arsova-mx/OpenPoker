---
name: db-migration
description: Add or change OpenPoker's PostgreSQL schema safely with a new Flyway migration plus the matching JPA entity change. Use whenever an entity field, table, index or constraint changes, or when a test fails with a Flyway or schema-validation error.
---

# Schema change with Flyway

Rules (from `docs/database.md`): PostgreSQL only, Flyway owns the schema, Hibernate only validates, and a migration
that is already on `main` is never edited. A project hook blocks edits to merged migrations.

## Steps

1. **Find the next version.**
   ```bash
   ls backend/src/main/resources/db/migration/ && git ls-tree --name-only origin/main backend/src/main/resources/db/migration/
   ```
   Also check open PRs for a migration with the same number (`gh pr list` then `gh pr diff <n> --name-only`). Two
   branches that both add `V4__…` will conflict at runtime even if git merges them cleanly.

2. **Change the entity** in `backend/src/main/java/com/openpoker/entity/`.

3. **Write `V<next>__<snake_case_description>.sql`** in `backend/src/main/resources/db/migration/`:
   - PostgreSQL syntax, lowercase `snake_case`.
   - Name every constraint and index: `uk_<table>_<cols>`, `fk_<table>_<target>`, `idx_<table>_<cols>`.
   - Before adding a column, constraint or index, check that V1..V<n-1> don't already create it
     (`grep -n "<name>" backend/src/main/resources/db/migration/*.sql`).
   - New `NOT NULL` column on an existing table: give it a `DEFAULT`, or add it nullable, backfill, then set
     `NOT NULL`.
   - One focused change per migration. No MySQL syntax (`AUTO_INCREMENT`, `DROP INDEX x ON t`, backticks).
   - Keep the column types the entities expect: ids are `varchar(36)`; `java.sql.Timestamp` maps to `timestamp(6)`
     and `java.time.Instant` to `timestamp(6) with time zone`.
   - If a merged migration turns out to be wrong, fix it forward with a new migration (see V5 and V6).

4. **Run the guards.**
   ```bash
   cd backend && ./mvnw test -Dtest=FlywayMigrationTest && ./mvnw verify
   ```
   Tests build the schema from the migrations on H2 in PostgreSQL mode and validate it against the entities. If
   they fail, fix the migration or the entity. Never switch tests to `ddl-auto=create-drop`/`update` or disable
   Flyway.

5. **Check on real PostgreSQL** when the change uses anything H2 might accept differently (partial indexes, `jsonb`,
   enums, `ON CONFLICT`): run the Compose stack (`/verify`, step 4) or a local `postgres:17-alpine`.

6. **Document it**: add a row to the "Current migrations" table in `docs/database.md`, and update `docs/api.md` if
   the change is visible in the API.
