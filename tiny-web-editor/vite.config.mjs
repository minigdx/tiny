import { defineConfig } from 'vite'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))

console.log("🧹 COUCOU")
console.log(resolve(__dirname, 'index.html'))
export default defineConfig({
    root: "kotlin",
    build: {
        rollupOptions: {
            input: {
                // HTML entry point - relative to root (kotlin directory)
                main: resolve(__dirname, 'kotlin/index.html'),
                // Audio worklet entry point (will be bundled separately)
                // 'audio-worklet': resolve(__dirname, 'kotlin/tiny-tiny-engine/com/github/minigdx/tiny/platform/webgl/Todo.mjs')
                // 'audio-worklet': resolve(__dirname, 'kotlin/tiny-tiny-engine/com/github/minigdx/tiny/platform/webgl/HissGeneratorWorkletModule.mjs')
                'audio-worklet': resolve(__dirname, 'kotlin/tiny-tiny-engine/HissGeneratorWorkletModule__worklet__module.mjs')
            }
        }
    }
})