/// <reference types="vite/client" />

interface ImportMetaEnv {
  // Backend address: from the root .env locally, from the Render settings online
  readonly VITE_API_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
