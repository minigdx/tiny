package com.github.minigdx.tiny.platform.test

import kotlin.test.Test

class HeadlessGlfwPlatformTest {
    @Test
    fun testSimpleLineDrawing() {
        val script = """
            function _draw()
                shape.line(0, 0, 10, 10, 2)
            end
        """.trimIndent()

        headlessGlfwTest("simple-line-test", script) { gameController ->
            // Advance by one frame
            gameController.step()

            // Take a screenshot
            val screenshot = gameController.captureScreen()

            // Verify we got some pixel data
            assert(screenshot.isNotEmpty()) { "Screenshot should contain pixel data" }

            // The screenshot should be RGBA format (4 bytes per pixel)
            // For a 10x10 image, we should have 400 bytes
            assert(screenshot.size == 400) { "Screenshot should be 400 bytes (10x10x4 RGBA), got ${screenshot.size}" }

            // Save screenshot to .test directory
            gameController.saveScreenshot("line-drawing")
        }
    }

    @Test
    fun testScreenshotComparison() {
        val script = """
            function _draw()
                shape.line(0, 0, 10, 10, 2)
            end
        """.trimIndent()

        // Test within the same test session so reference and comparison use same temp directory
        headlessGlfwTest("screenshot-comparison", script, 10 to 10, true) { gameController ->
            gameController.step()

            // Save as reference
            gameController.saveScreenshot("reference-line")

            // Now test comparison with the same screenshot
            val matches = gameController.compareWithReference("reference-line")
            assert(matches) { "Screenshot should match itself" }
        }
    }
}
