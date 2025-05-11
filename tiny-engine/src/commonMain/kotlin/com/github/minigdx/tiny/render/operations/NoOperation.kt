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

object CameraOperation : NoOperation {
    override val target: RenderUnit = RenderUnit.CPU
}
