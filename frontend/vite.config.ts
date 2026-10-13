/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'
import { apiOriginOf, contentSecurityPolicy } from './csp.ts'

// One .env file in the repository root, shared with Docker Compose and Spring Boot
const ENV_DIR = '..'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // Vite writes VITE_ variables into the code at build time: a production build without the backend
  // address would call the frontend itself, so it stops here with a clear message (like D83 for the backend)
  const env = loadEnv(mode, ENV_DIR, 'VITE_')
  if (mode === 'production' && !env.VITE_API_BASE_URL) {
    throw new Error('Set VITE_API_BASE_URL (the backend address) before building the frontend')
  }
  // A complete https address (or localhost): the token is sent to it
  if (mode === 'production') {
    apiOriginOf(env.VITE_API_BASE_URL)
  }

  return {
    plugins: [react()],
    envDir: ENV_DIR,
    // Fixed port: CORS_ALLOWED_ORIGINS in .env points to http://localhost:5173
    server: {
      port: 5173,
      strictPort: true,
    },
    // npm run preview serves the production build with the same header that Render must send (D45, D103)
    preview: {
      port: 5173,
      strictPort: true,
      headers: env.VITE_API_BASE_URL
        ? { 'Content-Security-Policy': contentSecurityPolicy(apiOriginOf(env.VITE_API_BASE_URL)) }
        : {},
    },
    css: {
      preprocessorOptions: {
        scss: {
          // Hide the Sass deprecation warnings raised inside Bootstrap's own files
          quietDeps: true,
          // main.scss must use @import to configure Bootstrap 5.3 (see the comment there)
          silenceDeprecations: ['import'],
        },
      },
    },
    test: {
      environment: 'jsdom',
      setupFiles: './src/test/setup.ts',
      coverage: {
        provider: 'v8',
        include: ['src/**/*.{ts,tsx}'],
        exclude: ['src/main.tsx', 'src/test/**', 'src/**/*.test.{ts,tsx}'],
      },
    },
  }
})
