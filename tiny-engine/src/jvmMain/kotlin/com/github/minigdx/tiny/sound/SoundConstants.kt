package com.github.minigdx.tiny.sound

const val SAMPLE_RATE = 44100

const val CHUNK_DURATION = 0.05 // seconds
const val CHUNK_SIZE = (SAMPLE_RATE * CHUNK_DURATION).toInt()

const val BITS_PER_SAMPLE = 16
const val CHANNELS = 1 // Code actuel est pour Mono
const val IS_SIGNED = true
const val IS_BIG_ENDIAN = false // Little Endian est courant pour PCM WAV

// Calcul dérivé pour la conversion
const val BYTES_PER_SAMPLE = BITS_PER_SAMPLE / 8
