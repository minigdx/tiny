package com.github.minigdx.tiny.input

enum class TouchSignal {
    TOUCH1,
    TOUCH2,
    TOUCH3,
    ;

    companion object {
        fun signal(index: Int): TouchSignal? {
            return when (index) {
                0 -> TOUCH1
                1 -> TOUCH2
                2 -> TOUCH3
                else -> null
            }
        }
    }
}
