/**
 * Base URL for the physics engine HTTP API.
 *
 * <p>Normally left empty, which produces relative, same-origin URLs (e.g. `/api/v1/...`). A proxy
 * forwards those to the backend: the Vite dev server in development, nginx in the Docker build. This
 * is the default in both environments, so {@link PHYSICS_API_BASE} is `''` unless explicitly set.
 *
 * <p>Set the optional `VITE_PHYSICS_API_BASE_URL` build-time env var (no trailing slash, e.g.
 * `http://localhost:8080`) ONLY when the frontend must call a backend on a different origin without
 * a proxy in front of it. That makes requests cross-origin and therefore depends on backend CORS.
 */
export const PHYSICS_API_BASE = String(import.meta.env.VITE_PHYSICS_API_BASE_URL ?? '').replace(
  /\/$/,
  '',
)

/** @param path e.g. `/api/v1/config/physics` */
export function physicsApiUrl(path: string): string {
  const p = path.startsWith('/') ? path : `/${path}`
  return `${PHYSICS_API_BASE}${p}`
}
