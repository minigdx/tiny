package com.github.minigdx.tiny.cli

class GamePalette(val name: String, val colors: List<String>) {
    companion object {
        val ONE_BIT = GamePalette(
            "1bit",
            listOf("#000000", "#FFFFFF"),
        )
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
                "#FFCCAA",
            ),
        )

        val GAMEBOY = GamePalette(
            "gameboy",
            listOf(
                "#E0F8D0", // Lightest
                "#88C070",
                "#346856",
                "#081820", // Darkest
            ),
        )

        // https://lospec.com/palette-list/rgr-proto16
        val RGR_PROTO16 = GamePalette(
            "rgr-proto16",
            listOf(
                "#FFF9B3",
                "#B9C5CC",
                "#4774B3",
                "#144B66",
                "#8FB347",
                "#2E994E",
                "#F29066",
                "#E65050",
                "#707D7C",
                "#293C40",
                "#170B1A",
                "#0A010D",
                "#570932",
                "#871E2E",
                "#FFBF40",
                "#CC1424",
            ),
        )
        val ALL = listOf(ONE_BIT, PICO8, GAMEBOY, RGR_PROTO16)
    }
}
