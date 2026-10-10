/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // One .env file in the repository root, shared with Docker Compose and Spring Boot
  envDir: '..',
  // Fixed port: CORS_ALLOWED_ORIGINS in .env points to http://localhost:5173
  server: {
    port: 5173,
    strictPort: true,
  },
  css: {
    preprocessorOptions: {
      scss: {
        // Hide the Sass deprecation warnings raised inside Bootstrap's own files
        quietDeps: true,
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
})
