package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.resources.GameScript

/**
 * [GameEngineListener] is an interface to be notified when the game engine switch from one script to another.
 *
 * It's useful to be notified when the game engine switch from one script to another.
 * It's also useful to be notified when the game engine reload a script.
 */
interface GameEngineListener {
    fun switchScript(
        before: GameScript?,
        after: GameScript?,
    )

    fun reload(gameScript: GameScript?)
}
