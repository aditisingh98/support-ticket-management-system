import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Optional local UI iteration only: Vite on 9001 proxies /api → Spring Boot on 9000.
// Final demo serves the built SPA from Spring Boot at http://localhost:9000 only.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 9001,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:9000',
        changeOrigin: true,
      },
    },
  },
  preview: {
    port: 9001,
    strictPort: true,
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true,
  },
})
