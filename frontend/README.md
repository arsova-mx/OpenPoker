# OpenPoker frontend

React 18 + Vite 5 + TypeScript single-page app for OpenPoker. Styling with Tailwind CSS 4 and
[shadcn/ui](https://ui.shadcn.com); state with Zustand; real time with `@stomp/stompjs`; forms with
react-hook-form + zod; toasts with sonner.

For the whole project (backend, Docker Compose, configuration) see the [root README](../README.md).

## Development

Requirements: Node.js 22+ and a backend running on `http://localhost:8080` (see the root README).

```bash
npm ci
npm run dev      # http://localhost:3000
```

The dev server proxies `/api`, `/ws` and `/ws-native` to `http://localhost:8080`, so the browser only talks to
one origin.

## Scripts

| Command | What it does |
|---|---|
| `npm run dev` | Vite dev server on port 3000 |
| `npm run lint` | ESLint on `src/` with `--max-warnings 0` (CI fails on any warning) |
| `npm run build` | Strict type-check (`tsc -b`) and production build to `dist/` |
| `npm run preview` | Serve the production build locally |
| `npm run format` | Prettier over `src/` |

CI runs `npm ci`, `npm run lint` and `npm run build` on every pull request.

## Configuration

| Variable | When | Default | Description |
|---|---|---|---|
| `VITE_API_URL` | build time | `/api` | Base URL of the REST API. Keep it relative when the API is served from the same origin (nginx in the Docker image, or a proxy). Use an absolute URL such as `https://api.example.com/api` when the backend lives on another host; the WebSocket URL (`/ws-native`) is derived from it. |

## Production image

`Dockerfile` builds the bundle and serves it with nginx. `nginx.conf.template` proxies `/api/` and `/ws-native`
to `BACKEND_URL` (default in Compose: `http://backend:8080`) and falls back to `index.html` for client-side
routes.

## Project structure

```
src/
├── api/clients/     # axios instance (base URL, auth header, error toasts)
├── api/services/    # one service per domain: auth, sessions, tickets, votes, decks
├── components/      # feature components; components/ui holds shadcn/ui primitives
├── hooks/           # useStompClient and other hooks
├── pages/ routes/   # pages and React Router configuration
├── store/           # Zustand stores
├── lib/             # helpers (cn, error messages)
└── types/           # types shared across the app
```

Contributor and AI-agent conventions for this package are in [CLAUDE.md](CLAUDE.md).
