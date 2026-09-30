# OpenPoker API reference

OpenPoker exposes a **REST API** under `/api` and a **STOMP over WebSocket** API for real-time events.

> **Status:** pre-release. The API is not stable yet and may change before v1.0 (see [#63](https://github.com/arsova-mx/OpenPoker/issues/63)). An OpenAPI/Swagger spec is planned in [#56](https://github.com/arsova-mx/OpenPoker/issues/56).

## Authentication

- `POST /api/auth/login` and `POST /api/auth/register` return a JWT.
- Send it as `Authorization: Bearer <token>` on REST requests, and in the STOMP `CONNECT` frame.
- Tokens expire after `JWT_EXPIRATION_MS` (default 1 hour).
- **Guests** (no account) call `POST /api/sessions/{code}/guests` and get a **guest token**. It identifies that guest inside that single session over WebSocket and is rejected by the REST API.
- Session data (backlog, comments, real-time events) is only available to **participants** of that session.

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
| `POST` | `/api/tickets` | JWT (host) | `{ title, description, gameSessionId }` | Add a ticket to the session backlog |
| `GET` | `/api/tickets/session/{sessionId}` | JWT (participant) | | Session backlog |
| `PATCH` | `/api/tickets/{ticketId}/status` | JWT (host) | `?newStatus=WAITING\|VOTING\|REVEALED\|FINISHED` | Change a ticket's status |
| `POST` | `/api/sessions/{code}/votes` | JWT | `?ticketId=` + `{ cardValue: "<card id>" }` | Cast or update a vote. `cardValue` is the **card UUID** |
| `GET` | `/api/sessions/{code}/votes` | JWT | `?ticketId=` | Votes for a ticket (masked until revealed) |
| `POST` | `/api/sessions/{code}/votes/reveal` | JWT (host) | `?ticketId=` | Reveal votes and compute statistics |
| `POST` | `/api/sessions/{code}/votes/reset` | JWT (host) | `?ticketId=` | Start a new voting round |
| `GET` | `/api/tickets/{ticketId}/comments` | JWT (participant) | | Ticket discussion, oldest first |
| `POST` | `/api/tickets/{ticketId}/comments` | JWT (participant) | `{ content }` | Add a comment |
| `GET` | `/api/users/me` | JWT | | Your full profile (`id, username, email, role, companyName, phoneNumber`) |
| `PATCH` | `/api/users/profile` | JWT | `{ companyName?, phoneNumber? }` | Update your profile |
| `GET` | `/api/users/{id}` | JWT | | Public profile of another user: `{ id, username }` only |

Planned: `GET /api/users/me/sessions` and `GET /api/sessions/{code}/summary` for session history ([#40](https://github.com/arsova-mx/OpenPoker/issues/40)).

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

| Destination | Payload | Who | Effect |
|---|---|---|---|
| `/app/session.join` | `{ inviteCode, ticketId? }` | User or guest | Joins the room and broadcasts participants and state. The identity comes from the `CONNECT` token |
| `/app/session.leave` | `{ ticketId? }` | Participant | Leaves the room |
| `/app/session.vote` | `{ inviteCode, ticketId, cardValue }` | Participant | Casts a vote (`cardValue` = card UUID) |
| `/app/session.reveal` | `{ ticketId }` | Host | Reveals votes |
| `/app/session.reset-votes` | `{ ticketId }` | Host | Starts a new round |
| `/app/session.finish` | `{ ticketId }` | Host | Marks the **ticket** as finished |
| `/app/session.set-timer` | `{ ticketId, durationSeconds? }` | Host | Starts or clears the round timer |

### Server → clients (`/topic/session/{code}/...`)
| Topic | Payload | Sent when |
|---|---|---|
| `participants` | `[{ participantId, displayName, role, isGuest }]` | Join, leave, disconnect |
| `state` | Session details (`id, sessionCode, name, hostUsername, participantCount, createdAt, deckId, seriesType`) | Most state changes |
| `vote-status` | `{ "<participantId>": true, ... }` (who has voted, no values) | Vote, reveal, reset, join |
| `votes` | `{ sessionCode, votes: [{ voteId, username, cardValue, votedAt }], revealed, suggestedAverage, suggestedCardValue, statistics: { average, consensusPercentage, isFullConsensus, outlierVoteIds } }` | Reveal, and reset (empty) |
| `ticket-updated` | `{ id, title, description, gameSessionId, status }` | Ticket created, status change, finish, reset |
| `timer` | `{ durationSeconds, timerExpiresAt, isExpired }` | Timer set, expired |

### Private queue (`/user/queue/...`)
| Destination | Payload | Sent when |
|---|---|---|
| `/user/queue/errors` | `{ action, code, message }` with `code` in `BAD_REQUEST`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`, `INTERNAL_ERROR` | One of **your** WebSocket actions failed. Other participants never see it |

### Known limitations
These are tracked and will change the contract:
- Sessions are identified by `code` in some routes and by `sessionId` in others ([#63](https://github.com/arsova-mx/OpenPoker/issues/63)).
