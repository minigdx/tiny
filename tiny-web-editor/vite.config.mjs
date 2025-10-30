import { defineConfig } from 'vite'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))

export default defineConfig({
    root: "kotlin",
    build: {
        sourcemap: true,
        rollupOptions: {
            input: {
                main: resolve(__dirname, 'kotlin/index.html'),
            },

            output: {
                entryFileNames: (chunkInfo) => {
                    if (chunkInfo.name === 'main') {
                        return 'tiny-engine.js';
                    }
                    // Default pattern for other entries
                    return '[name]-[hash].js';
                },
                chunkFileNames: 'chunks/[name]-[hash].js',
                manualChunks: (id) => {
                    // Split vendor code from Kotlin stdlib and coroutines
                    if (id.includes('node_modules')) {
                        return 'vendor';
                    }
                    // Split Kotlin standard library
                    if (id.includes('kotlin-kotlin-stdlib')) {
                        return 'kotlin-stdlib';
                    }
                    // Split coroutines library
                    if (id.includes('kotlinx-coroutines-core')) {
                        return 'kotlin-coroutines';
                    }
                    // Split Lua library
                    if (id.includes('luak')) {
                        return 'lua';
                    }
                }
            }
        }
    }
})