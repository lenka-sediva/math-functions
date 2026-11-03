import { defineConfig } from 'vite';

export default defineConfig({
  // Relative base so the build works at https://<user>.github.io/<repo>/ without hard-coding the repo name.
  base: './',
  build: {
    target: 'es2022',
    outDir: 'dist',
  },
});
