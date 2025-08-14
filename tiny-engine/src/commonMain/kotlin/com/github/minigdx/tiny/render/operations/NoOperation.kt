package com.github.minigdx.tiny.render.operations

import com.github.minigdx.tiny.render.RenderUnit

interface NoOperation : RenderOperation

object DitheringOperation : NoOperation {
    override val target: RenderUnit = RenderUnit.CPU
}

object PaletteOperation : NoOperation {
    override val target: RenderUnit = RenderUnit.CPU
}

object ClipOperation : NoOperation {
    override val target: RenderUnit = RenderUnit.CPU
}

// FIXME: when profiler on, it's using _engine.lua that invoke a lot CameraOperation.
//   as it's on the CPU, there is a switch between GPU/CPU. Is it expected?
object CameraOperation : NoOperation {
    override val target: RenderUnit = RenderUnit.CPU
}
