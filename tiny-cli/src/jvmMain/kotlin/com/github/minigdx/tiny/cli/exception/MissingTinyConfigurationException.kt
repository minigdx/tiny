package com.github.minigdx.tiny.cli.exception

import java.io.File

class MissingTinyConfigurationException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause) {

    constructor(file: File) : this(
        "The file '${file.absolutePath}' doesn't exist. " +
            "It's required to load the game configuration. " +
            "Can you check if the file is not located in another path? " +
            "Without a valid file, the game will not be able to run.",
    )
}
