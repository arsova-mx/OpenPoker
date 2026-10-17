# Frontend (React + Vite + TypeScript)

## Layout (`src/`)

| Path | Role |
|---|---|
| `api/clients/APIClient.ts` | The axios instance. Base URL `VITE_API_URL` or `/api`; adds `Authorization: Bearer` from `localStorage.token` |
| `api/services/*.ts` | One object per domain (`authService`, `sessionServices`, `ticketService`, `voteService`, ...). Paths are relative to `/api` |
| `components/<Feature>/` | Feature components (`VotingBoard`, `SessionLobby`, ...) |
| `components/ui/` | shadcn/ui primitives (config in `components.json`). Add new ones with the shadcn CLI, don't hand-copy |
| `hooks/useStompClient.ts` | The only STOMP client: connects to `/ws-native`, sends the JWT on CONNECT, returns `{ connected, subscribe, publish }` |
| `pages/`, `routes/` | Pages and the `createBrowserRouter` config; `ProtectedRoute` guards `/home` and `/session/:code` |
| `store/authStore.ts` | The only Zustand store (persisted under `auth`) |
| `types/index.ts` | Shared types. Keep them aligned with the backend DTOs and `docs/api.md` |
| `lib/errors.ts` | `getErrorMessage` handles both plain-string and JSON error bodies from the backend |

Use the `@/` alias (maps to `src/`) for new imports.

## Rules

- **API calls** go through a service in `api/services/` that uses `APIClient`. Don't call `fetch`/axios directly in
  components. The response interceptor already shows an error toast for failed requests (except `/auth/*`), so
  don't add a second toast for the same failure.
- **Real time**: subscribe through `useStompClient().subscribe(...)` and always unsubscribe in the effect cleanup.
  Topics are `/topic/session/{code}/<topic>`; client messages go to `/app/session.<action>`. Check
  `docs/api.md` (and `WebSocketController` in the backend) before publishing: a destination without a backend
  handler is silently ignored.
- **Forms**: react-hook-form + zod (`zodResolver`). **Toasts**: `sonner` (the `<Toaster />` is mounted in
  `routes/Root.tsx`).
- **Auth state** is split today: the raw token lives in `localStorage.token` (written by `authService.saveToken`) and
  `useAuthStore` persists only `isAuthenticated`, `tokenDuration` and `username`. Unifying it is #47; don't add a
  third place.
- **UI copy** is Spanish and hardcoded (no i18n library yet). Keep new copy in Spanish and consistent with nearby
  screens.
- **Accessibility**: interactive cards and controls must be reachable by keyboard and have labels (#66). The
  `accessibility` and `frontend-design` skills are available for UI work.

## Checks

```bash
npm run lint    # ESLint on src/**/*.ts(x), --max-warnings 0 (react-refresh warnings fail too)
npm run build   # tsc -b with strict, noUnusedLocals and noUnusedParameters, then vite build
npm run format  # Prettier: single quotes, semicolons, printWidth 100, 2 spaces
```

Run lint and build before finishing. There is no test runner yet (Vitest + Testing Library is #55); when it lands,
cover voting components first.

ESLint only checks `.ts`/`.tsx`. The leftover `.js`/`.jsx` files (`pages/SessionPage.jsx`,
`api/services/websocket.js`) are dead and broken; don't import or extend them.

## Environment

- `VITE_API_URL` is baked in at build time. Relative (`/api`) when nginx or a proxy serves the API on the same
  origin; absolute (`https://api.example.com/api`) when the backend is on another host. The WebSocket URL is derived
  from it in `useStompClient.ts`.
- The dev server runs on port 3000 and proxies `/api`, `/ws` and `/ws-native` to `http://localhost:8080`.
