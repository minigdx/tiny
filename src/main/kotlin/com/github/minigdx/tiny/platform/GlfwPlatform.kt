package com.github.minigdx.tiny.platform

import com.github.minigdx.tiny.engine.GameLoop
import org.lwjgl.glfw.GLFW
import org.lwjgl.system.MemoryUtil

class GlfwPlatform : Platform {

    var window: Long = 0

    override fun initWindow() {
        if (!GLFW.glfwInit()) {
            throw IllegalStateException("Unable to initialize GLFW")
        }

        GLFW.glfwDefaultWindowHints() // optional, the current window hints are already the default

        GLFW.glfwWindowHint(
            GLFW.GLFW_VISIBLE,
            GLFW.GLFW_FALSE
        ) // the window will stay hidden after creation
        GLFW.glfwWindowHint(
            GLFW.GLFW_RESIZABLE,
            GLFW.GLFW_TRUE
        ) // the window will be resizable

        // Create the window
        window = GLFW.glfwCreateWindow(
            256,
            256,
            "pico16",
            MemoryUtil.NULL,
            MemoryUtil.NULL
        )
        if (window == MemoryUtil.NULL) {
            throw IllegalStateException("Failed to create the GLFW window")
        }

        // Get the resolution of the primary monitor
        val vidmode =
            GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())
                ?: throw IllegalStateException("No primary monitor found")
        // Center our window
        GLFW.glfwSetWindowPos(
            window,
            (vidmode.width() - 256) / 2,
            (vidmode.height() - 256) / 2
        )

        // Make the OpenGL context current
        GLFW.glfwMakeContextCurrent(window)
        // Enable v-sync
        GLFW.glfwSwapInterval(1)

        org.lwjgl.opengl.GL.createCapabilities()

        // Make the window visible
        GLFW.glfwShowWindow(window)

        // Get the size of the device window
        val tmpWidth = MemoryUtil.memAllocInt(1)
        val tmpHeight = MemoryUtil.memAllocInt(1)
        GLFW.glfwGetWindowSize(window, tmpWidth, tmpHeight)
    }

    override fun initGameLoop() = Unit

    override fun gameLoop(gameLoop: GameLoop) {
        // Render loop
        while (!GLFW.glfwWindowShouldClose(window)) {
            gameLoop.advance(30f)
            // Advance the game
            // game.render(delta)
            GLFW.glfwSwapBuffers(window) // swap the color buffers
            GLFW.glfwPollEvents()
        }
    }

    override fun endGameLoop() = Unit
}
