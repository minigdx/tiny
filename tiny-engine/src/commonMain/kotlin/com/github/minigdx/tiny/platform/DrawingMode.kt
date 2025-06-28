package com.github.minigdx.tiny.platform

enum class DrawingMode {
    DEFAULT, // Default mode for drawing images
    ALPHA_BLEND, // Mode for drawing with transparency (alpha blending)
    STENCIL_WRITE, // Mode for writing to the stencil buffer
    STENCIL_TEST, // Mode for drawing based on the stencil buffer content
    STENCIL_NOT_TEST, // Mode for drawing based on the stencil buffer not written
}
