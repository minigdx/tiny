package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.resources.GameLevel
import com.github.minigdx.tiny.resources.GameScript
import com.github.minigdx.tiny.resources.Sound
import com.github.minigdx.tiny.resources.SpriteSheet

interface GameResourceAccess {
    /**
     * The boot script sprite sheet.
     * Only the engine / bootscript can control what to do with it.
     */
    val bootSpritesheet: SpriteSheet?

    val currentScript: GameScript?

    /**
     * Set the current script.
     * @return the previous game script, if any
     */
    fun setCurrentScript(index: Int): Pair<GameScript?, GameScript>

    /**
     * Access a sprite sheet by its index.
     */
    fun findSpritesheet(index: Int): SpriteSheet?

    /**
     * Find a sprite sheet by its name
     */
    fun findSpritesheet(name: String): SpriteSheet?

    /**
     * Generate a new Spritesheet index, in case of a new spritesheet.
     */
    fun newSpritesheetIndex(): Int

    /**
     * Save the spritesheet as a new spritesheet in the list of spritesheet;
     */
    fun saveSpritesheet(sheet: SpriteSheet)

    /**
     * Access a level by its index.
     */
    fun findLevel(index: Int): GameLevel?

    /**
     * Access sound by its index
     */
    fun findSound(index: Int): Sound?

    /**
     * Access sound by its name
     */
    fun findSound(name: String): Sound?

    fun findGameScript(name: String): GameScript?
}
