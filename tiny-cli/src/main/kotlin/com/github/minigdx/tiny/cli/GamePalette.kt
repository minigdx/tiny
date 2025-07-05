package com.github.minigdx.tiny.cli

class GamePalette(val name: String, val colors: List<String>, val source: String? = null) {
    companion object {
        val ONE_BIT =
            GamePalette(
                "1bit",
                listOf("#000000", "#FFFFFF"),
            )
        val PICO8 =
            GamePalette(
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
                "https://lospec.com/palette-list/pico-8",
            )

        val GAMEBOY =
            GamePalette(
                "gameboy",
                listOf(
                    // Lightest
                    "#E0F8D0",
                    "#88C070",
                    "#346856",
                    // Darkest
                    "#081820",
                ),
            )

        // https://lospec.com/palette-list/rgr-proto16
        val RGR_PROTO16 =
            GamePalette(
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
                "https://lospec.com/palette-list/rgr-proto16",
            )

        // Oil 6 Palette
        val OIL_6 = GamePalette(
            "oil-6",
            listOf(
                "#fbf5ef",
                "#f2d3ab",
                "#c69fa5",
                "#8b6d9c",
                "#494d7e",
                "#272744",
            ),
            "https://lospec.com/palette-list/oil-6",
        )

        // Blessing Palette
        val BLESSING = GamePalette(
            "blessing",
            listOf(
                "#74569b",
                "#96fbc7",
                "#f7ffae",
                "#ffb3cb",
                "#d8bfd8",
            ),
            "https://lospec.com/palette-list/blessing",
        )

        // Ice Cream GB Palette
        val ICE_CREAM_GB = GamePalette(
            "ice-cream-gb",
            listOf(
                "#7c3f58",
                "#eb6b6f",
                "#f9a875",
                "#fff6d3",
            ),
            "https://lospec.com/palette-list/ice-cream-gb",
        )

        // 2bit demichrome Palette
        val _2BIT_DEMICHROME = GamePalette(
            "2bit-demichrome",
            listOf(
                "#211e20",
                "#555568",
                "#a0a08b",
                "#e9efec",
            ),
            "https://lospec.com/palette-list/2bit-demichrome",
        )

        // Eulbink Palette
        val EULBINK = GamePalette(
            "eulbink",
            listOf(
                "#ffffff",
                "#0ce6f2",
                "#0098db",
                "#1e579c",
                "#203562",
                "#252446",
                "#201533",
            ),
            "https://lospec.com/palette-list/eulbink",
        )

        // Curiosities Palette
        val CURIOSITIES = GamePalette(
            "curiosities",
            listOf(
                "#46425e",
                "#15788c",
                "#00b9be",
                "#ffeecc",
                "#ffb0a3",
                "#ff6973",
            ),
            "https://lospec.com/palette-list/curiosities",
        )

        // Vanilla Milkshake Palette
        val VANILLA_MILKSHAKE = GamePalette(
            "vanilla-milkshake",
            listOf(
                "#28282e", "#6c5671", "#d9c8bf", "#f98284", "#b0a9e4", "#accce4",
                "#b3e3da", "#feaae4", "#87a889", "#b0eb93", "#e9f59d", "#ffe6c6",
                "#dea38b", "#ffc384", "#fff7a0", "#fff7e4",
            ),
            "https://lospec.com/palette-list/vanilla-milkshake",
        )

        // Mushroom Palette
        val MUSHROOM = GamePalette(
            "mushroom",
            listOf(
                "#2e222f", "#45293f", "#7a3045", "#993d41", "#cd683d", "#fbb954",
                "#f2ec8b", "#b0a987", "#997f73", "#665964", "#443846", "#576069",
                "#788a87", "#a9b2a2",
            ),
            "https://lospec.com/palette-list/mushroom",
        )

        // Resurrect 32 Palette
        val RESURRECT_32 = GamePalette(
            "resurrect-32",
            listOf(
                "#ffffff", "#fb6b1d", "#e83b3b", "#831c5d", "#c32454", "#f04f78",
                "#f68181", "#fca790", "#e3c896", "#ab947a", "#966c6c", "#625565",
                "#3e3546", "#0b5e65", "#0b8a8f", "#1ebc73", "#91db69", "#fbff86",
                "#fbb954", "#cd683d", "#9e4539", "#7a3045", "#6b3e75", "#905ea9",
                "#a884f3", "#eaaded", "#8fd3ff", "#4d9be6", "#4d65b4", "#484a77",
                "#30e1b9", "#8ff8e2",
            ),
            "https://lospec.com/palette-list/resurrect-32",
        )

        // Bubblegum 16 Palette
        val BUBBLEGUM_16 = GamePalette(
            "bubblegum-16",
            listOf(
                "#16171a", "#7f0622", "#d62411", "#ff8426", "#ffd100", "#fafdff",
                "#ff80a4", "#ff2674", "#94216a", "#430067", "#234975", "#68aed4",
                "#bfff3c", "#10d275", "#007899", "#002859",
            ),
            "https://lospec.com/palette-list/bubblegum-16",
        )
        val ALL = listOf(
            ONE_BIT,
            PICO8,
            GAMEBOY,
            RGR_PROTO16,
            OIL_6,
            BLESSING,
            ICE_CREAM_GB,
            _2BIT_DEMICHROME,
            EULBINK,
            CURIOSITIES,
            VANILLA_MILKSHAKE,
            MUSHROOM,
            RESURRECT_32,
            BUBBLEGUM_16,
        )
    }
}
