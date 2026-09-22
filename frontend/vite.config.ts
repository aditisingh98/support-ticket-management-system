import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Frontend: http://localhost:8001
// Backend API (via proxy): http://localhost:8000
export default defineConfig({
  plugins: [react()],
  server: {
    port: 8001,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8000',
        changeOrigin: true,
      },
    },
  },
  preview: {
    port: 8001,
    strictPort: true,
  },
})
