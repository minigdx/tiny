package com.github.minigdx.tiny.cli

class GamePalette(val name: String, val colors: List<String>) {
    companion object {
        val PICO8 = GamePalette(
            "pico8",
            listOf(
                "#000000",
                "#1D2B53",
                "#7E2553",
                "#008751",
                "#AB5236",
                "#5F574F",
                "#C2C3C7",
                "#FFF1E8",
                "#FF004D",
                "#FFA300",
                "#FFEC27",
                "#00E436",
                "#29ADFF",
                "#83769C",
                "#FF77A8",
                "#FFCCAA"
            )
        )

        val GAMEBOY = GamePalette(
            "gameboy",
            listOf(
                "#E0F8D0", // Lightest
                "#88C070",
                "#346856",
                "#081820" // Darkest
            )
        )

        val ALL = listOf(PICO8, GAMEBOY)
    }
}
