# dashboard-ui

React 19 dashboard for Project Kinetile: feasibility views, trends, compression ledger, tile inventory, and a step-energy calculator. Built with Vite, TypeScript, and Tailwind CSS.

## Development

```bash
npm install
npm run dev
```

Dev server: `http://localhost:3000`. Requests to `/api` are proxied to the physics engine (default `http://localhost:8080`). Override with `VITE_DEV_PHYSICS_PROXY_TARGET`.

Requires the physics engine and a populated database (see root [README.md](../README.md)).

## Scripts

| Command | Purpose |
|---------|---------|
| `npm run dev` | Vite dev server with HMR |
| `npm run build` | Production static build |
| `npm run preview` | Preview production build |
| `npm run test` | Vitest unit tests |
| `npm run lint` | ESLint |

## Production

The [Dockerfile](Dockerfile) builds static assets and serves them with nginx, proxying `/api/v1/` to the physics-engine container (see root `docker-compose.yml`).
