# OpenPoker API reference

OpenPoker exposes a **REST API** under `/api` and a **STOMP over WebSocket** API for real-time events.

> **Status:** pre-release. The API is not stable yet and may change before v1.0 (see [#63](https://github.com/arsova-mx/OpenPoker/issues/63)). An OpenAPI/Swagger spec is planned in [#56](https://github.com/arsova-mx/OpenPoker/issues/56).

## Authentication

- `POST /api/auth/login` and `POST /api/auth/register` return a JWT.
- Send it as `Authorization: Bearer <token>` on REST requests, and in the STOMP `CONNECT` frame.
- Tokens expire after `JWT_EXPIRATION_MS` (default 1 hour).
- **Guests** (no account) call `POST /api/sessions/{code}/guests` and get a **guest token**. It identifies that guest inside that single session over WebSocket and is rejected by the REST API.
- Session data (backlog, comments, history, real-time events) is only available to **participants** of that session.
- A request to an authenticated endpoint without a valid token gets `403 Forbidden` (there is no `401` with a `WWW-Authenticate` header yet). `401` is only used for wrong credentials on login.

## CORS

Browsers can only call the API and open the WebSocket from the origins listed in `ALLOWED_ORIGINS` (comma separated, patterns like `https://*.pages.dev` allowed), plus the backend's own origin. Requests from other origins get `403`. Requests without an `Origin` header (servers, curl) are not affected.

## Rate limits

Public endpoints that can be abused are limited per client IP. Each limit is a token bucket: `capacity` attempts in a row, recovered gradually over `period`.

| Endpoint | Default | Property |
|---|---|---|
| `POST /api/auth/login` | 10 per minute per IP | `app.rate-limit.login.*` |
| `POST /api/auth/register` | 5 per 10 minutes per IP | `app.rate-limit.register.*` |
| `POST /api/sessions/{code}/guests` | 30 per minute per IP | `app.rate-limit.guest-join.*` |
| Failed logins for the same account, from any IP | 5 per 5 minutes (a successful login resets it) | `app.rate-limit.login-failures.*` |

When a limit is hit the API answers `429 Too Many Requests` with a `Retry-After` header (seconds) and `{ message, retryAfterSeconds }`. Limits can be tuned with environment variables (for example `APP_RATELIMIT_LOGIN_CAPACITY=20`) or disabled with `APP_RATELIMIT_ENABLED=false`.

The client IP is taken from `X-Forwarded-For` when the request comes from an internal proxy (the frontend's nginx). **Behind Cloudflare**, configure nginx to use the real client IP (`real_ip_header CF-Connecting-IP;` plus `set_real_ip_from` with Cloudflare's IP ranges). Otherwise every user is seen with Cloudflare's IPs and users would share limits.

The counters live in the memory of each backend instance.

## REST endpoints

| Method | Path | Auth | Body / params | Description |
|---|---|---|---|---|
| `POST` | `/api/auth/register` | Public | `{ username, email, password }` | Create an account. Returns `201` with `{ token, id, username }` |
| `POST` | `/api/auth/login` | Public | `{ username, password }` | Returns `{ token, id, username }` |
| `GET` | `/api/card-decks` | Public | | List estimation decks (Fibonacci, T-Shirt, Dot Voting) with their cards |
| `GET` | `/api/card-decks/{seriesType}` | Public | `FIBONACCI` \| `T_SHIRT` \| `DOT_VOTING` | One deck |
| `POST` | `/api/sessions` | JWT | `{ name }` | Create a session with the default deck |
| `POST` | `/api/sessions/{deckId}` | JWT | `{ name }` | Create a session with a specific deck |
| `GET` | `/api/sessions/{code}` | JWT | | Session details by invite code |
| `POST` | `/api/sessions/{code}/join` | JWT | | Join a session as the authenticated user (idempotent) |
| `POST` | `/api/sessions/{code}/guests` | Public | `{ guestName }` (2-30 chars) | Join as a guest. Returns `201` with `{ session, participantId, displayName, guestToken }`. `409` if the name is already used in the session or by a registered user |
| `GET` | `/api/sessions` | JWT | | Same list as `GET /api/card-decks`. Kept for compatibility; use `/api/card-decks` |
| `POST` | `/api/tickets` | JWT (host) | `{ title, description, gameSessionId }` | Add a ticket to the session backlog. Returns a [ticket](#ticket) |
| `GET` | `/api/tickets/session/{sessionId}` | JWT (participant) | | Session backlog: a list of [tickets](#ticket) |
| `PATCH` | `/api/tickets/{ticketId}/status` | JWT (host) | `?newStatus=WAITING\|VOTING\|REVEALED\|FINISHED` and, with `FINISHED`, optional `&estimatedCardId=<card id>` | Change a ticket's status. `FINISHED` records `finishedAt` and the final estimate. Returns the [ticket](#ticket) |
| `POST` | `/api/sessions/{code}/votes` | JWT (participant) | `?ticketId=` + `{ cardValue: "<card id>" }` | Cast your vote for the current round, or change it. `cardValue` is the **card UUID**. Returns `{ voteId, username, cardValue, votedAt }`. The ticket must be in `VOTING` (`409` otherwise) |
| `GET` | `/api/sessions/{code}/votes` | JWT (participant) | `?ticketId=` | Votes of the current round: `{ sessionCode, votes: [{ voteId, username, cardValue, votedAt }], revealed }`. Before the reveal every `cardValue` is `"*"` |
| `POST` | `/api/sessions/{code}/votes/reveal` | JWT (host) | `?ticketId=` | Reveal the current round. Returns the [results](#results) |
| `POST` | `/api/sessions/{code}/votes/reset` | JWT (host) | `?ticketId=` | Start a new voting round. Previous rounds are kept. `204` |
| `GET` | `/api/tickets/{ticketId}/comments` | JWT (participant) | | Ticket discussion, oldest first: `[{ id, username, content, createdAt }]` |
| `POST` | `/api/tickets/{ticketId}/comments` | JWT (participant) | `{ content }` | Add a comment |
| `GET` | `/api/users/me` | JWT | | Your full profile (`id, username, email, role, companyName, phoneNumber`) |
| `PATCH` | `/api/users/profile` | JWT | `{ companyName?, phoneNumber? }` | Update your profile |
| `GET` | `/api/users/{id}` | JWT | | Public profile of another user: `{ id, username }` only |
| `GET` | `/api/history/sessions` | JWT | `?filter=ALL\|HOST\|PARTICIPANT` (default `ALL`), `?page=0&size=10&sort=createdAt,desc` | [Session history](#session-history) of the caller |
| `GET` | `/api/history/sessions/{sessionId}/tickets` | JWT (participant) | | Tickets of a past or current session with their outcome |
| `POST` | `/api/integrations/github/issues/query` | JWT | `{ repo, personalAccessToken? }` | List open issues of a GitHub repository. See [GitHub integration](#github-integration) |
| `POST` | `/api/integrations/github/import` | JWT (host) | `{ sessionId, repo, personalAccessToken?, issues: [...] }` | Add the selected issues to the backlog |
| `POST` | `/api/integrations/github/tickets/{ticketId}/export` | JWT (host) | `{ personalAccessToken, customComment? }` | Post the final estimate as a comment on the issue |

### Ticket

```json
{
  "id": "uuid",
  "title": "Exportar estimaciones",
  "description": "…",
  "gameSessionId": "uuid",
  "status": "WAITING | VOTING | REVEALED | FINISHED",
  "currentRound": 1,
  "externalSource": "GITHUB | null"
}
```

`currentRound` starts at 1 and goes up by one each time the host resets the votes. Votes are stored per round, so
earlier rounds are never lost.

### Results

```json
{
  "sessionCode": "UKWURN",
  "votes": [{ "voteId": "uuid", "username": "ana", "cardValue": "8", "votedAt": "2026-10-17T04:33:43.053Z" }],
  "revealed": true,
  "suggestedAverage": 8.0,
  "suggestedCardValue": "8",
  "statistics": { "average": 8.0, "consensusPercentage": 100.0, "isFullConsensus": true, "outlierVoteIds": [] }
}
```

### Session history

Sessions are never deleted. A session stays `ACTIVE` until it is closed, and participants who leave are marked
instead of removed, so the history keeps every session the user hosted or joined.

`GET /api/history/sessions` returns a Spring Data page:

```json
{
  "content": [{
    "sessionId": "uuid",
    "sessionCode": "UKWURN",
    "name": "Refinamiento",
    "status": "ACTIVE | FINISHED",
    "createdAt": "2026-10-17T04:33:42.778Z",
    "closedAt": null,
    "host": true,
    "totalTickets": 3,
    "estimatedTickets": 1
  }],
  "number": 0, "size": 10, "numberOfElements": 1, "totalElements": 1, "totalPages": 1,
  "first": true, "last": true, "empty": false,
  "pageable": { "...": "…" }, "sort": { "...": "…" }
}
```

`host` tells whether the caller hosted that session. The page wrapper is Spring Data's default serialization and
may change to a smaller `{ content, page: { size, number, totalElements, totalPages } }` shape before v1.0.

`GET /api/history/sessions/{sessionId}/tickets` returns
`[{ id, title, description, status, currentRound, finishedAt, estimatedValue }]`, where `estimatedValue` is the
value of the final card (`null` if none was chosen). Non-participants get `403`.

### GitHub integration

The host can bring GitHub issues into the backlog and, once a ticket is estimated, post the result back to the
issue. The backend calls `https://api.github.com` on the user's behalf.

- **Personal access token (PAT).** Sent in the request body on each call (never in the URL), used for that single
  call to GitHub and **not stored**. It's optional to list or import issues of a public repository and required to
  export. For export it needs permission to comment on issues of that repository.
- **`POST /issues/query`** → `[{ id, number, title, body, html_url, state, pull_request }]`, the repository's open
  issues. Any authenticated user can call it. `repo` is `owner/name`.
- **`POST /import`** (host of `sessionId`) → the created [tickets](#ticket). Each issue becomes a `WAITING` ticket
  titled `#<number> <title>`, with a summary of the issue body as description, appended at the end of the backlog,
  and linked to the issue (`externalSource: "GITHUB"`). The server uses the issue data sent by the client and does
  not fetch it again. Importing the same issue twice creates two tickets. Participants are notified on
  `/topic/session/{code}/tickets-imported`.
- **`POST /tickets/{ticketId}/export`** (host) → `{ message }`. Only for `FINISHED` tickets imported from GitHub
  (`400` otherwise). Posts `customComment` if present, or a default comment with the final estimate and the number
  of voting rounds.

## WebSocket (STOMP)

### Endpoints
| Endpoint | Transport |
|---|---|
| `/ws-native` | Native WebSocket. The web client uses this one. |
| `/ws` | SockJS fallback |

Send `Authorization: Bearer <token>` in the `CONNECT` headers: a user JWT, or a guest token from `POST /api/sessions/{code}/guests`. An invalid or expired token is rejected, and `SEND`/`SUBSCRIBE` frames are rejected once the token expires.

**Authorization rules** (a rejected frame closes the connection with a STOMP `ERROR`):
- Clients can only `SEND` to `/app/**`. Publishing to `/topic/**` is not allowed.
- `SUBSCRIBE` to `/topic/session/{code}/**` requires being a participant of that session: join it via REST first (users) or connect with a guest token for that session (guests).
- `SUBSCRIBE` to `/user/queue/errors` is always allowed; it only receives this connection's errors.

### Client → server (`/app/...`)
All payloads are JSON objects with string values.

The identity of every action comes from the `CONNECT` token and the room joined with `session.join`, never from
the payload. Send `session.join` first; the other destinations fail until the connection has joined a room.

| Destination | Payload | Who | Effect |
|---|---|---|---|
| `/app/session.join` | `{ inviteCode, ticketId? }` | User or guest | Joins the room (or rejoins it) and broadcasts `participants`, `state` and, with `ticketId`, `vote-status`. Fails if the session is closed |
| `/app/session.leave` | `{ ticketId? }` | Participant (not the host) | Leaves the room. The participant is marked as left, not deleted |
| `/app/session.vote` | `{ ticketId, cardValue }` | Participant | Casts or changes your vote in the current round (`cardValue` = card UUID) |
| `/app/session.reveal` | `{ ticketId }` | Host | Reveals the current round |
| `/app/session.reset-votes` | `{ ticketId }` | Host | Starts a new round. Previous rounds are kept |
| `/app/session.finish` | `{ ticketId }` | Host | Marks the **ticket** (not the session) as finished |
| `/app/session.set-timer` | `{ ticketId, durationSeconds? }` | Host | Starts the round timer, or clears it without `durationSeconds` |

### Server → clients (`/topic/session/{code}/...`)
| Topic | Payload | Sent when |
|---|---|---|
| `participants` | `[{ participantId, displayName, role, isGuest }]`, `role` = `HOST` \| `VOTER` | Join, leave, disconnect |
| `state` | Session details (`id, sessionCode, name, hostUsername, participantCount, createdAt, deckId, seriesType`) | Most state changes |
| `vote-status` | `{ "<participantId>": true \| false, ... }`: who has voted in the current round, without values. `{}` right after a reset | Join, vote, reveal, reset |
| `votes` | The [results](#results) of the round | Reveal |
| `votes` | Results with `votes: []` and `revealed: false`, and also `{ action: "VOTES_RESET", ticketId, newRound }` | Reset |
| `ticket-updated` | A [ticket](#ticket) | Ticket created, status change, finish, reset |
| `tickets-imported` | `{ message, count }` | Issues imported from GitHub. Reload the backlog |
| `timer` | `{ durationSeconds, timerExpiresAt, isExpired }` | Timer set, cleared, expired |

### Private queue (`/user/queue/...`)
| Destination | Payload | Sent when |
|---|---|---|
| `/user/queue/errors` | `{ action, code, message }` with `code` in `BAD_REQUEST`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`, `INTERNAL_ERROR` | One of **your** WebSocket actions failed. `action` is the destination without `/app/` (`ticket.finish` for `session.finish`). Other participants never see it. Unexpected errors carry a generic message |

### Known limitations
These are tracked and will change the contract:
- Sessions are identified by `code` in some routes and by `sessionId` in others ([#63](https://github.com/arsova-mx/OpenPoker/issues/63)).
- **There is no endpoint to close a session yet.** The history shows every session as `ACTIVE`, and `closedAt` is
  always `null` (#40).
- Participants who left still appear in `participants` and in `vote-status`, and keep access to the session data.
- REST errors are plain strings or ad-hoc JSON, not `ProblemDetail` ([#62](https://github.com/arsova-mx/OpenPoker/issues/62)).
- The web client still publishes to `/app/session.ticket-update`, which has no handler; ticket changes go through
  `PATCH /api/tickets/{ticketId}/status`.
