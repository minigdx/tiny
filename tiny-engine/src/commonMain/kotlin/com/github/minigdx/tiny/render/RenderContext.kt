package com.github.minigdx.tiny.render

sealed interface RenderContext

interface GPURenderContext : RenderContext

interface CPURenderContext : RenderContext
