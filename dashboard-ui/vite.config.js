import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, repoRoot, '')
  const physicsProxyTarget = env.VITE_DEV_PHYSICS_PROXY_TARGET || 'http://localhost:8080'

  const apiProxy = {
    '/api': {
      target: physicsProxyTarget,
      changeOrigin: true,
    },
  }

  return {
    plugins: [react(), tailwindcss()],
    server: {
      port: 3000,
      strictPort: true,
      proxy: apiProxy,
    },
    preview: {
      port: 3000,
      strictPort: true,
      proxy: apiProxy,
    },
  }
})
