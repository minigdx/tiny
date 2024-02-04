package com.github.minigdx.tiny.sound

data class Beat(val index: Int, val notes: List<WaveGenerator>)

data class Pattern(val index: Int, val beats: List<Beat>)
data class Song(val bpm: Int, val patterns: Map<Int, Pattern>, val music: List<Pattern>)
