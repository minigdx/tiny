package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.lua.Note
import com.github.minigdx.tiny.sound.SoundManager.Companion.SAMPLE_RATE
import kotlin.math.roundToInt

/**
 * Beat-by-beat music scheduler that generates audio in real-time using [InstrumentPlayer]s.
 *
 * Instead of pre-computing the entire music buffer, the scheduler advances beat-by-beat,
 * firing noteOn/noteOff events at beat boundaries. Configuration changes (root, scale,
 * progression, etc.) are applied at the next bar boundary for seamless transitions.
 */
class MusicScheduler {
    // The sequence data filled by MusicGenerator
    private val sequence = MusicalSequence(index = -1)

    // Playback state
    private var playing = false
    private var currentBeat = 0
    private var samplesUntilNextBeat = 0
    private var samplesPerBeat = 0

    // One InstrumentPlayer per track (chord=0, bass=1, lead=2, drums=3)
    private val players = arrayOfNulls<InstrumentPlayer>(4)

    // Track the last note played on each track for noteOff
    private val lastNote = arrayOfNulls<Note>(4)

    // Per-note volume (set at beat boundary from MusicalNote.volume)
    private val noteVolume = FloatArray(4) { 0f }

    // Pending config change applied at next bar boundary
    private var pendingConfig: MusicConfiguration? = null
    private var pendingInstruments: Array<Instrument?>? = null

    // Current instruments reference
    private var instruments: Array<Instrument?> = emptyArray()

    fun isPlaying(): Boolean = playing

    /**
     * Start streaming playback with the given configuration and instruments.
     */
    fun play(
        config: MusicConfiguration,
        instruments: Array<Instrument?>,
    ) {
        this.instruments = instruments

        // Generate the sequence from config
        MusicGenerator.generate(sequence, config)
        sequence.configuration = config
        sequence.tempo = config.bpm
        linkInstruments(instruments)

        // Create InstrumentPlayers for each track
        createPlayers()

        samplesPerBeat = (60f / sequence.tempo * SAMPLE_RATE).roundToInt()
        currentBeat = 0
        samplesUntilNextBeat = 0
        playing = true
    }

    /**
     * Stop streaming playback, releasing all notes.
     */
    fun stop() {
        playing = false
        for (i in 0 until 4) {
            lastNote[i]?.let { players[i]?.noteOff(it) }
            lastNote[i] = null
            noteVolume[i] = 0f
        }
    }

    /**
     * Schedule a config update to be applied at the next bar boundary.
     * The music continues playing without interruption.
     */
    fun updateConfig(
        config: MusicConfiguration,
        instruments: Array<Instrument?>,
    ) {
        pendingConfig = config
        pendingInstruments = instruments
    }

    /**
     * Generate a single audio sample. Called from the audio thread per sample.
     */
    fun generate(): Float {
        if (!playing) return 0f

        if (samplesUntilNextBeat <= 0) {
            onBeatBoundary()
        }
        samplesUntilNextBeat--

        var sum = 0f
        for (i in 0 until 4) {
            val player = players[i] ?: continue
            val track = sequence.tracks[i]
            if (!track.mute) {
                sum += player.generate() * track.volume * noteVolume[i]
            }
        }

        // Scale down by number of tracks to prevent clipping
        return sum * MIX_SCALE
    }

    private fun onBeatBoundary() {
        // At bar boundary (every 8 beats), check for pending config changes
        if (currentBeat % 8 == 0) {
            applyPendingConfig()
        }

        // Schedule notes for each track
        for (i in 0 until 4) {
            val track = sequence.tracks[i]
            if (track.mute || currentBeat >= track.beats.size) continue

            val beat = track.beats[currentBeat]
            val player = players[i] ?: continue

            // noteOff previous note
            lastNote[i]?.let { player.noteOff(it) }
            lastNote[i] = null

            // noteOn new note (if not silence)
            if (beat.note != null && beat.volume > 0f) {
                player.noteOn(beat.note!!)
                lastNote[i] = beat.note
                noteVolume[i] = beat.volume
            } else {
                noteVolume[i] = 0f
            }
        }

        samplesPerBeat = (60f / sequence.tempo * SAMPLE_RATE).roundToInt()
        samplesUntilNextBeat = samplesPerBeat

        // Advance beat, loop after 32 beats (indices 0-31)
        currentBeat = (currentBeat + 1) % LOOP_LENGTH
    }

    private fun applyPendingConfig() {
        val config = pendingConfig ?: return
        val insts = pendingInstruments ?: return

        pendingConfig = null
        pendingInstruments = null

        instruments = insts

        // Regenerate the sequence with new config
        MusicGenerator.generate(sequence, config)
        sequence.configuration = config
        sequence.tempo = config.bpm
        linkInstruments(insts)

        // Recreate players with potentially new instruments
        for (i in 0 until 4) {
            lastNote[i]?.let { players[i]?.noteOff(it) }
            lastNote[i] = null
            noteVolume[i] = 0f
        }
        createPlayers()

        samplesPerBeat = (60f / sequence.tempo * SAMPLE_RATE).roundToInt()
    }

    private fun linkInstruments(instruments: Array<Instrument?>) {
        sequence.tracks.forEach { track ->
            track.instrument = instruments.getOrNull(track.instrumentIndex)
        }
    }

    private fun createPlayers() {
        for (i in 0 until 4) {
            val inst = sequence.tracks[i].instrument
            players[i] = if (inst != null) InstrumentPlayer(inst) else null
        }
    }

    companion object {
        private const val LOOP_LENGTH = 32
        private const val MIX_SCALE = 0.25f
    }
}
