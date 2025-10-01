package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.sound.Instrument
import com.github.minigdx.tiny.sound.Music
import com.github.minigdx.tiny.sound.MusicalBar
import com.github.minigdx.tiny.sound.MusicalSequence
import com.github.minigdx.tiny.sound.SoundHandler
import com.github.minigdx.tiny.sound.Sweep
import com.github.minigdx.tiny.sound.Vibrato
import com.github.minigdx.tiny.sound.VirtualSoundBoard
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

/**
 *
 * envelope(attack, delay, substain, release)
 * instrument(List<Harmonie>, envelope)
 *
 * note (pitch, duration)
 * bars (List<note>, instrument)
 * track (List<bars>)
 * sequence(List<track>)
 * music(List<sequence)
 *
 *
 * sfx.bars(2) // Load the bars number 2
 *
 * sfx.beat(5, C1, 3, 0.5) // Set the beat 5 with the note C1.
 * sfx.bars(2).play()
 * local seq = sfx.sequence(2)
 * local track = seq.track(1)
 * track.solo()
 * track.play()
 *
 * sequence.play()
 *
 * sfx.play(2) // play the bar id 2 ?
 * sfx.music(1) // load another music
 * sfx.save("toto") // save actual music
 *
 *
 */

@TinyLib(
    "sfx",
    """Sound API to play/loop/stop a sound.
A sound can be created using the sound editor, using the command line `tiny-cli sfx <filename>`.
WARNING: Because of browser behaviour, a sound can *only* be played only after the first
user interaction.

Avoid to start a music or a sound at the beginning of the game.
Before it, force the player to hit a key or click by adding an interactive menu
or by starting the sound as soon as the player is moving.
""",
)
class SfxLib(
    private val resourceAccess: GameResourceAccess,
    private val soundBoard: VirtualSoundBoard,
    // When validating the script, don't play sound
    private val playSound: Boolean = true,
) : TwoArgFunction() {
    companion object {
        private const val STREAM_TEMPO = 200
        private const val STREAM_VOLUME = 0.8f
        private const val NOTE_DURATION = 1f
        private const val NOTE_BEAT = 0f
    }

    override fun call(
        arg1: LuaValue,
        arg2: LuaValue,
    ): LuaValue {
        val ctrl = LuaTable()
        ctrl.set("play", play())
        ctrl.set("loop", loop())
        ctrl.set("stop", stop())

        ctrl.set("mplay", mplay())
        ctrl.set("mloop", mloop())
        ctrl.set("mstop", mstop())

        ctrl.set("music", music())

        ctrl.set("instrument", instrument())
        ctrl.set("bar", bar())
        ctrl.set("track", track())

        ctrl.set("load", load())
        ctrl.set("save", save())

        ctrl.set("export", export())

        arg2.set("sfx", ctrl)
        arg2.get("package").get("loaded").set("sfx", ctrl)
        return ctrl
    }

    private data class SoundKey(val soundIndex: Int, val barIndex: Int)

    private data class SequenceKey(val soundIndex: Int, val sequenceIndex: Int)

    private var currentMusic: Music? = null
    private var currentSound: Int = 0
    private var currentSequence: Int = 0

    private val handlers = mutableMapOf<SoundKey, SoundHandler>()
    private val sequenceHandlers = mutableMapOf<SequenceKey, SoundHandler>()

    // Cache for instrument Lua wrappers
    private val instrumentWrapperCache = mutableMapOf<Int, LuaValue>()
    private var lastMusicVersion = 0

    fun getCurrentMusic(): Music {
        fun findCurrentMusic(): Music {
            val defaultMusic = resourceAccess.findSound(0)?.data?.music ?: Music()
            currentMusic = defaultMusic
            invalidateInstrumentCache()
            return defaultMusic
        }
        return currentMusic ?: findCurrentMusic()
    }

    private fun invalidateInstrumentCache() {
        instrumentWrapperCache.clear()
        lastMusicVersion++
    }

    inner class export : ZeroArgFunction() {
        override fun call(): LuaValue {
            // FIXME:
            TODO("E")
            /*
                        val music = getCurrentMusic()
                        val sequence = music.sequences.getOrNull(currentSequence)
                        sequence?.run { resourceAccess.exportAsSound(sequence) }

                        return NIL

             */
        }
    }

    @TinyFunction("Save the actual music using the filename.")
    inner class save : OneArgFunction() {
        @TinyCall("Save the actual music using the filename")
        override fun call(
            @TinyArg("filename") arg: LuaValue,
        ): LuaValue {
            // FIXME:
            TODO("F")
            /*
                        val music = getCurrentMusic()
                        val content = Json.encodeToString(music)
                        resourceAccess.save(arg.checkjstring()!!, content)
                        return NONE

             */
        }
    }

    @TinyFunction("Load the actual SFX sound as the actual music.")
    inner class load : OneArgFunction() {
        @TinyCall("Load the actual SFX sound as the actual music using filename or its index")
        override fun call(
            @TinyArg("filename") arg: LuaValue,
        ): LuaValue {
            // FIXME:
            TODO()
            /*
                        val sound = if (arg.isint()) {
                            resourceAccess.sound(arg.checkint())
                        } else {
                            resourceAccess.sound(arg.checkjstring()!!)
                        }
                        return if (sound == null) {
                            valueOf(currentSound)
                        } else {
                            val soundIndex = currentSound
                            currentMusic = sound.data.music
                            currentSound = sound.index
                            // Clear cache when music changes
                            invalidateInstrumentCache()
                            valueOf(soundIndex)
                        }

             */
        }
    }

    inner class music() : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            // FIXME:
            TODO("G")
            /*
                        val music = getCurrentMusic()
                        val index = arg.checkint().coerceIn(0, music.sequences.size)
                        val sequence = music.sequences.getOrNull(index) ?: return NIL
                        resourceAccess.play(sequence)
                        return NONE

             */
        }
    }

    @TinyFunction("Access track using its index or its name.")
    inner class track : OneArgFunction() {
        private fun getTrack(
            music: Music,
            index: Int,
        ): MusicalSequence.Track? {
            return music.sequences
                .getOrNull(currentSequence)
                ?.tracks
                ?.getOrNull(index)
        }

        @TinyCall("Access instrument using its index or its name.")
        override fun call(arg: LuaValue): LuaValue {
            val music = getCurrentMusic()
            val index = arg.asTrackIndex(music) ?: return NIL
            return getTrack(music, index)
                ?.toLua()
                ?: NIL
        }

        fun MusicalSequence.Track.toLua(): LuaValue {
            val obj = WrapperLuaTable()

            obj.function1("play") {
                // FIXME:
                TODO("H")
                /*
                                resourceAccess.play(this)
                                NONE

                 */
            }

            obj.wrap(
                "volume",
                { valueOf(this.volume.toDouble()) },
                { this.volume = it.optdouble(0.0).toFloat() },
            )

            obj.wrap("beats") {
                val result = LuaTable()
                this.beats.map { b ->
                    val note = b.note
                    WrapperLuaTable().apply {
                        this.wrap("note") {
                            note?.note?.let { valueOf(it) } ?: NIL
                        }
                        this.wrap(
                            "notei",
                            {
                                when {
                                    b.isOffNote -> valueOf(-1) // Special value for note off
                                    note?.index != null -> valueOf(note.index)
                                    else -> NIL
                                }
                            },
                            { arg ->
                                when {
                                    arg.isnil() -> {
                                        // Set to repeat previous note (null)
                                        b.note = null
                                        b.isOffNote = false
                                    }

                                    arg.checkint() == -1 -> {
                                        // Set to note off (silence)
                                        b.note = null
                                        b.isOffNote = true
                                    }

                                    else -> {
                                        // Set to specific note
                                        val noteIndex = arg.checkint()
                                        b.note = Note.fromIndex(noteIndex.coerceIn(Note.C0.index..Note.B8.index) - 1)
                                        b.isOffNote = false
                                    }
                                }
                            },
                        )
                        this.wrap(
                            "octave",
                            {
                                note?.octave?.let { valueOf(it) } ?: NIL
                            },
                            { arg ->
                                b.note = if (arg.isnil() || note == null) {
                                    Note.C5
                                } else {
                                    Note.fromName(note.note + arg.checkint().coerceIn(0, 8))
                                }
                            },
                        )
                        this.wrap(
                            "volume",
                            {
                                valueOf(b.volume.toDouble() * 255.0)
                            },
                            { b.volume = it.tofloat().coerceIn(0f, 255f) / 255f },
                        )
                        this.wrap(
                            "mode",
                            {
                                if (b.isRepeating) valueOf(1) else valueOf(0)
                            },
                            { b.isRepeating = it.checkint() >= 1 },
                        )
                        this.wrap(
                            "instrument",
                            {
                                b.instrumentIndex?.let { valueOf(it) } ?: NIL
                            },
                            { arg ->
                                b.instrumentIndex = if (arg.isnil()) {
                                    null
                                } else {
                                    if (arg.checkint() < 0) {
                                        null
                                    } else {
                                        arg.checkint().coerceIn(0, 7)
                                    }
                                }
                            },
                        )
                    }
                }.forEachIndexed { index, value ->
                    result.insert(index + 1, value)
                }
                result
            }
            return obj
        }
    }

    @TinyFunction("Access instrument using its index or its name.")
    inner class instrument : LibFunction() {
        @TinyCall("Access instrument using its index or its name.")
        override fun call(a: LuaValue): LuaValue {
            val music = getCurrentMusic()
            val index = a.asInstrumentIndex(music) ?: return NIL

            // Check cache first
            return instrumentWrapperCache[index]
            // Check music
                ?: music.instruments.getOrNull(index)?.toLua()?.also {
                    // Cache the result of [toLua].
                    instrumentWrapperCache[index] = it
                }
                // Give up
                ?: NIL
        }

        @TinyCall(
            "Access instrument using its index or its name. " +
                    "Create it if the instrument is missing and the flag is true.",
        )
        override fun call(
            a: LuaValue,
            b: LuaValue,
        ): LuaValue {
            val instrument = call(a)
            return if (instrument == NIL && b.optboolean(false)) {
                val index = a.toint()
                val newInstrument = Instrument(index)
                getCurrentMusic().instruments[index] = newInstrument
                // Clear cache for this index since we're creating new instrument
                instrumentWrapperCache.remove(index)
                newInstrument.toLua()
            } else {
                instrument
            }
        }

        fun Instrument.toLua(): LuaValue {
            val obj = WrapperLuaTable()

            obj.wrap(
                "index",
                { valueOf(this.index) },
            )
            obj.wrap("all") {
                val luaTable = LuaTable()
                getCurrentMusic().instruments.mapNotNull { it?.index }.forEach {
                    luaTable.insert(0, valueOf(it))
                }
                luaTable
            }
            obj.wrap(
                "name",
                { this.name?.let { valueOf(it) } ?: NIL },
                { this.name = it.optjstring(null) },
            )
            obj.wrap(
                "wave",
                { valueOf(this.wave.name) },
                {
                    this.wave = it.checkjstring()
                        ?.let { Instrument.WaveType.valueOf(it) }
                        ?: Instrument.WaveType.SINE
                },
            )
            obj.wrap(
                "attack",
                { valueOf(this.attack.toDouble()) },
                { this.attack = it.optdouble(0.0).toFloat() },
            )
            obj.wrap(
                "decay",
                { valueOf(this.decay.toDouble()) },
                { this.decay = it.optdouble(0.0).toFloat() },
            )
            obj.wrap(
                "sustain",
                { valueOf(this.sustain.toDouble()) },
                { this.sustain = it.optdouble(0.0).toFloat() },
            )
            obj.wrap(
                "release",
                { valueOf(this.release.toDouble()) },
                { this.release = it.optdouble(0.0).toFloat() },
            )
            obj.wrap("sweep") {
                // FIXME: to be cached
                val sweep = WrapperLuaTable()
                sweep.wrap(
                    "active",
                    {
                        val value = this.modulations[0].active
                        valueOf(value)
                    },
                    {
                        val newValue = it.optboolean(false)
                        this.modulations[0].active = newValue
                    },
                )
                sweep.wrap(
                    "acceleration",
                    { valueOf((this.modulations[0] as Sweep).acceleration.toDouble()) },
                    { (this.modulations[0] as Sweep).acceleration = it.optdouble(0.0).toFloat() },
                )
                sweep.wrap(
                    "sweep",
                    { valueOf((this.modulations[0] as Sweep).sweep.toDouble()) },
                    { (this.modulations[0] as Sweep).sweep = it.optdouble(0.0).toFloat() },
                )
                sweep
            }
            obj.wrap("vibrato") {
                // FIXME: to be cached
                val vibrato = WrapperLuaTable()
                vibrato.wrap(
                    "active",
                    {
                        val value = this.modulations[1].active
                        valueOf(value)
                    },
                    {
                        val newValue = it.optboolean(false)
                        this.modulations[1].active = newValue
                    },
                )
                vibrato.wrap(
                    "frequency",
                    { valueOf((this.modulations[1] as Vibrato).vibratoFrequency.toDouble()) },
                    { (this.modulations[1] as Vibrato).vibratoFrequency = it.optdouble(0.0).toFloat() },
                )
                vibrato.wrap(
                    "depth",
                    { valueOf((this.modulations[1] as Vibrato).depth.toDouble()) },
                    { (this.modulations[1] as Vibrato).depth = it.optdouble(0.0).toFloat() },
                )
                vibrato
            }

            obj.function1("noteOn") { noteName ->
                val note = Note.fromName(noteName.tojstring())
                soundBoard.noteOn(note, this)
                NONE
            }

            obj.function1("noteOff") { noteName ->
                val note = Note.fromName(noteName.tojstring())
                soundBoard.noteOff(note)
                NONE
            }

            obj.wrap("harmonics") {
                WrapperLuaTable().apply {
                    (0 until this@toLua.harmonics.size).forEach { index ->
                        wrap(
                            "${index + 1}",
                            { valueOf(this@toLua.harmonics[index].toDouble()) },
                            { this@toLua.harmonics[index] = it.tofloat() },
                        )
                    }
                }
            }

            // Cache the wrapper before returning
            instrumentWrapperCache[index] = obj
            return obj
        }
    }

    @TinyFunction("Access sfx using its index or its name.")
    inner class bar : OneArgFunction() {
        @TinyCall("Access sfx using its index or its name.")
        override fun call(arg: LuaValue): LuaValue {
            val music = getCurrentMusic()
            val index = arg.checkint()
            return music.musicalBars
                .getOrNull(index)
                ?.toLua(music) ?: NIL
        }

        fun MusicalBar.toLua(music: Music): LuaValue {
            val obj = WrapperLuaTable()

            obj.wrap(
                "index",
                { valueOf(this.index) },
            )

            obj.wrap(
                "all",
                {
                    val luaTable = LuaTable()
                    getCurrentMusic().musicalBars.map { it.index }.forEach {
                        luaTable.insert(0, valueOf(it))
                    }
                    luaTable
                },
            )

            obj.wrap(
                "bpm",
                { valueOf(this.tempo) },
                { this.tempo = it.checkint() },
            )

            obj.function1("set_volume") { arg ->
                val beat = arg["beat"].todouble().toFloat()
                val volume = arg["volume"].todouble().toFloat()

                this.setVolume(beat, volume)

                NONE
            }

            obj.function1("set_note") { arg ->
                val beat = arg["beat"].todouble().toFloat()
                val note = Note.fromName(arg["note"].tojstring())
                val duration = arg["duration"].todouble().toFloat()
                val uniqueOnBeat = arg["unique"].optboolean(false)

                this.setNote(note, beat, duration, uniqueOnBeat)

                NONE
            }

            obj.function1("remove_note") { arg ->
                val beat = arg["beat"].todouble().toFloat()
                val note = Note.fromName(arg["note"].tojstring())

                this.removeNote(note, beat)

                NONE
            }

            obj.function1("note_data") { arg ->
                val note = Note.fromName(arg.tojstring())

                LuaTable().apply {
                    this.set("note", valueOf(note.name))
                    this.set("notei", valueOf(note.index))
                    this.set("octave", valueOf(note.octave))
                }
            }

            obj.wrap("notes") {
                val result = LuaTable()
                this.beats.sortedBy { it.beat }
                    .map {
                        LuaTable().apply {
                            this.set("note", it.note?.name?.let { valueOf(it) } ?: NIL)
                            this.set("notei", it.note?.index?.let { valueOf(it) } ?: NIL)
                            this.set("octave", it.note?.octave?.let { valueOf(it) } ?: NIL)
                            this.set("volume", valueOf(it.volume.toDouble()))
                            this.set("beat", valueOf(it.beat.toDouble()))
                            this.set("duration", valueOf(it.duration.toDouble()))
                        }
                    }.forEachIndexed { index, value ->
                        result.insert(index + 1, value)
                    }
                result
            }

            obj.function0("play") {
                val handler = soundBoard.prepare(this).also { it.play() }
                val result = WrapperLuaTable()
                result.function0("stop") {
                    handler.stop()
                    NONE
                }
                result
            }

            obj.function1("set_instrument") { arg ->
                if (arg.isnil()) {
                    this.instrument?.let { valueOf(it.index) } ?: NIL
                } else {
                    // Set the instrument for the bar.
                    val index = arg.asInstrumentIndex(music) ?: return@function1 NIL
                    val instrument = music.instruments.getOrNull(index) ?: return@function1 NIL
                    this.instrument = instrument
                    this.instrumentIndex = instrument.index
                    NONE
                }
            }
            obj.wrap("instrument") {
                this.instrument?.index?.let { valueOf(it) } ?: NIL
            }

            return obj
        }
    }

    @TinyFunction(
        "Play the bar by it's index of the current sound. " +
                "The index of a bar of the current music.",
    )
    inner class play : OneArgFunction() {
        @TinyCall("Play the sound at the index 0.")
        override fun call(): LuaValue = super.call()

        @TinyCall("Play the sound by it's index.")
        override fun call(
            @TinyArg("sound") arg: LuaValue,
        ): LuaValue {
            val bars = getCurrentMusic().musicalBars
            if (bars.isEmpty()) return NIL

            val index = if (arg.isnumber()) {
                arg.checkint().coerceIn(0, bars.size - 1)
            } else {
                0
            }

            if (playSound) {
                // FIXME:
                TODO("K")
                /*
                                val soundData = resourceAccess.sound(currentSound)?.data
                                val sfx = soundData?.musicalBars?.getOrNull(index) ?: return NIL

                                val handler = soundData.soundManager.createSoundHandler(sfx, sfx.size.toLong())
                                handlers[SoundKey(currentSound, index)] = handler
                                handler.play()

                                return NONE

                 */
            } else {
                return NIL
            }
        }
    }

    @TinyFunction(
        "Loop the bar by it's index of the current sound. " +
                "The index of a bar of the current music.",
    )
    inner class loop : OneArgFunction() {
        @TinyCall("Loop the sound at the index 0.")
        override fun call(): LuaValue = super.call()

        @TinyCall("Loop the sound by it's index.")
        override fun call(
            @TinyArg("sound") arg: LuaValue,
        ): LuaValue {
            val bars = getCurrentMusic().musicalBars
            if (bars.isEmpty()) return NIL

            val index = if (arg.isnumber()) {
                arg.checkint().coerceIn(0, bars.size - 1)
            } else {
                0
            }

            if (playSound) {
                // FIXME:
                TODO("L")
                /*
                                val soundData = resourceAccess.sound(currentSound)?.data
                                val sfx = soundData?.musicalBars?.getOrNull(index) ?: return NIL

                                val handler = soundData.soundManager.createSoundHandler(sfx, sfx.size.toLong())
                                handlers[SoundKey(currentSound, index)] = handler
                                handler.loop()

                                return NONE

                 */
            } else {
                return NIL
            }
        }
    }

    @TinyFunction(
        "Stop the bar by it's index of the current sound. " +
                "The index of a bar of the current music.",
    )
    inner class stop : OneArgFunction() {
        @TinyCall("Stop the sound at the index 0.")
        override fun call(): LuaValue = super.call()

        @TinyCall("Stop the sound by it's index.")
        override fun call(
            @TinyArg("sound") arg: LuaValue,
        ): LuaValue {
            val bars = getCurrentMusic().musicalBars
            if (bars.isEmpty()) return NIL

            val index = if (arg.isnumber()) {
                arg.checkint().coerceIn(0, bars.size - 1)
            } else {
                0
            }

            handlers[SoundKey(currentSound, index)]?.stop() ?: return NIL
            return NONE
        }
    }

    @TinyFunction(
        "Play the sequence by it's index of the current sound. " +
                "The index of a sequence of the current music.",
    )
    inner class mplay : OneArgFunction() {
        @TinyCall("Play the sequence at the index 0.")
        override fun call(): LuaValue = super.call()

        @TinyCall("Play the sequence by it's index.")
        override fun call(
            @TinyArg("sequence") arg: LuaValue,
        ): LuaValue {
            val sequences = getCurrentMusic().sequences
            if (sequences.isEmpty()) return NIL

            val index = if (arg.isnumber()) {
                arg.checkint().coerceIn(0, sequences.size - 1)
            } else {
                0
            }

            if (playSound) {
                // FIXME:
                TODO("M")
                /*
                                val soundData = resourceAccess.sound(currentSound)?.data
                                val sfx = soundData?.musicalSequences?.getOrNull(index) ?: return NIL

                                val handler = soundData.soundManager.createSoundHandler(sfx, sfx.size.toLong())
                                sequenceHandlers[SequenceKey(currentSound, index)] = handler
                                handler.play()

                                return NONE

                 */
            } else {
                return NIL
            }
        }
    }

    @TinyFunction(
        "Loop the sequence by it's index of the current sound. " +
                "The index of a sequence of the current music.",
    )
    inner class mloop : OneArgFunction() {
        @TinyCall("Loop the sequence at the index 0.")
        override fun call(): LuaValue = super.call()

        @TinyCall("Loop the sequence by it's index.")
        override fun call(
            @TinyArg("sequence") arg: LuaValue,
        ): LuaValue {
            val sequences = getCurrentMusic().sequences
            if (sequences.isEmpty()) return NIL

            val index = if (arg.isnumber()) {
                arg.checkint().coerceIn(0, sequences.size - 1)
            } else {
                0
            }

            if (playSound) {
                // FIXME:
                TODO("N")
                /*
                                val soundData = resourceAccess.sound(currentSound)?.data
                                val sfx = soundData?.musicalSequences?.getOrNull(index) ?: return NIL

                                val handler = soundData.soundManager.createSoundHandler(sfx, sfx.size.toLong())
                                sequenceHandlers[SequenceKey(currentSound, index)] = handler
                                handler.loop()

                                return NONE

                 */
            } else {
                return NIL
            }
        }
    }

    @TinyFunction(
        "Stop the sequence by it's index of the current sound. " +
                "The index of a sequence of the current music.",
    )
    inner class mstop : OneArgFunction() {
        @TinyCall("Stop the sequence at the index 0.")
        override fun call(): LuaValue = super.call()

        @TinyCall("Stop the sequence by it's index.")
        override fun call(
            @TinyArg("sequence") arg: LuaValue,
        ): LuaValue {
            val sequences = getCurrentMusic().sequences
            if (sequences.isEmpty()) return NIL

            val index = if (arg.isnumber()) {
                arg.checkint().coerceIn(0, sequences.size - 1)
            } else {
                0
            }

            sequenceHandlers[SequenceKey(currentSound, index)]?.stop() ?: return NIL
            return NONE
        }
    }

    private fun LuaValue.asInstrumentIndex(music: Music): Int? {
        return if (this.isint()) {
            val index = this.checkint()
            music.instruments
                .firstOrNull { it?.index == index }
                ?.index
        } else {
            music.instruments
                .firstOrNull { inst -> inst?.name == this.checkjstring() }
                ?.index
        }
    }

    private fun LuaValue.asTrackIndex(music: Music): Int {
        return this.checkint() % music.sequences[currentSequence].tracks.size
    }
}
