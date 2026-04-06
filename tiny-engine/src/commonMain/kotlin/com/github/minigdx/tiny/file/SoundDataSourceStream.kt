package com.github.minigdx.tiny.file

import com.github.minigdx.tiny.platform.SoundData
import com.github.minigdx.tiny.sound.Music
import com.github.minigdx.tiny.sound.MusicalPattern
import com.github.minigdx.tiny.sound.MusicalPhrase
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
        music.musicalPhrases.forEach { musicBar ->
            musicBar.instrument = music.instruments[musicBar.instrumentIndex]
        }
        music.sequences.forEach { sequence ->
            sequence.patterns.forEach { pattern ->
                pattern.tracks.forEach { phrase ->
                    phrase.instrument = music.instruments[phrase.instrumentIndex]
                }
            }
        }

        // Synthesize bars and sequences in parallel.
        // Each task gets a copied instrument to avoid shared mutable state
        // (NOISE wave type has filter state in Instrument).
        val (sounds, sequences) = coroutineScope {
            val soundsDeferred = music.musicalPhrases.map { bar ->
                async {
                    val isolatedBar = MusicalPhrase(
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
                    // Create an isolated copy with fresh instrument state per phrase
                    val isolatedPatterns = sequence.patterns.map { pattern ->
                        val isolatedTracks = pattern.tracks.map { phrase ->
                            MusicalPhrase(
                                index = phrase.index,
                                instrumentIndex = phrase.instrumentIndex,
                                volume = phrase.volume,
                                mute = phrase.mute,
                            ).also {
                                it.instrument = phrase.instrument?.copyWithFreshState()
                                it.setNotes(phrase.beats)
                            }
                        }.toTypedArray()
                        MusicalPattern(
                            index = pattern.index,
                            tracks = isolatedTracks,
                        )
                    }.toTypedArray()
                    val isolatedSequence = MusicalSequence(
                        index = sequence.index,
                        patterns = isolatedPatterns,
                        tempo = sequence.tempo,
                        name = sequence.name,
                        arrangement = sequence.arrangement,
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
