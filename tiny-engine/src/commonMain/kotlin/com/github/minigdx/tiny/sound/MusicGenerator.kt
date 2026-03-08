package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.lua.Note
import kotlin.random.Random

object MusicGenerator {
    private val NOTE_NAMES = listOf("C", "Cs", "D", "Ds", "E", "F", "Fs", "G", "Gs", "A", "As", "B")

    val SCALE_NAMES = listOf("Major", "Minor", "Penta Maj", "Penta Min", "Dorian", "Mixolydian")

    val SCALES: Map<String, List<Int>> = mapOf(
        "Major" to listOf(0, 2, 4, 5, 7, 9, 11),
        "Minor" to listOf(0, 2, 3, 5, 7, 8, 10),
        "Penta Maj" to listOf(0, 2, 4, 7, 9),
        "Penta Min" to listOf(0, 3, 5, 7, 10),
        "Dorian" to listOf(0, 2, 3, 5, 7, 9, 10),
        "Mixolydian" to listOf(0, 2, 4, 5, 7, 9, 10),
    )

    val PROGRESSION_NAMES = listOf("Classic", "Melancholy", "Dreamy", "Tense", "Upbeat", "Cycle")

    val PROGRESSIONS: Map<String, List<Int>> = mapOf(
        "Classic" to listOf(1, 5, 6, 4),
        "Melancholy" to listOf(6, 4, 1, 5),
        "Dreamy" to listOf(1, 4, 6, 5),
        "Tense" to listOf(1, 7, 6, 5),
        "Upbeat" to listOf(1, 4, 5, 4),
        "Cycle" to listOf(2, 5, 1, 4),
    )

    val DRUM_PATTERN_NAMES = listOf("Rock", "Dance", "Halftime", "Funky", "March", "Sparse")

    val DRUM_PATTERNS: Map<String, DrumPattern> = mapOf(
        "Rock" to DrumPattern(
            kick = intArrayOf(1, 0, 0, 0, 1, 0, 0, 0),
            snare = intArrayOf(0, 0, 1, 0, 0, 0, 1, 0),
            hihat = intArrayOf(1, 1, 1, 1, 1, 1, 1, 1),
        ),
        "Dance" to DrumPattern(
            kick = intArrayOf(1, 0, 1, 0, 1, 0, 1, 0),
            snare = intArrayOf(0, 0, 1, 0, 0, 0, 1, 0),
            hihat = intArrayOf(0, 1, 0, 1, 0, 1, 0, 1),
        ),
        "Halftime" to DrumPattern(
            kick = intArrayOf(1, 0, 0, 0, 0, 0, 0, 0),
            snare = intArrayOf(0, 0, 0, 0, 1, 0, 0, 0),
            hihat = intArrayOf(1, 0, 1, 0, 1, 0, 1, 0),
        ),
        "Funky" to DrumPattern(
            kick = intArrayOf(1, 0, 0, 1, 0, 0, 1, 0),
            snare = intArrayOf(0, 0, 1, 0, 0, 1, 0, 0),
            hihat = intArrayOf(1, 1, 0, 1, 1, 0, 1, 1),
        ),
        "March" to DrumPattern(
            kick = intArrayOf(1, 0, 1, 0, 1, 0, 1, 0),
            snare = intArrayOf(0, 1, 0, 1, 0, 1, 0, 1),
            hihat = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        ),
        "Sparse" to DrumPattern(
            kick = intArrayOf(1, 0, 0, 0, 0, 0, 0, 0),
            snare = intArrayOf(0, 0, 0, 0, 1, 0, 0, 0),
            hihat = intArrayOf(0, 0, 1, 0, 0, 0, 1, 0),
        ),
    )

    val LEAD_STYLES = listOf("Stepwise", "Arpeggiated", "Bouncy", "Sparse", "Random")

    data class DrumPattern(
        val kick: IntArray,
        val snare: IntArray,
        val hihat: IntArray,
    )

    private fun noteNameToIndex(name: String): Int {
        val idx = NOTE_NAMES.indexOf(name)
        return if (idx >= 0) idx else 0
    }

    private fun semitoneToNote(semitone: Int): Note {
        val clamped = semitone.coerceIn(0, 95)
        return Note.fromIndex(clamped)
    }

    private fun buildScaleNotes(
        rootName: String,
        scale: List<Int>,
        octave: Int,
    ): List<Int> {
        val rootSemi = noteNameToIndex(rootName) + octave * 12
        return scale.map { rootSemi + it }
    }

    private fun chordRootSemitone(
        rootName: String,
        scale: List<Int>,
        degree: Int,
        octave: Int,
    ): Int {
        val rootSemi = noteNameToIndex(rootName) + octave * 12
        val idx = ((degree - 1) % scale.size)
        return rootSemi + scale[idx]
    }

    private fun buildChordNotes(
        rootName: String,
        scale: List<Int>,
        degree: Int,
        octave: Int,
    ): List<Int> {
        val root = chordRootSemitone(rootName, scale, degree, octave)
        val rootSemi = noteNameToIndex(rootName) + octave * 12
        val thirdIdx = ((degree - 1 + 2) % scale.size)
        val fifthIdx = ((degree - 1 + 4) % scale.size)
        var third = rootSemi + scale[thirdIdx]
        var fifth = rootSemi + scale[fifthIdx]
        if (third <= root) third += 12
        if (fifth <= root) fifth += 12
        return listOf(root, third, fifth)
    }

    private fun clearTrack(track: MusicalSequence.Track) {
        track.beats.indices.forEach { i ->
            track.beats[i] = MusicalNote(null, i.toFloat(), 1f, 1f)
        }
    }

    fun generate(
        sequence: MusicalSequence,
        config: MusicConfiguration,
    ) {
        sequence.tempo = config.bpm

        val track0 = sequence.tracks[0]
        val track1 = sequence.tracks[1]
        val track2 = sequence.tracks[2]
        val track3 = sequence.tracks[3]

        generateChords(track0, config)
        generateBass(track1, config)
        generateLead(track2, config, Random(config.seed))
        generateDrums(track3, config)
    }

    private fun generateChords(
        track: MusicalSequence.Track,
        config: MusicConfiguration,
    ) {
        val scale = SCALES[config.scaleName] ?: SCALES["Major"]!!
        val progression = PROGRESSIONS[config.progressionName] ?: PROGRESSIONS["Classic"]!!
        clearTrack(track)
        track.instrumentIndex = config.chordInstrument
        track.volume = config.chordVolume

        for (bar in 0..3) {
            val degree = progression[bar % progression.size]
            val chord = buildChordNotes(config.root, scale, degree, 3)
            for (i in 0..7) {
                val beat = bar * 8 + i
                if (beat < 33) {
                    val noteIdx = i % chord.size
                    track.beats[beat] = MusicalNote(
                        semitoneToNote(chord[noteIdx]),
                        beat.toFloat(),
                        1f,
                        0.5f,
                    )
                }
            }
        }
    }

    private fun generateBass(
        track: MusicalSequence.Track,
        config: MusicConfiguration,
    ) {
        val scale = SCALES[config.scaleName] ?: SCALES["Major"]!!
        val progression = PROGRESSIONS[config.progressionName] ?: PROGRESSIONS["Classic"]!!
        clearTrack(track)
        track.instrumentIndex = config.bassInstrument
        track.volume = config.bassVolume

        for (bar in 0..3) {
            val degree = progression[bar % progression.size]
            val root = chordRootSemitone(config.root, scale, degree, 2)
            for (i in 0..7) {
                val beat = bar * 8 + i
                if (beat < 33) {
                    when (i) {
                        0, 4 -> {
                            track.beats[beat] = MusicalNote(
                                semitoneToNote(root),
                                beat.toFloat(),
                                1f,
                                0.7f,
                            )
                        }
                        2, 6 -> {
                            val fifthIdx = ((degree - 1 + 4) % scale.size)
                            val fifth = noteNameToIndex(config.root) + 2 * 12 + scale[fifthIdx]
                            track.beats[beat] = MusicalNote(
                                semitoneToNote(fifth),
                                beat.toFloat(),
                                1f,
                                0.5f,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun generateLead(
        track: MusicalSequence.Track,
        config: MusicConfiguration,
        random: Random,
    ) {
        val scale = SCALES[config.scaleName] ?: SCALES["Major"]!!
        val style = config.leadStyle
        clearTrack(track)
        track.instrumentIndex = config.leadInstrument
        track.volume = config.leadVolume

        val scaleNotes = buildScaleNotes(config.root, scale, 4)
        var pos = random.nextInt(scaleNotes.size)

        for (beat in 0..32) {
            var play = false
            when (style) {
                "Stepwise" -> {
                    play = true
                    pos += listOf(-1, 0, 1).random(random)
                }
                "Arpeggiated" -> {
                    play = true
                    pos += listOf(1, 2).random(random)
                }
                "Bouncy" -> {
                    play = true
                    pos += listOf(-2, -1, 1, 2, 3).random(random)
                }
                "Sparse" -> {
                    play = (beat % 2 == 0) && (random.nextFloat() > 0.3f)
                    if (play) pos += listOf(-1, 0, 1).random(random)
                }
                "Random" -> {
                    play = random.nextFloat() > 0.25f
                    if (play) pos = random.nextInt(scaleNotes.size)
                }
            }

            if (pos < 0) pos += scaleNotes.size
            if (pos >= scaleNotes.size) pos %= scaleNotes.size

            if (play && beat < 33) {
                var semi = scaleNotes[pos]
                if (semi > 95) semi -= 12
                if (semi < 0) semi += 12
                track.beats[beat] = MusicalNote(
                    semitoneToNote(semi),
                    beat.toFloat(),
                    1f,
                    0.4f + random.nextFloat() * 0.2f,
                )
            }
        }
    }

    private fun generateDrums(
        track: MusicalSequence.Track,
        config: MusicConfiguration,
    ) {
        val pattern = DRUM_PATTERNS[config.drumPattern] ?: DRUM_PATTERNS["Rock"]!!
        clearTrack(track)
        track.instrumentIndex = config.drumInstrument
        track.volume = config.drumVolume

        val kickNote = Note.fromName("C2")
        val snareNote = Note.fromName("C4")
        val hihatNote = Note.fromName("C6")

        for (bar in 0..3) {
            for (i in 0..7) {
                val beat = bar * 8 + i
                val pi = i % pattern.kick.size
                if (beat < 33) {
                    when {
                        pattern.kick[pi] == 1 -> {
                            track.beats[beat] = MusicalNote(kickNote, beat.toFloat(), 1f, 0.7f)
                        }
                        pattern.snare[pi] == 1 -> {
                            track.beats[beat] = MusicalNote(snareNote, beat.toFloat(), 1f, 0.6f)
                        }
                        pattern.hihat[pi] == 1 -> {
                            track.beats[beat] = MusicalNote(hihatNote, beat.toFloat(), 1f, 0.35f)
                        }
                    }
                }
            }
        }
    }
}
