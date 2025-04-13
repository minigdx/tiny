package com.github.minigdx.tiny.render

import com.danielgergely.kgl.Framebuffer
import com.danielgergely.kgl.GL_FRAMEBUFFER
import com.danielgergely.kgl.GL_TEXTURE_2D
import com.danielgergely.kgl.Kgl
import com.danielgergely.kgl.Texture

fun Kgl.usingFramebuffer(
    framebuffer: Framebuffer,
    block: () -> Unit,
) {
    bindFramebuffer(GL_FRAMEBUFFER, framebuffer)
    block()
    bindFramebuffer(GL_FRAMEBUFFER, null)
}

fun Kgl.usingTexture(
    texture: Texture,
    block: () -> Unit,
) {
    bindTexture(GL_TEXTURE_2D, texture)
    block()
    bindTexture(GL_TEXTURE_2D, null)
}
