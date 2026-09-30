/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Base URL de la API REST. Por defecto "/api" (mismo origen, vía proxy de Vite o nginx). */
  readonly VITE_API_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
