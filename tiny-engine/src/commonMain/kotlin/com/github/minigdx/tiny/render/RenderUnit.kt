package com.github.minigdx.tiny.render

/**
 * Specifies the target processing unit for rendering operations.
 */
enum class RenderUnit {
    /**
     * Target the Central Processing Unit (CPU).
     * Often used for tasks less suitable for massive parallelism or requiring complex logic.
     */
    CPU,

    /**
     * Target the Graphics Processing Unit (GPU).
     * Ideal for highly parallel rendering tasks like processing large numbers of pixels.
     */
    GPU,

    /**
     * Target both unit (CPU/GPU).
     *
     * Can be applied in any unit. Because can do both or because it's altering the state of the next operation.
     *
     * Selecting the right unit might depend on the operation by itself or the previous operation, to keep the
     * same unit.
     */
    BOTH,

    ;

    fun compatibleWith(target: RenderUnit): Boolean {
        return when (this) {
            CPU -> target == CPU || target == BOTH
            GPU -> target == GPU || target == BOTH
            BOTH -> true
        }
    }
}
