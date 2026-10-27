import path from 'node:path';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

// - tailwindcss(): estilos de Tailwind (los usa el código generado con v0 / shadcn/ui)
// - alias "@": permite importar como lo hace v0, por ejemplo "@/components/ui/button"
// - proxy: redirige /api y /uploads al backend de Spring Boot
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(import.meta.dirname, './src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/uploads': 'http://localhost:8080',
    },
  },
});
