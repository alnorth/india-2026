import { defineConfig } from 'astro/config';
import react from '@astrojs/react';
import tailwind from '@astrojs/tailwind';
import fs from 'fs';
import path from 'path';

export default defineConfig({
  integrations: [
    react(),
    tailwind({
      applyBaseStyles: false,
    }),
  ],
  output: 'static',
  build: {
    format: 'directory',
  },
  site: 'https://india-2026.alnorth.com',
  trailingSlash: 'always',
  vite: {
    plugins: [
      {
        name: 'copy-content',
        closeBundle() {
          // Copy content directory to dist after build
          const src = path.join(process.cwd(), 'content');
          const dest = path.join(process.cwd(), 'dist', 'content');

          function copyDir(src, dest) {
            if (!fs.existsSync(dest)) {
              fs.mkdirSync(dest, { recursive: true });
            }
            const entries = fs.readdirSync(src, { withFileTypes: true });
            for (const entry of entries) {
              const srcPath = path.join(src, entry.name);
              const destPath = path.join(dest, entry.name);
              if (entry.isDirectory()) {
                copyDir(srcPath, destPath);
              } else {
                fs.copyFileSync(srcPath, destPath);
              }
            }
          }

          if (fs.existsSync(src)) {
            copyDir(src, dest);
            console.log('✓ Copied content directory to dist/');
          }
        }
      }
    ]
  }
});
