package com.github.minigdx.tiny.file

import com.github.minigdx.tiny.platform.SoundData
import com.github.minigdx.tiny.sound.Music
import com.github.minigdx.tiny.sound.MusicalBar
import com.github.minigdx.tiny.sound.MusicalSequence
import com.github.minigdx.tiny.sound.SoundManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class SoundDataSourceStream(
    private val name: String,
    val soundManager: SoundManager,
    val delegate: SourceStream<ByteArray>,
) : SourceStream<SoundData> {
    override suspend fun exists(): Boolean = delegate.exists()

    override suspend fun read(): SoundData {
        val bytes = delegate.read()

        val music: Music = Music.deserialize(bytes.decodeToString())

        music.instruments = Array(16) {
            if (it in 0 until music.instruments.size) {
                music.instruments[it]
            } else {
                null
            }
        }
        music.musicalBars.forEach { musicBar ->
            musicBar.instrument = music.instruments[musicBar.instrumentIndex]
        }
        music.sequences.forEach { sequence ->
            sequence.tracks.forEach { track ->
                track.instrument = music.instruments[track.instrumentIndex]
            }
        }

        // Synthesize bars and sequences in parallel.
        // Each task gets a copied instrument to avoid shared mutable state
        // (NOISE wave type has filter state in Instrument).
        val (sounds, sequences) = coroutineScope {
            val soundsDeferred = music.musicalBars.map { bar ->
                async {
                    val isolatedBar = MusicalBar(
                        index = bar.index,
                        instrumentIndex = bar.instrumentIndex,
                        tempo = bar.tempo,
                        name = bar.name,
                        volume = bar.volume,
                    )
                    isolatedBar.instrument = bar.instrument?.copyWithFreshState()
                    isolatedBar.setNotes(bar.beats)
                    soundManager.convert(isolatedBar)
                }
            }
            val sequencesDeferred = music.sequences.map { sequence ->
                async {
                    // Create an isolated copy with fresh instrument state per track
                    val isolatedTracks = sequence.tracks.map { track ->
                        MusicalSequence.Track(
                            index = track.index,
                            instrumentIndex = track.instrumentIndex,
                            mute = track.mute,
                            volume = track.volume,
                        ).also {
                            it.instrument = track.instrument?.copyWithFreshState()
                            it.beats.clear()
                            it.beats.addAll(track.beats)
                        }
                    }.toTypedArray()
                    val isolatedSequence = MusicalSequence(
                        index = sequence.index,
                        tracks = isolatedTracks,
                        tempo = sequence.tempo,
                        name = sequence.name,
                    )
                    soundManager.convert(isolatedSequence)
                }
            }
            soundsDeferred.awaitAll() to sequencesDeferred.awaitAll()
        }

        return SoundData(name, music, sounds, sequences)
    }

    override fun wasModified(): Boolean = delegate.wasModified()
}
