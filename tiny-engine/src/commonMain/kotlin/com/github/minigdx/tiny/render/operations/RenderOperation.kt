package com.github.minigdx.tiny.render.operations

import com.github.minigdx.tiny.render.OperationsRender
import com.github.minigdx.tiny.render.RenderContext
import com.github.minigdx.tiny.render.RenderUnit

@Deprecated("use SpriteBatch instead")
sealed interface RenderOperation {
    val target: RenderUnit

    /**
     * Render the operation on the GPU, by using a shader.
     */
    fun executeGPU(
        context: RenderContext,
        renderUnit: OperationsRender,
    ): Unit = invalidTarget(RenderUnit.GPU)

    /**
     * Try to merge the current operation with the previous one, to batch operations.
     */
    fun mergeWith(previousOperation: RenderOperation?): Boolean = false

    private fun invalidTarget(renderUnit: RenderUnit): Nothing =
        throw IllegalStateException(
            "The operation ${this::class.simpleName} does not support $renderUnit render operations. ",
        )
}
