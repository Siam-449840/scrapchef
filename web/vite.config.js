import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true, // Port lock to maintain persistent Puter app origin and ~/AppData/<app_id>/ sandbox
  },
});
