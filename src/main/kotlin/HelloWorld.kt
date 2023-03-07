import com.danielgergely.kgl.KglLwjgl
import org.lwjgl.Version
import org.lwjgl.glfw.*
import org.lwjgl.glfw.Callbacks.*
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.*
import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil

class HelloWorld {
    // The window handle
    private var window: Long = 0
    fun run() {
        println("Hello LWJGL " + Version.getVersion() + "!")
        init()
        loop()

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window)
        glfwDestroyWindow(window)

        // Terminate GLFW and free the error callback
        glfwTerminate()
        glfwSetErrorCallback(null)?.free()
    }

    private fun init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set()

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        check(glfwInit()) { "Unable to initialize GLFW" }

        // Configure GLFW
        glfwDefaultWindowHints() // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE) // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE) // the window will be resizable

        // Create the window
        window = glfwCreateWindow(1024, 768, "Hello World!", MemoryUtil.NULL, MemoryUtil.NULL)
        if (window == MemoryUtil.NULL) throw RuntimeException("Failed to create the GLFW window")

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window) { window, key, scancode, action, mods ->
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) glfwSetWindowShouldClose(
                window,
                true
            ) // We will detect this in the rendering loop
        }
        MemoryStack.stackPush().use { stack ->
            val pWidth = stack.mallocInt(1) // int*
            val pHeight = stack.mallocInt(1) // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight)

            // Get the resolution of the primary monitor
            val vidmode: GLFWVidMode = glfwGetVideoMode(glfwGetPrimaryMonitor())!!

            // Center the window
            glfwSetWindowPos(
                window,
                (vidmode.width() - pWidth[0]) / 2,
                (vidmode.height() - pHeight[0]) / 2
            )
        }

        // Make the OpenGL context current
        glfwMakeContextCurrent(window)
        // Enable v-sync
        glfwSwapInterval(1)

        // Make the window visible
        glfwShowWindow(window)
    }

    private fun loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities(true)
        // Set the clear color
        glOrtho(0.0,1024.0,768.0,0.0,-1.0,-1.0)
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT) // clear the framebuffer

            drawTriangle(0.0,0.0,0.3)

            glfwSwapBuffers(window) // swap the color buffers
            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents()
        }
    }

    private fun drawTriangle(x: Double, y: Double, size: Double) {
        /**
         * Draw triangle at position (x,y) with given size where
         *  - center of the screen is (0,0)
         *  - left-bottom point of the screen is (-1,-1)
         *  - right-top point of the screen is (1,1)
         *  - size 1.0 fills entire screen
         */

        glGenVertexArrays()

            // KglLwjgl.createVertexArray()

        glPushMatrix()
        glTranslated(x, y, 0.0)
        glBegin(GL_TRIANGLES);
        // Top & Red
        glColor3d(1.0, 0.0, 0.0);
        glVertex2d(-1.0*size, -1.0*size);
        // Right & Green
        glColor3d(0.0, 1.0, 0.0);
        glVertex2d(0.0, 1.0*size);
        // Left & Blue
        glColor3d(0.0, 0.0, 1.0);
        glVertex2d(1.0*size, -1.0*size);
        glEnd();
        glPopMatrix()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            HelloWorld().run()
        }
    }
}
