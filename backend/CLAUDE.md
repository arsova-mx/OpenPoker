# Backend (Spring Boot)

Base package `com.openpoker` (`src/main/java/com/openpoker/`). Packages: `controller`, `service`, `repository`,
`dto`, `entity`, `globalexception`, `security`, `config`, `ratelimit`, `sessioncodegenerator`, `model`.

## Conventions

- Constructor injection with Lombok `@RequiredArgsConstructor`; logging with `@Slf4j`.
- **Controllers** live under `/api/...`, take the caller as `@AuthenticationPrincipal String username`, validate
  with `@RequestBody @Valid`, and return `ResponseEntity` with DTOs (201 on create). Keep business logic and
  repository access in services. Some controllers call repositories directly (`TicketController`,
  `VoteController`, `CommentController`): that is debt (#63), not the pattern.
- **DTOs** are Java `record`s in `dto/` with Bean Validation annotations. Name them `*Request` / `*Response`.
  Never return entities from a controller.
- **Entities** use `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder` (not `@Data`). Ids are `UUID`
  stored as `VARCHAR(36)` (`@JdbcTypeCode(SqlTypes.VARCHAR)`, `@Column(length = 36)`) and assigned in
  `@PrePersist`. Any entity change needs a Flyway migration (see below).
- **Transactions** go on service methods (`@Transactional`, `readOnly = true` for queries).
- **Errors**: one exception class per domain error in `globalexception/`, each mapped explicitly in
  `GlobalExceptionHandler` (there is no catch-all). Status mapping in use: not found 404, already exists / wrong
  state 409, not allowed 403, invalid value 400, bad credentials 401, rate limited 429 with `Retry-After`.
  Bodies are currently plain strings; moving to `ProblemDetail` is #62.
- Messages, comments and logs are in Spanish; identifiers in English. No emoji in new code.

## Security checklist (REST)

`SecurityConfig` permits only `/api/auth/**`, `/api/card-decks/**`, `POST /api/sessions/*/guests` and the WebSocket
handshakes; every other `/api/**` route needs a valid user JWT. The principal is the username, with no roles.

For any endpoint that reads or writes session data:

1. Take `@AuthenticationPrincipal String username`.
2. Call `SessionAccessService.requireParticipant(sessionId, username)` or `requireParticipantOfTicket(ticketId,
   username)` (404 if the session doesn't exist, 403 if the caller isn't a participant).
3. Enforce host-only actions in the service layer.
4. Do not add a `permitAll` matcher without an explicit reason in the PR.

Guest tokens (`typ=guest`) are rejected on REST by `JwtService.validateToken()`; they only work on the STOMP
CONNECT.

## Security checklist (STOMP)

- Config: simple in-memory broker on `/topic` and `/queue`, app prefix `/app`, user prefix `/user`, endpoints
  `/ws-native` (used by the web app) and `/ws` (SockJS).
- `StompAuthChannelInterceptor` authenticates CONNECT (user JWT or guest token), allows SEND only to `/app/**`, and
  SUBSCRIBE only to `/user/queue/**` or `/topic/session/{code}/...` for members of that session. New topics must
  match `^/topic/session/([^/]+)/.+$` or subscriptions to them are denied.
- Handlers live in `WebSocketController`. Resolve the caller from `WebSocketSessionRegistry`
  (`getRequiredSessionInfo`), never from the payload. Host checks go in the service. Report failures with
  `publishError` instead of letting exceptions escape.
- Broadcast with `SimpMessagingTemplate` to `/topic/session/{code}/<topic>` and document the topic and payload in
  `docs/api.md`.

## Database and Flyway

- Migrations: `src/main/resources/db/migration/V<n>__<snake_case>.sql`, PostgreSQL syntax, explicit constraint names
  (`uk_<table>_<cols>`, `fk_<table>_<target>`, `idx_<table>_<cols>`).
- Never edit a migration that is on `main`; add a new one. Hibernate only validates (`ddl-auto=validate`).
- `java.sql.Timestamp` maps to `timestamp(6)`, `java.time.Instant` to `timestamp(6) with time zone`.
- `FlywayMigrationTest` and the context-loading tests fail if entities and migrations disagree. That is the guard:
  fix the migration or the entity, never relax `validate`, disable Flyway (also not per test class) or switch tests
  to `create-drop`.
- Spring Boot 4 needs `spring-boot-starter-flyway` + `flyway-database-postgresql`; with `flyway-core` alone the
  migrations silently don't run.
- Use the `/db-migration` skill for the full procedure.

## Tests

- Run: `./mvnw verify` (all), `./mvnw test -Dtest=ClassName` or `-Dtest=ClassName#method`.
- Profile `test` (`src/test/resources/application-test.properties`): H2 with `MODE=PostgreSQL;NON_KEYWORDS=VALUE`,
  schema built by Flyway, rate limiting off (`RateLimitIntegrationTest` turns it on).
- Integration tests are real HTTP and real WebSocket, no MockMvc:
  `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@ActiveProfiles("test")`, port from
  `@Value("${local.server.port}")`, `seeding.seedDecks()` (from `TestDataSeeding`) in `@BeforeEach`.
- Helpers in `src/test/java/com/openpoker/support/`: `ApiTestClient` (`registerUser`, `createSession`,
  `createTicket`, generic `call(...)`) and `StompTestClient` (`subscribe` returns a `BlockingQueue<String>`, `send`,
  `awaitRejection`). Close STOMP clients in `@AfterEach`.
- `@DisplayName` and assertion messages are in Spanish.
- All test classes share one database, and surefire runs them in filesystem order (different on macOS and Linux).
  A test must not depend on data or state left by another class, and must not hold seeded data in an uncommitted
  test transaction that the HTTP server thread needs to read. Check with
  `./mvnw test -Dsurefire.runOrder=reversealphabetical` (and `random`) when you add or change integration tests.
- Every security-relevant change needs a test that proves the forbidden case is rejected (see
  `security/*IntegrationTest`).

## Configuration

All runtime settings come from environment variables with local-dev defaults in `application.properties`
(`SPRING_DATASOURCE_*`, `JWT_SECRET`, `JWT_EXPIRATION_MS`, `ALLOWED_ORIGINS`, `APP_RATELIMIT_*`). Add new settings
the same way, document them in the README configuration table and in `.env.example`, and never commit real values.
`spring.jpa.show-sql` stays `false` in the default profile; the `dev` profile turns SQL logging on.
